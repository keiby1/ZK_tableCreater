# Метрики утилизации CPU контейнеров в OpenShift (Prometheus / VictoriaMetrics)

## 1. Какими запросами получить утилизацию CPU контейнеров

Источник фактического потребления CPU — **cAdvisor** (метрики kubelet). VictoriaMetrics совместим с Prometheus и те же метрики подхватывает без изменений.

### Основная метрика

- **`container_cpu_usage_seconds_total`** — кумулятивное время использования CPU в секундах (по ядрам).  
  Имеет лейблы вроде: `namespace`, `pod`, `container`, `node` (в OpenShift набор лейблов может немного отличаться, см. документацию по версии).

### Примеры запросов (PromQL / VictoriaMetrics)

**Утилизация CPU в ядрах (rate по контейнерам):**
```promql
sum(rate(container_cpu_usage_seconds_total{container!="", container!="POD"}[5m])) by (namespace, pod, container)
```

**Утилизация в % от запрошенного CPU (request):**
```promql
sum(rate(container_cpu_usage_seconds_total{container!="", container!="POD"}[5m])) by (namespace, pod, container)
/
sum(kube_pod_container_resource_requests{resource="cpu"}) by (namespace, pod, container)
* 100
```

**Утилизация в % от лимита (limit), если задан:**
```promql
sum(rate(container_cpu_usage_seconds_total{container!="", container!="POD"}[5m])) by (namespace, pod, container)
/
sum(kube_pod_container_resource_limits{resource="cpu"}) by (namespace, pod, container)
* 100
```

**Только контейнеры в нужном namespace:**
```promql
sum(rate(container_cpu_usage_seconds_total{namespace="my-namespace", container!="", container!="POD"}[5m])) by (pod, container)
```

В OpenShift в UI мониторинга те же метрики запрашиваются через PromQL; при использовании recording rules может быть агрегат вида `namespace_pod_name_container_name:container_cpu_usage_seconds_total:sum_rate` — тогда в дашбордах часто используют уже его.

---

## 2. В чём разница, если приложение — StatefulSet

- **Сами метрики CPU не меняются:** и для Deployment, и для StatefulSet используется одна и та же **`container_cpu_usage_seconds_total`** из cAdvisor по подам/контейнерам. Запросы выше применимы и к StatefulSet.

Отличия только в том, **как идентифицировать и группировать поды**:

| Аспект | Deployment | StatefulSet |
|--------|------------|-------------|
| Имена подов | Случайный суффикс (`app-7d8f9c-xk2lm`) | Фиксированные: `<statefulset-name>-0`, `-1`, `-2` … |
| Лейблы | Часто `app`, `deployment` | Плюс связь с StatefulSet: например `statefulset.kubernetes.io/pod-name` |
| kube-state-metrics | `kube_deployment_*`, `kube_pod_*` | Дополнительно `kube_statefulset_*` (реплики, статус и т.д.) |

Для **StatefulSet** удобно:

- Фильтровать по имени контроллера (имя StatefulSet = префикс имени пода):
  ```promql
  sum(rate(container_cpu_usage_seconds_total{namespace="my-namespace", pod=~"my-statefulset-.*", container!="", container!="POD"}[5m])) by (pod, container)
  ```
- Строить графики **по каждой реплике** (pod = `my-statefulset-0`, `my-statefulset-1`, …) — реплики стабильно идентифицируются по имени.

**Итого:** запросы к Prometheus/VictoriaMetrics для утилизации CPU контейнеров в OpenShift — те же (rate по `container_cpu_usage_seconds_total` и при необходимости деление на request/limit). Для StatefulSet разница только в фильтрации по имени пода и в использовании метрик `kube_statefulset_*` для количества реплик и статуса, а не для самого CPU usage.
