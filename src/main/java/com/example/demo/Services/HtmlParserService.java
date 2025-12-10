package com.example.demo.Services;

import com.example.demo.DTO.Container;
import com.example.demo.DTO.Deployment;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HtmlParserService {

    public List<Deployment> parseHtmlTable(String html) {
        List<Deployment> deployments = new LinkedList<>();
        
        // Извлекаем tbody
        Pattern tbodyPattern = Pattern.compile("<tbody>(.*?)</tbody>", Pattern.DOTALL);
        Matcher tbodyMatcher = tbodyPattern.matcher(html);
        
        if (!tbodyMatcher.find()) {
            return deployments;
        }
        
        String tbody = tbodyMatcher.group(1);
        
        // Извлекаем все строки tr
        Pattern trPattern = Pattern.compile("<tr>(.*?)</tr>", Pattern.DOTALL);
        Matcher trMatcher = trPattern.matcher(tbody);
        
        String currentDeploymentName = null;
        int currentPodCount = 0;
        Deployment currentDeployment = null;
        int cellOffset = 0; // Смещение для ячеек (0 если есть rowspan, 2 если нет)
        
        while (trMatcher.find()) {
            String row = trMatcher.group(1);
            
            // Проверяем, есть ли rowspan в первой ячейке (количество подов)
            Pattern rowspanPattern = Pattern.compile("rowspan=\"(\\d+)\"");
            Matcher rowspanMatcher = rowspanPattern.matcher(row);
            
            boolean hasRowspan = rowspanMatcher.find();
            
            // Извлекаем все td элементы
            Pattern tdPattern = Pattern.compile("<td[^>]*>(.*?)</td>", Pattern.DOTALL);
            Matcher tdMatcher = tdPattern.matcher(row);
            
            List<String> cells = new LinkedList<>();
            while (tdMatcher.find()) {
                String cellContent = tdMatcher.group(1).trim();
                cells.add(cellContent);
            }
            
            if (hasRowspan) {
                // Новая группа деплоймента
                // Извлекаем количество подов (первая ячейка)
                currentPodCount = parseInt(cells.get(0));
                
                // Извлекаем название деплоймента (вторая ячейка)
                currentDeploymentName = cells.get(1);
                
                // Создаем новый деплоймент
                currentDeployment = new Deployment();
                currentDeployment.setName(currentDeploymentName);
                currentDeployment.setPodCount(currentPodCount);
                currentDeployment.setContainers(new LinkedList<>());
                deployments.add(currentDeployment);
                
                cellOffset = 0; // Первые две ячейки присутствуют
            } else {
                cellOffset = 2; // Первые две ячейки отсутствуют из-за rowspan
            }
            
            if (currentDeployment == null || cells.size() < 13 - cellOffset) {
                continue;
            }
            
            // Создаем контейнер из текущей строки
            Container container = new Container();
            container.setName(cells.get(2 - cellOffset)); // Название контейнера
            container.setCpuLim(parseInt(cells.get(3 - cellOffset))); // ЦПУ лимиты
            container.setCpuRq(parseInt(cells.get(4 - cellOffset))); // ЦПУ реквесты
            container.setMemLim(parseInt(cells.get(5 - cellOffset))); // Память лимиты
            container.setMemRq(parseInt(cells.get(6 - cellOffset))); // Память реквесты
            container.setCpuMaxPercent(parseInt(cells.get(7 - cellOffset))); // ЦПУ утилизация макс
            container.setCpuAvgPercent(parseInt(cells.get(8 - cellOffset))); // ЦПУ утилизация сред
            container.setCpuMaxAbs(parseInt(cells.get(9 - cellOffset))); // ЦПУ утилизация абс
            container.setMemMaxPercent(parseInt(cells.get(10 - cellOffset))); // Утилизация памяти макс
            container.setMemAvgPercent(parseInt(cells.get(11 - cellOffset))); // Утилизация памяти сред
            container.setMemMaxAbs(parseInt(cells.get(12 - cellOffset))); // Утилизация памяти абс
            
            currentDeployment.getContainers().add(container);
        }
        
        return deployments;
    }
    
    private int parseInt(String value) {
        try {
            // Удаляем все нечисловые символы, кроме минуса
            String cleaned = value.replaceAll("[^0-9-]", "");
            if (cleaned.isEmpty()) {
                return 0;
            }
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

