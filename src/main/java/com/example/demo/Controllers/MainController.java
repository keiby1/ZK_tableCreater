package com.example.demo.Controllers;

import com.example.demo.DTO.Container;
import com.example.demo.DTO.Deployment;
import com.example.demo.Services.HtmlComparisonService;
import com.example.demo.Services.HtmlParserService;
import com.example.demo.Services.HtmlTableService;
import com.example.demo.Services.VictoriaMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

@RestController
public class MainController {
    
    @Autowired
    private HtmlTableService htmlTableService;
    
    @Autowired
    private HtmlParserService htmlParserService;
    
    @Autowired
    private HtmlComparisonService htmlComparisonService;

    @Autowired
    private VictoriaMetricsService victoriaMetricsService;
    
    @RequestMapping("/ping")
    public String test(){
        return "2";
    }

    @RequestMapping
    public ResponseEntity<String> mainPageView(){
        return new ResponseEntity<>("Утилита для получения информации о ресурсах и их утилизации в ДА для ЗелКора", HttpStatus.OK);
    }
    
    /**
     * Та же логика, что и /get (те же параметры, сбор данных, формирование HTML),
     * но результат отдаётся как файл для скачивания (Content-Disposition: attachment).
     */
    @GetMapping("/getHtml")
    public ResponseEntity<String> getHtml(
            @RequestParam(name = "useMetrics", defaultValue = "false") boolean useMetrics,
            @RequestParam(name = "namespace", required = false) String namespace,
            @RequestParam(name = "from", required = false) Long from,
            @RequestParam(name = "to", required = false) Long to) {
        List<Deployment> deployments = useMetrics
                ? victoriaMetricsService.fetchDeployments(namespace, from, to)
                : generateTestData();
        String html = htmlTableService.generateHtmlTable(deployments, from, to);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        headers.setContentDispositionFormData("attachment", "deployment_table.html");

        return new ResponseEntity<>(html, headers, HttpStatus.OK);
    }

    /**
     * Таблица деплойментов: из метрик VictoriaMetrics (useMetrics=true) или тестовые данные.
     * Результат отображается в браузере.
     * @param useMetrics true — брать данные из VictoriaMetrics
     * @param namespace  фильтр по namespace (для useMetrics=true); из Grafana можно не передавать — тогда все неймспейсы
     * @param from       начало интервала (UTC, мс, Unix); из Grafana: ${__from}
     * @param to         конец интервала (UTC, мс, Unix); из Grafana: ${__to}
     */
    @GetMapping("/get")
    public ResponseEntity<String> get(
            @RequestParam(name = "useMetrics", defaultValue = "false") boolean useMetrics,
            @RequestParam(name = "namespace", required = false) String namespace,
            @RequestParam(name = "from", required = false) Long from,
            @RequestParam(name = "to", required = false) Long to) {
        List<Deployment> deployments = useMetrics
                ? victoriaMetricsService.fetchDeployments(namespace, from, to)
                : generateTestData();
        String html = htmlTableService.generateHtmlTable(deployments, from, to);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        // без Content-Disposition — страница отображается в браузере

        return new ResponseEntity<>(html, headers, HttpStatus.OK);
    }

    /**
     * Те же параметры и сбор данных, что и /get, но результат возвращается в виде JSON
     * (список деплойментов с контейнерами и метриками).
     */
    @GetMapping(value = "/getJson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Deployment>> getJson(
            @RequestParam(name = "useMetrics", defaultValue = "false") boolean useMetrics,
            @RequestParam(name = "namespace", required = false) String namespace,
            @RequestParam(name = "from", required = false) Long from,
            @RequestParam(name = "to", required = false) Long to) {
        List<Deployment> deployments = useMetrics
                ? victoriaMetricsService.fetchDeployments(namespace, from, to)
                : generateTestData();
        return ResponseEntity.ok(deployments);
    }

    /**
     * GET /compare — страница с формой: два поля для выбора файлов (drag-drop и кнопки), затем отправка на POST /compare.
     */
    @GetMapping("/compare")
    public ResponseEntity<String> comparePage() {
        String html = buildCompareUploadPage();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        return new ResponseEntity<>(html, headers, HttpStatus.OK);
    }

    /**
     * POST /compare — приём двух HTML-файлов таблиц, парсинг и возврат страницы с результатом сравнения.
     */
    @PostMapping(value = "/compare", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> compareSubmit(
            @RequestParam("file1") MultipartFile file1,
            @RequestParam("file2") MultipartFile file2) {
        String file1Name = file1.getOriginalFilename() != null ? file1.getOriginalFilename() : "Файл 1";
        String file2Name = file2.getOriginalFilename() != null ? file2.getOriginalFilename() : "Файл 2";
        String html1;
        String html2;
        try {
            html1 = new String(file1.getBytes(), StandardCharsets.UTF_8);
            html2 = new String(file2.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            HttpHeaders errHeaders = new HttpHeaders();
            errHeaders.setContentType(MediaType.TEXT_HTML);
            String errorPage = buildCompareUploadPage()
                    + "<div style=\"margin:20px;padding:16px;background:#ffebee;border:1px solid #ef9a9a;border-radius:6px;\">"
                    + "Ошибка чтения файлов: " + e.getMessage() + "</div></body></html>";
            return new ResponseEntity<>(errorPage, errHeaders, HttpStatus.OK);
        }
        List<Deployment> deployments1 = htmlParserService.parseHtmlTable(html1);
        List<Deployment> deployments2 = htmlParserService.parseHtmlTable(html2);
        String comparisonHtml = htmlComparisonService.generateComparisonTable(deployments1, deployments2, file1Name, file2Name);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        return new ResponseEntity<>(comparisonHtml, headers, HttpStatus.OK);
    }

    private String buildCompareUploadPage() {
        return "<!DOCTYPE html>\n"
                + "<html lang=\"ru\">\n"
                + "<head>\n"
                + "  <meta charset=\"UTF-8\">\n"
                + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                + "  <title>Сравнение таблиц</title>\n"
                + "  <style>\n"
                + "    body { font-family: Arial, sans-serif; margin: 24px; background: #f5f5f5; }\n"
                + "    h1 { color: #2e7d32; margin-bottom: 8px; }\n"
                + "    .upload-zone { border: 2px dashed #9e9e9e; border-radius: 8px; padding: 24px; margin: 12px 0; background: #fafafa; min-height: 80px; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: background .2s, border-color .2s; }\n"
                + "    .upload-zone:hover, .upload-zone.dragover { background: #e8f5e9; border-color: #4CAF50; }\n"
                + "    .upload-zone input[type=file] { display: none; }\n"
                + "    .upload-zone .label { color: #616161; }\n"
                + "    .upload-zone.has-file .label { color: #2e7d32; font-weight: bold; }\n"
                + "    form { max-width: 600px; }\n"
                + "    .btn { margin-top: 16px; padding: 12px 24px; background: #4CAF50; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 1em; }\n"
                + "    .btn:hover { background: #43A047; }\n"
                + "    .btn:disabled { background: #9e9e9e; cursor: not-allowed; }\n"
                + "    .hint { font-size: 0.9em; color: #757575; margin-top: 4px; }\n"
                + "  </style>\n"
                + "</head>\n"
                + "<body>\n"
                + "  <h1>Сравнение двух таблиц выгрузки</h1>\n"
                + "  <p class=\"hint\">Выберите два HTML-файла таблиц (скачанных через /get или /getHtml). Перетащите файлы в зоны или нажмите для выбора.</p>\n"
                + "  <form id=\"compareForm\" action=\"/compare\" method=\"post\" enctype=\"multipart/form-data\">\n"
                + "    <div>\n"
                + "      <label>Первый файл</label>\n"
                + "      <div class=\"upload-zone\" id=\"zone1\" data-input=\"input1\">\n"
                + "        <span class=\"label\" id=\"label1\">Перетащите файл сюда или нажмите для выбора</span>\n"
                + "        <input type=\"file\" name=\"file1\" id=\"input1\" accept=\".html,.htm\">\n"
                + "      </div>\n"
                + "    </div>\n"
                + "    <div>\n"
                + "      <label>Второй файл</label>\n"
                + "      <div class=\"upload-zone\" id=\"zone2\" data-input=\"input2\">\n"
                + "        <span class=\"label\" id=\"label2\">Перетащите файл сюда или нажмите для выбора</span>\n"
                + "        <input type=\"file\" name=\"file2\" id=\"input2\" accept=\".html,.htm\">\n"
                + "      </div>\n"
                + "    </div>\n"
                + "    <button type=\"submit\" class=\"btn\" id=\"submitBtn\" disabled>Сравнить таблицы</button>\n"
                + "  </form>\n"
                + "  <script>\n"
                + "    function setupZone(zoneId, inputId, labelId) {\n"
                + "      var zone = document.getElementById(zoneId);\n"
                + "      var input = document.getElementById(inputId);\n"
                + "      var label = document.getElementById(labelId);\n"
                + "      function setFile(f) {\n"
                + "        if (f && f.name) { label.textContent = f.name; zone.classList.add('has-file'); } else { label.textContent = 'Перетащите файл сюда или нажмите для выбора'; zone.classList.remove('has-file'); }\n"
                + "        updateSubmit();\n"
                + "      }\n"
                + "      zone.addEventListener('click', function() { input.click(); });\n"
                + "      input.addEventListener('change', function() { setFile(input.files[0]); });\n"
                + "      zone.addEventListener('dragover', function(e) { e.preventDefault(); zone.classList.add('dragover'); });\n"
                + "      zone.addEventListener('dragleave', function() { zone.classList.remove('dragover'); });\n"
                + "      zone.addEventListener('drop', function(e) { e.preventDefault(); zone.classList.remove('dragover'); input.files = e.dataTransfer.files; setFile(input.files[0]); });\n"
                + "    }\n"
                + "    function updateSubmit() {\n"
                + "      document.getElementById('submitBtn').disabled = !document.getElementById('input1').files.length || !document.getElementById('input2').files.length;\n"
                + "    }\n"
                + "    setupZone('zone1', 'input1', 'label1');\n"
                + "    setupZone('zone2', 'input2', 'label2');\n"
                + "  </script>\n"
                + "</body>\n"
                + "</html>";
    }
    
    private List<Deployment> generateTestData() {
        List<Deployment> deployments = new LinkedList<>();
        
        // Первый деплоймент - 3 пода
        Deployment deployment1 = new Deployment();
        deployment1.setName("frontend-deployment");
        deployment1.setPodCount(3);
        deployment1.setStartTime(45);
        deployment1.setContainers(new LinkedList<>());
        
        Container container1_1 = new Container();
        container1_1.setName("nginx-container");
        container1_1.setCpuRq(100);
        container1_1.setCpuLim(500);
        container1_1.setMemRq(128);
        container1_1.setMemLim(512);
        container1_1.setCpuMaxPercent(65);
        container1_1.setCpuAvgPercent(45);
        container1_1.setCpuMaxAbs(325);
        container1_1.setMemMaxPercent(70);
        container1_1.setMemAvgPercent(55);
        container1_1.setMemMaxAbs(358);
        container1_1.setThrottlingPercent(0);
        deployment1.getContainers().add(container1_1);
        
        Container container1_2 = new Container();
        container1_2.setName("app-container");
        container1_2.setCpuRq(200);
        container1_2.setCpuLim(1000);
        container1_2.setMemRq(256);
        container1_2.setMemLim(1024);
        container1_2.setCpuMaxPercent(35);
        container1_2.setCpuAvgPercent(25);
        container1_2.setCpuMaxAbs(350);
        container1_2.setMemMaxPercent(45);
        container1_2.setMemAvgPercent(30);
        container1_2.setMemMaxAbs(460);
        container1_2.setThrottlingPercent(2);
        deployment1.getContainers().add(container1_2);
        
        Container container1_3 = new Container();
        container1_3.setName("cache-container");
        container1_3.setCpuRq(50);
        container1_3.setCpuLim(200);
        container1_3.setMemRq(64);
        container1_3.setMemLim(256);
        container1_3.setCpuMaxPercent(15);
        container1_3.setCpuAvgPercent(10);
        container1_3.setCpuMaxAbs(30);
        container1_3.setMemMaxPercent(85);
        container1_3.setMemAvgPercent(75);
        container1_3.setMemMaxAbs(217);
        container1_3.setThrottlingPercent(4);
        deployment1.getContainers().add(container1_3);
        
        deployments.add(deployment1);
        
        // Второй деплоймент - 3 пода
        Deployment deployment2 = new Deployment();
        deployment2.setName("backend-deployment");
        deployment2.setPodCount(3);
        deployment2.setStartTime(95);
        deployment2.setContainers(new LinkedList<>());
        
        Container container2_1 = new Container();
        container2_1.setName("api-container");
        container2_1.setCpuRq(300);
        container2_1.setCpuLim(1500);
        container2_1.setMemRq(512);
        container2_1.setMemLim(2048);
        container2_1.setCpuMaxPercent(75);
        container2_1.setCpuAvgPercent(60);
        container2_1.setCpuMaxAbs(1125);
        container2_1.setMemMaxPercent(68);
        container2_1.setMemAvgPercent(50);
        container2_1.setMemMaxAbs(1392);
        container2_1.setThrottlingPercent(6);
        deployment2.getContainers().add(container2_1);
        
        Container container2_2 = new Container();
        container2_2.setName("db-container");
        container2_2.setCpuRq(500);
        container2_2.setCpuLim(2000);
        container2_2.setMemRq(1024);
        container2_2.setMemLim(4096);
        container2_2.setCpuMaxPercent(40);
        container2_2.setCpuAvgPercent(30);
        container2_2.setCpuMaxAbs(800);
        container2_2.setMemMaxPercent(55);
        container2_2.setMemAvgPercent(40);
        container2_2.setMemMaxAbs(2252);
        container2_2.setThrottlingPercent(1);
        deployment2.getContainers().add(container2_2);
        
        Container container2_3 = new Container();
        container2_3.setName("worker-container");
        container2_3.setCpuRq(150);
        container2_3.setCpuLim(800);
        container2_3.setMemRq(256);
        container2_3.setMemLim(1024);
        container2_3.setCpuMaxPercent(90);
        container2_3.setCpuAvgPercent(70);
        container2_3.setCpuMaxAbs(720);
        container2_3.setMemMaxPercent(25);
        container2_3.setMemAvgPercent(15);
        container2_3.setMemMaxAbs(256);
        container2_3.setThrottlingPercent(3);
        deployment2.getContainers().add(container2_3);
        
        deployments.add(deployment2);
        
        // Третий деплоймент - 3 пода
        Deployment deployment3 = new Deployment();
        deployment3.setName("monitoring-deployment");
        deployment3.setPodCount(3);
        deployment3.setStartTime(150);
        deployment3.setContainers(new LinkedList<>());
        
        Container container3_1 = new Container();
        container3_1.setName("prometheus-container");
        container3_1.setCpuRq(200);
        container3_1.setCpuLim(1000);
        container3_1.setMemRq(512);
        container3_1.setMemLim(2048);
        container3_1.setCpuMaxPercent(62);
        container3_1.setCpuAvgPercent(48);
        container3_1.setCpuMaxAbs(620);
        container3_1.setMemMaxPercent(58);
        container3_1.setMemAvgPercent(42);
        container3_1.setMemMaxAbs(1187);
        container3_1.setThrottlingPercent(5);
        deployment3.getContainers().add(container3_1);
        
        Container container3_2 = new Container();
        container3_2.setName("grafana-container");
        container3_2.setCpuRq(100);
        container3_2.setCpuLim(500);
        container3_2.setMemRq(256);
        container3_2.setMemLim(1024);
        container3_2.setCpuMaxPercent(18);
        container3_2.setCpuAvgPercent(12);
        container3_2.setCpuMaxAbs(90);
        container3_2.setMemMaxPercent(72);
        container3_2.setMemAvgPercent(58);
        container3_2.setMemMaxAbs(737);
        container3_2.setThrottlingPercent(0);
        deployment3.getContainers().add(container3_2);
        
        Container container3_3 = new Container();
        container3_3.setName("alertmanager-container");
        container3_3.setCpuRq(50);
        container3_3.setCpuLim(200);
        container3_3.setMemRq(128);
        container3_3.setMemLim(512);
        container3_3.setCpuMaxPercent(50);
        container3_3.setCpuAvgPercent(35);
        container3_3.setCpuMaxAbs(100);
        container3_3.setMemMaxPercent(95);
        container3_3.setMemAvgPercent(80);
        container3_3.setMemMaxAbs(486);
        container3_3.setThrottlingPercent(2);
        deployment3.getContainers().add(container3_3);
        
        deployments.add(deployment3);
        
        return deployments;
    }
}
