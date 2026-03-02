package com.example.demo.Services;

import com.example.demo.DTO.Container;
import com.example.demo.DTO.Deployment;
import com.example.demo.DTO.PrometheusResponse;
import com.example.demo.Config.VictoriaMetricsConfig;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Сбор метрик из VictoriaMetrics (OpenShift) и маппинг в DTO Deployment/Container.
 * Время старта пода: kube_pod_start_time − kube_pod_created; если в кластере они совпадают (оба = creationTimestamp), будет 0.
 */
@Service
public class VictoriaMetricsService {

    private static final String LABEL_NAMESPACE = "namespace";
    private static final String LABEL_DEPLOYMENT = "deployment";
    private static final String LABEL_STATEFULSET = "statefulset";
    private static final String LABEL_POD = "pod";
    private static final String LABEL_CONTAINER = "container";
    private static final String LABEL_OWNER_NAME = "owner_name";
    private static final String LABEL_REPLICASET = "replicaset";

    private final VictoriaMetricsClient client;
    private final VictoriaMetricsConfig config;

    public VictoriaMetricsService(VictoriaMetricsClient client, VictoriaMetricsConfig config) {
        this.client = client;
        this.config = config;
    }

    /**
     * Собрать список деплойментов с контейнерами из метрик OpenShift.
     * @param namespace фильтр по namespace (null/пустой — все неймспейсы)
     * @param fromMs    начало интервала UTC, мс (Grafana ${__from}); null — «сейчас»
     * @param toMs      конец интервала UTC, мс (Grafana ${__to}); null — «сейчас»
     */
    public List<Deployment> fetchDeployments(String namespace, Long fromMs, Long toMs) {
        String range = config.getTimeRange();
        Long evaluationTimeSec = null;
        if (fromMs != null && toMs != null && toMs > fromMs) {
            long rangeSec = (toMs - fromMs) / 1000;
            range = rangeSec + "s";
            evaluationTimeSec = toMs / 1000;
        }

        // 1) Деплойменты и StatefulSet'ы — количество подов
        Map<String, Integer> deploymentReplicas = queryDeploymentReplicas(namespace, evaluationTimeSec);
        Map<String, Integer> statefulSetReplicas = queryStatefulSetReplicas(namespace, evaluationTimeSec);

        // 2) Маппинг pod -> deployment (через ReplicaSet) и pod -> statefulset
        Map<String, String> podToDeployment = queryPodToDeployment(namespace, evaluationTimeSec);
        Map<String, String> podToStatefulSet = queryPodToStatefulSet(namespace, evaluationTimeSec);

        if (podToDeployment.isEmpty() && deploymentReplicas.isEmpty()
                && podToStatefulSet.isEmpty() && statefulSetReplicas.isEmpty()) {
            return List.of();
        }

        // 3) Ресурсы контейнеров (requests/limits)
        Map<ContainerKey, ResourceValues> resources = queryContainerResources(namespace, evaluationTimeSec);

        // 4) Утилизация CPU и памяти (avg/max за период)
        Map<ContainerKey, UsageValues> usage = queryContainerUsage(range, namespace, evaluationTimeSec);

        // 4.5) Время старта пода (сек): kube_pod_start_time - kube_pod_created по каждому поду
        Map<String, Long> podStartupSeconds = queryPodStartupSeconds(namespace, evaluationTimeSec);

        // 5) Собираем деплойменты и стейтфулсеты
        List<Deployment> result = new ArrayList<>();

        // Деплойменты
        Map<DeploymentKey, List<ContainerKey>> deploymentToContainers = new HashMap<>();
        for (ContainerKey ck : resources.keySet()) {
            String dep = podToDeployment.get(ck.namespace + "/" + ck.pod);
            if (dep == null) continue;
            DeploymentKey dk = new DeploymentKey(ck.namespace, dep);
            deploymentToContainers.computeIfAbsent(dk, k -> new ArrayList<>()).add(ck);
        }
        for (String nsAndDep : deploymentReplicas.keySet()) {
            String[] parts = nsAndDep.split("/", 2);
            if (parts.length != 2) continue;
            deploymentToContainers.putIfAbsent(new DeploymentKey(parts[0], parts[1]), new ArrayList<>());
        }
        for (Map.Entry<DeploymentKey, List<ContainerKey>> e : deploymentToContainers.entrySet()) {
            DeploymentKey dk = e.getKey();
            int podCount = deploymentReplicas.getOrDefault(dk.namespace + "/" + dk.deploymentName, 0);
            if (podCount == 0) continue;
            Deployment deployment = buildWorkload(dk.deploymentName, podCount, e.getValue(), resources, usage, podStartupSeconds);
            if (deployment != null) {
                deployment.setWorkloadType("Deployment");
                result.add(deployment);
            }
        }

        // StatefulSet'ы
        Map<DeploymentKey, List<ContainerKey>> statefulSetToContainers = new HashMap<>();
        for (ContainerKey ck : resources.keySet()) {
            String sts = podToStatefulSet.get(ck.namespace + "/" + ck.pod);
            if (sts == null) continue;
            DeploymentKey dk = new DeploymentKey(ck.namespace, sts);
            statefulSetToContainers.computeIfAbsent(dk, k -> new ArrayList<>()).add(ck);
        }
        for (String nsAndSts : statefulSetReplicas.keySet()) {
            String[] parts = nsAndSts.split("/", 2);
            if (parts.length != 2) continue;
            statefulSetToContainers.putIfAbsent(new DeploymentKey(parts[0], parts[1]), new ArrayList<>());
        }
        for (Map.Entry<DeploymentKey, List<ContainerKey>> e : statefulSetToContainers.entrySet()) {
            DeploymentKey dk = e.getKey();
            int podCount = statefulSetReplicas.getOrDefault(dk.namespace + "/" + dk.deploymentName, 0);
            if (podCount == 0) continue;
            Deployment statefulSet = buildWorkload(dk.deploymentName, podCount, e.getValue(), resources, usage, podStartupSeconds);
            if (statefulSet != null) {
                statefulSet.setWorkloadType("StatefulSet");
                result.add(statefulSet);
            }
        }

        result.sort(Comparator.comparing(Deployment::getWorkloadType).thenComparing(Deployment::getName));
        return result;
    }

    private Deployment buildWorkload(String name, int podCount, List<ContainerKey> containerKeys,
                                     Map<ContainerKey, ResourceValues> resources,
                                     Map<ContainerKey, UsageValues> usage,
                                     Map<String, Long> podStartupSeconds) {
        Deployment deployment = new Deployment();
        deployment.setName(name);
        deployment.setPodCount(podCount);
        long startTimeSec = containerKeys.stream()
                .map(ck -> ck.namespace + "/" + ck.pod)
                .distinct()
                .mapToLong(podKey -> podStartupSeconds.getOrDefault(podKey, 0L))
                .max()
                .orElse(0L);
        deployment.setStartTime(startTimeSec);
        deployment.setContainers(new LinkedList<>());

        Map<String, List<ContainerKey>> byContainer = containerKeys.stream()
                .collect(Collectors.groupingBy(ck -> ck.container));
        for (Map.Entry<String, List<ContainerKey>> ce : byContainer.entrySet()) {
            Container container = buildContainer(ce.getKey(), ce.getValue(), resources, usage);
            deployment.getContainers().add(container);
        }
        if (deployment.getContainers().isEmpty()) return null;
        return deployment;
    }

    /** Добавить фильтр по namespace в PromQL (selector). */
    private static String addNamespaceFilter(String query, String namespace) {
        if (namespace == null || namespace.isBlank()) return query;
        String escaped = namespace.replace("\\", "\\\\").replace("\"", "\\\"");
        int lastBrace = query.lastIndexOf('}');
        if (lastBrace >= 0) {
            return query.substring(0, lastBrace) + ",namespace=\"" + escaped + "\"}" + query.substring(lastBrace + 1);
        }
        return query + "{namespace=\"" + escaped + "\"}";
    }

    private Map<String, Integer> queryDeploymentReplicas(String namespace, Long evaluationTimeSec) {
        String query = addNamespaceFilter("kube_deployment_status_replicas_available", namespace);
        PrometheusResponse resp = client.query(query, evaluationTimeSec);
        Map<String, Integer> out = new HashMap<>();
        if (resp == null || resp.getData() == null || resp.getData().getResult() == null) return out;
        for (PrometheusResponse.Result r : resp.getData().getResult()) {
            String ns = VictoriaMetricsClient.getLabel(r.getMetric(), LABEL_NAMESPACE);
            String dep = VictoriaMetricsClient.getLabel(r.getMetric(), LABEL_DEPLOYMENT);
            if (ns == null || dep == null) continue;
            double v = VictoriaMetricsClient.parseValue(r.getValue());
            if (!Double.isNaN(v)) {
                out.put(ns + "/" + dep, (int) Math.round(v));
            }
        }
        return out;
    }

    private Map<String, String> queryPodToDeployment(String namespace, Long evaluationTimeSec) {
        // kube_pod_owner: pod -> replicaset name (owner_name when owner_kind=ReplicaSet)
        String qPod = addNamespaceFilter("kube_pod_owner{owner_kind=\"ReplicaSet\"}", namespace);
        PrometheusResponse respPod = client.query(qPod, evaluationTimeSec);
        if (respPod == null || respPod.getData() == null || respPod.getData().getResult() == null) {
            return Map.of();
        }

        Set<String> replicasetNames = new HashSet<>();
        Map<String, String> podToReplicaset = new HashMap<>();
        for (PrometheusResponse.Result r : respPod.getData().getResult()) {
            String ns = VictoriaMetricsClient.getLabel(r.getMetric(), LABEL_NAMESPACE);
            String pod = VictoriaMetricsClient.getLabel(r.getMetric(), LABEL_POD);
            String rs = VictoriaMetricsClient.getLabel(r.getMetric(), LABEL_OWNER_NAME);
            if (ns != null && pod != null && rs != null) {
                podToReplicaset.put(ns + "/" + pod, rs);
                replicasetNames.add(ns + "/" + rs);
            }
        }

        // kube_replicaset_owner: replicaset -> deployment name
        String qRS = addNamespaceFilter("kube_replicaset_owner", namespace);
        PrometheusResponse respRS = client.query(qRS, evaluationTimeSec);
        Map<String, String> replicasetToDeployment = new HashMap<>();
        if (respRS != null && respRS.getData() != null && respRS.getData().getResult() != null) {
            for (PrometheusResponse.Result r : respRS.getData().getResult()) {
                String ns = VictoriaMetricsClient.getLabel(r.getMetric(), LABEL_NAMESPACE);
                String rs = VictoriaMetricsClient.getLabel(r.getMetric(), LABEL_REPLICASET);
                String dep = VictoriaMetricsClient.getLabel(r.getMetric(), LABEL_OWNER_NAME);
                if (ns != null && rs != null && dep != null) {
                    replicasetToDeployment.put(ns + "/" + rs, dep);
                }
            }
        }

        Map<String, String> podToDeployment = new HashMap<>();
        for (Map.Entry<String, String> e : podToReplicaset.entrySet()) {
            String ns = e.getKey().substring(0, e.getKey().indexOf('/'));
            String rsKey = ns + "/" + e.getValue();
            String dep = replicasetToDeployment.get(rsKey);
            if (dep != null) {
                podToDeployment.put(e.getKey(), dep);
            }
        }
        return podToDeployment;
    }

    private Map<String, Integer> queryStatefulSetReplicas(String namespace, Long evaluationTimeSec) {
        String query = addNamespaceFilter("kube_statefulset_status_replicas_ready", namespace);
        PrometheusResponse resp = client.query(query, evaluationTimeSec);
        Map<String, Integer> out = new HashMap<>();
        if (resp == null || resp.getData() == null || resp.getData().getResult() == null) return out;
        for (PrometheusResponse.Result r : resp.getData().getResult()) {
            String ns = VictoriaMetricsClient.getLabel(r.getMetric(), LABEL_NAMESPACE);
            String sts = VictoriaMetricsClient.getLabel(r.getMetric(), LABEL_STATEFULSET);
            if (ns == null || sts == null) continue;
            double v = VictoriaMetricsClient.parseValue(r.getValue());
            if (!Double.isNaN(v)) {
                out.put(ns + "/" + sts, (int) Math.round(v));
            }
        }
        return out;
    }

    /**
     * Время старта пода в секундах: kube_pod_start_time − kube_pod_created по каждому поду.
     * Если в кластере обе метрики равны (например, оба = creationTimestamp), результат будет 0.
     * Ключ мапы: "namespace/pod".
     */
    private Map<String, Long> queryPodStartupSeconds(String namespace, Long evaluationTimeSec) {
        Map<String, Double> startTime = new HashMap<>();
        Map<String, Double> created = new HashMap<>();
        String qStart = addNamespaceFilter("kube_pod_start_time", namespace);
        String qCreated = addNamespaceFilter("kube_pod_created", namespace);
        PrometheusResponse respStart = client.query(qStart, evaluationTimeSec);
        PrometheusResponse respCreated = client.query(qCreated, evaluationTimeSec);
        if (respStart != null && respStart.getData() != null && respStart.getData().getResult() != null) {
            for (PrometheusResponse.Result r : respStart.getData().getResult()) {
                String ns = VictoriaMetricsClient.getLabel(r.getMetric(), LABEL_NAMESPACE);
                String pod = VictoriaMetricsClient.getLabel(r.getMetric(), LABEL_POD);
                if (ns != null && pod != null) {
                    double v = VictoriaMetricsClient.parseValue(r.getValue());
                    if (!Double.isNaN(v)) startTime.put(ns + "/" + pod, v);
                }
            }
        }
        if (respCreated != null && respCreated.getData() != null && respCreated.getData().getResult() != null) {
            for (PrometheusResponse.Result r : respCreated.getData().getResult()) {
                String ns = VictoriaMetricsClient.getLabel(r.getMetric(), LABEL_NAMESPACE);
                String pod = VictoriaMetricsClient.getLabel(r.getMetric(), LABEL_POD);
                if (ns != null && pod != null) {
                    double v = VictoriaMetricsClient.parseValue(r.getValue());
                    if (!Double.isNaN(v)) created.put(ns + "/" + pod, v);
                }
            }
        }
        Map<String, Long> out = new HashMap<>();
        for (String key : startTime.keySet()) {
            Double c = created.get(key);
            if (c == null) continue;
            double diff = startTime.get(key) - c;
            out.put(key, (long) Math.max(0, Math.round(diff)));
        }
        return out;
    }

    private Map<String, String> queryPodToStatefulSet(String namespace, Long evaluationTimeSec) {
        String qPod = addNamespaceFilter("kube_pod_owner{owner_kind=\"StatefulSet\"}", namespace);
        PrometheusResponse respPod = client.query(qPod, evaluationTimeSec);
        Map<String, String> podToStatefulSet = new HashMap<>();
        if (respPod == null || respPod.getData() == null || respPod.getData().getResult() == null) {
            return podToStatefulSet;
        }
        for (PrometheusResponse.Result r : respPod.getData().getResult()) {
            String ns = VictoriaMetricsClient.getLabel(r.getMetric(), LABEL_NAMESPACE);
            String pod = VictoriaMetricsClient.getLabel(r.getMetric(), LABEL_POD);
            String sts = VictoriaMetricsClient.getLabel(r.getMetric(), LABEL_OWNER_NAME);
            if (ns != null && pod != null && sts != null) {
                podToStatefulSet.put(ns + "/" + pod, sts);
            }
        }
        return podToStatefulSet;
    }

    private Map<ContainerKey, ResourceValues> queryContainerResources(String namespace, Long evaluationTimeSec) {
        Map<ContainerKey, ResourceValues> out = new HashMap<>();

        String qCpuRq = addNamespaceFilter("kube_pod_container_resource_requests{resource=\"cpu\"}", namespace);
        String qCpuLim = addNamespaceFilter("kube_pod_container_resource_limits{resource=\"cpu\"}", namespace);
        String qMemRq = addNamespaceFilter("kube_pod_container_resource_requests{resource=\"memory\"}", namespace);
        String qMemLim = addNamespaceFilter("kube_pod_container_resource_limits{resource=\"memory\"}", namespace);

        fillResourceMetric(out, qCpuRq, (rv, v) -> rv.cpuRqCores = v, evaluationTimeSec);
        fillResourceMetric(out, qCpuLim, (rv, v) -> rv.cpuLimCores = v, evaluationTimeSec);
        fillResourceMetric(out, qMemRq, (rv, v) -> rv.memRqBytes = v, evaluationTimeSec);
        fillResourceMetric(out, qMemLim, (rv, v) -> rv.memLimBytes = v, evaluationTimeSec);

        return out;
    }

    private void fillResourceMetric(Map<ContainerKey, ResourceValues> out, String query,
                                    java.util.function.BiConsumer<ResourceValues, Double> setter,
                                    Long evaluationTimeSec) {
        PrometheusResponse resp = client.query(query, evaluationTimeSec);
        if (resp == null || resp.getData() == null || resp.getData().getResult() == null) return;
        for (PrometheusResponse.Result r : resp.getData().getResult()) {
            ContainerKey ck = containerKeyFromMetric(r.getMetric());
            if (ck == null) continue;
            double v = VictoriaMetricsClient.parseValue(r.getValue());
            if (Double.isNaN(v)) continue;
            out.computeIfAbsent(ck, k -> new ResourceValues()).apply(setter, v);
        }
    }

    private ContainerKey containerKeyFromMetric(Map<String, String> metric) {
        String ns = VictoriaMetricsClient.getLabel(metric, LABEL_NAMESPACE);
        String pod = VictoriaMetricsClient.getLabel(metric, LABEL_POD);
        String container = VictoriaMetricsClient.getLabel(metric, LABEL_CONTAINER);
        if (ns == null || pod == null || container == null || "".equals(container) || "POD".equals(container)) {
            return null;
        }
        return new ContainerKey(ns, pod, container);
    }

    private Map<ContainerKey, UsageValues> queryContainerUsage(String range, String namespace, Long evaluationTimeSec) {
        Map<ContainerKey, UsageValues> out = new HashMap<>();
        String step = config.getSubqueryStep();
        String cpuWindow = config.getCpuRateWindow();

        // CPU: rate в ядрах. Окно rate и шаг подзапроса настраиваются для совпадения с Grafana.
        String cpuRate = addNamespaceFilter(
                "rate(container_cpu_usage_seconds_total{container!=\"\", container!=\"POD\"}[" + cpuWindow + "])", namespace);
        String qCpuAvg = "avg_over_time(" + cpuRate + "[" + range + ":" + step + "])";
        String qCpuMax = "max_over_time(" + cpuRate + "[" + range + ":" + step + "])";

        String mem = addNamespaceFilter(
                "container_memory_working_set_bytes{container!=\"\", container!=\"POD\"}", namespace);
        String qMemAvg = "avg_over_time(" + mem + "[" + range + ":" + step + "])";
        String qMemMax = "max_over_time(" + mem + "[" + range + ":" + step + "])";

        fillUsageMetric(out, qCpuAvg, (uv, v) -> uv.cpuAvgCores = v, evaluationTimeSec);
        fillUsageMetric(out, qCpuMax, (uv, v) -> uv.cpuMaxCores = v, evaluationTimeSec);
        fillUsageMetric(out, qMemAvg, (uv, v) -> uv.memAvgBytes = v, evaluationTimeSec);
        fillUsageMetric(out, qMemMax, (uv, v) -> uv.memMaxBytes = v, evaluationTimeSec);

        return out;
    }

    private void fillUsageMetric(Map<ContainerKey, UsageValues> out, String query,
                                 java.util.function.BiConsumer<UsageValues, Double> setter,
                                 Long evaluationTimeSec) {
        PrometheusResponse resp = client.query(query, evaluationTimeSec);
        if (resp == null || resp.getData() == null || resp.getData().getResult() == null) return;
        for (PrometheusResponse.Result r : resp.getData().getResult()) {
            ContainerKey ck = containerKeyFromMetric(r.getMetric());
            if (ck == null) continue;
            double v = VictoriaMetricsClient.parseValue(r.getValue());
            if (Double.isNaN(v)) continue;
            out.computeIfAbsent(ck, k -> new UsageValues()).apply(setter, v);
        }
    }

    private Container buildContainer(String containerName, List<ContainerKey> keys,
                                     Map<ContainerKey, ResourceValues> resources,
                                     Map<ContainerKey, UsageValues> usage) {
        Container c = new Container();
        c.setName(containerName);

        // Ресурсы берём с первого ключа (в рамках одного deployment/container они одинаковые)
        ResourceValues rv = keys.stream().map(resources::get).filter(Objects::nonNull).findFirst().orElse(null);
        if (rv != null) {
            c.setCpuRq(coresToMillicores(rv.cpuRqCores));
            c.setCpuLim(coresToMillicores(rv.cpuLimCores));
            c.setMemRq(bytesToMb(rv.memRqBytes));
            c.setMemLim(bytesToMb(rv.memLimBytes));
        } else {
            c.setCpuRq(0);
            c.setCpuLim(0);
            c.setMemRq(0);
            c.setMemLim(0);
        }

        // Утилизация: по контейнерам (несколько подов). Метод из конфига: average_per_pod или sum_then_percent (как в Grafana).
        boolean sumThenPercent = "sum_then_percent".equalsIgnoreCase(config.getAggregationMethod());
        if (sumThenPercent) {
            double sumCpuAvg = 0, sumMemAvg = 0, sumCpuLim = 0, sumMemLim = 0;
            double cpuMaxPct = 0, memMaxPct = 0;
            for (ContainerKey ck : keys) {
                UsageValues uv = usage.get(ck);
                ResourceValues res = resources.get(ck);
                if (uv == null) continue;
                double cpuLim = res != null ? res.cpuLimCores : 0;
                double memLim = res != null ? res.memLimBytes : 0;
                if (cpuLim <= 0) cpuLim = (res != null ? res.cpuRqCores : 0);
                if (memLim <= 0) memLim = (res != null ? res.memRqBytes : 0);
                sumCpuAvg += uv.cpuAvgCores;
                sumMemAvg += uv.memAvgBytes;
                sumCpuLim += cpuLim;
                sumMemLim += memLim;
                cpuMaxPct = Math.max(cpuMaxPct, toPercent(uv.cpuMaxCores, cpuLim));
                memMaxPct = Math.max(memMaxPct, toPercent(uv.memMaxBytes, memLim));
            }
            if (sumCpuLim > 0 || sumMemLim > 0) {
                c.setCpuAvgPercent((int) Math.round(toPercent(sumCpuAvg, sumCpuLim)));
                c.setCpuMaxPercent((int) Math.round(cpuMaxPct));
                c.setMemAvgPercent((int) Math.round(toPercent(sumMemAvg, sumMemLim)));
                c.setMemMaxPercent((int) Math.round(memMaxPct));
            } else {
                c.setCpuAvgPercent(0);
                c.setCpuMaxPercent(0);
                c.setMemAvgPercent(0);
                c.setMemMaxPercent(0);
            }
        } else {
            double cpuAvgSum = 0, cpuMaxMax = 0, memAvgSum = 0, memMaxMax = 0;
            int n = 0;
            for (ContainerKey ck : keys) {
                UsageValues uv = usage.get(ck);
                if (uv == null) continue;
                ResourceValues res = resources.get(ck);
                double cpuLim = res != null ? res.cpuLimCores : 0;
                double memLim = res != null ? res.memLimBytes : 0;
                if (cpuLim <= 0) cpuLim = (res != null ? res.cpuRqCores : 0);
                if (memLim <= 0) memLim = (res != null ? res.memRqBytes : 0);
                double cpuAvgPct = toPercent(uv.cpuAvgCores, cpuLim);
                double cpuMaxPct = toPercent(uv.cpuMaxCores, cpuLim);
                double memAvgPct = toPercent(uv.memAvgBytes, memLim);
                double memMaxPct = toPercent(uv.memMaxBytes, memLim);
                cpuAvgSum += cpuAvgPct;
                cpuMaxMax = Math.max(cpuMaxMax, cpuMaxPct);
                memAvgSum += memAvgPct;
                memMaxMax = Math.max(memMaxMax, memMaxPct);
                n++;
            }
            if (n > 0) {
                c.setCpuAvgPercent((int) Math.round(cpuAvgSum / n));
                c.setCpuMaxPercent((int) Math.round(cpuMaxMax));
                c.setMemAvgPercent((int) Math.round(memAvgSum / n));
                c.setMemMaxPercent((int) Math.round(memMaxMax));
            } else {
                c.setCpuAvgPercent(0);
                c.setCpuMaxPercent(0);
                c.setMemAvgPercent(0);
                c.setMemMaxPercent(0);
            }
        }

        // Абсолютные значения: макс использование в millicores и MB
        double cpuMaxAbs = 0, memMaxAbs = 0;
        for (ContainerKey ck : keys) {
            UsageValues uv = usage.get(ck);
            if (uv != null) {
                cpuMaxAbs = Math.max(cpuMaxAbs, uv.cpuMaxCores * 1000);
                memMaxAbs = Math.max(memMaxAbs, uv.memMaxBytes / (1024 * 1024));
            }
        }
        c.setCpuMaxAbs((int) Math.round(cpuMaxAbs));
        c.setMemMaxAbs((int) Math.round(memMaxAbs));

        return c;
    }

    private static int coresToMillicores(double cores) {
        return (int) Math.round(cores * 1000);
    }

    private static int bytesToMb(double bytes) {
        return (int) Math.round(bytes / (1024 * 1024));
    }

    private static double toPercent(double used, double limit) {
        if (limit <= 0) return 0;
        return used / limit * 100;
    }

    private record ContainerKey(String namespace, String pod, String container) {}

    private record DeploymentKey(String namespace, String deploymentName) {}

    private static class ResourceValues {
        double cpuRqCores, cpuLimCores, memRqBytes, memLimBytes;

        void apply(java.util.function.BiConsumer<ResourceValues, Double> setter, double v) {
            setter.accept(this, v);
        }
    }

    private static class UsageValues {
        double cpuAvgCores, cpuMaxCores, memAvgBytes, memMaxBytes;

        void apply(java.util.function.BiConsumer<UsageValues, Double> setter, double v) {
            setter.accept(this, v);
        }
    }
}
