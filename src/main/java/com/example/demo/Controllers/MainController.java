package com.example.demo.Controllers;

import com.example.demo.DTO.Container;
import com.example.demo.DTO.Deployment;
import com.example.demo.Services.HtmlComparisonService;
import com.example.demo.Services.HtmlParserService;
import com.example.demo.Services.HtmlTableService;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

@RestController
public class MainController {
    
    @Autowired
    private HtmlTableService htmlTableService;
    
    @Autowired
    private HtmlParserService htmlParserService;
    
    @Autowired
    private HtmlComparisonService htmlComparisonService;
    
    @RequestMapping("/test")
    public String test(){
        return "2";
    }
    
    @GetMapping("/getHtml")
    public ResponseEntity<String> getHtml() {
        List<Deployment> deployments = generateTestData();
        String html = htmlTableService.generateHtmlTable(deployments);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        headers.setContentDispositionFormData("attachment", "deployment_table.html");
        
        return new ResponseEntity<>(html, headers, HttpStatus.OK);
    }
    
    @GetMapping("/get")
    public ResponseEntity<String> get() {
        List<Deployment> deployments = generateTestData();
        String html = htmlTableService.generateHtmlTable(deployments);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        
        return new ResponseEntity<>(html, headers, HttpStatus.OK);
    }
    
    @RequestMapping("/compare")
    public ResponseEntity<String> compareHtmlFiles(
//            @RequestParam("file1") MultipartFile file1,
//            @RequestParam("file2") MultipartFile file2
    ) {
        //            URL resource1 = MainController.class.getClassLoader().getResource("deployment_table (1).html");
//            URL resource2 = MainController.class.getClassLoader().getResource("deployment_table (2).html");
//            File file1 = new File(resource1.toURI());
//            File file2 = new File(resource2.toURI());
        String html1 = "", html2 = "";
        try (InputStream is = MainController.class.getClassLoader().getResourceAsStream("deployment_table (1).html")) {
            html1 = new Scanner(is, "UTF-8").useDelimiter("\\A").next();
            System.out.println(html1);
        } catch (Exception e) {
            // Обработка
        }

        try (InputStream is = MainController.class.getClassLoader().getResourceAsStream("deployment_table (2).html")) {
            html2 = new Scanner(is, "UTF-8").useDelimiter("\\A").next();
            System.out.println(html2);
        } catch (Exception e) {
            // Обработка
        }

        // Читаем содержимое файлов
//            String html1 = new String(file1, StandardCharsets.UTF_8);
//            String html2 = new String(file2.toString(), StandardCharsets.UTF_8);

        // Парсим HTML таблицы
        List<Deployment> deployments1 = htmlParserService.parseHtmlTable(html1);
        List<Deployment> deployments2 = htmlParserService.parseHtmlTable(html2);

        // Генерируем таблицу сравнения
        String comparisonHtml = htmlComparisonService.generateComparisonTable(deployments1, deployments2);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);

        return new ResponseEntity<>(comparisonHtml, headers, HttpStatus.OK);
    }
    
    private List<Deployment> generateTestData() {
        List<Deployment> deployments = new LinkedList<>();
        
        // Первый деплоймент - 3 пода
        Deployment deployment1 = new Deployment();
        deployment1.setName("frontend-deployment");
        deployment1.setPodCount(3);
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
        deployment1.getContainers().add(container1_3);
        
        deployments.add(deployment1);
        
        // Второй деплоймент - 3 пода
        Deployment deployment2 = new Deployment();
        deployment2.setName("backend-deployment");
        deployment2.setPodCount(3);
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
        deployment2.getContainers().add(container2_3);
        
        deployments.add(deployment2);
        
        // Третий деплоймент - 3 пода
        Deployment deployment3 = new Deployment();
        deployment3.setName("monitoring-deployment");
        deployment3.setPodCount(3);
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
        deployment3.getContainers().add(container3_3);
        
        deployments.add(deployment3);
        
        return deployments;
    }
}
