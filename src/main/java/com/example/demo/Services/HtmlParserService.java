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
        int cellOffset = 0;
        
        while (trMatcher.find()) {
            String row = trMatcher.group(1);
            
            Pattern rowspanPattern = Pattern.compile("rowspan=\"(\\d+)\"");
            Matcher rowspanMatcher = rowspanPattern.matcher(row);
            boolean hasRowspan = rowspanMatcher.find();
            
            Pattern tdPattern = Pattern.compile("<td[^>]*>(.*?)</td>", Pattern.DOTALL);
            Matcher tdMatcher = tdPattern.matcher(row);
            List<String> cells = new LinkedList<>();
            while (tdMatcher.find()) {
                cells.add(tdMatcher.group(1).trim());
            }
            
            if (hasRowspan) {
                if (cells.size() < 13) continue;
                currentPodCount = parseInt(cells.get(0));
                currentDeploymentName = cells.get(1);
                currentDeployment = new Deployment();
                currentDeployment.setName(currentDeploymentName);
                currentDeployment.setPodCount(currentPodCount);
                currentDeployment.setStartTime(cells.size() >= 14 ? parseInt(cells.get(13)) : 0);
                currentDeployment.setContainers(new LinkedList<>());
                deployments.add(currentDeployment);
                cellOffset = 0;
            } else {
                cellOffset = 2;
                if (cells.size() < 11) continue;
            }
            
            if (currentDeployment == null || cells.size() < 13 - cellOffset) continue;
            
            Container container = new Container();
            container.setName(cells.get(2 - cellOffset));
            container.setCpuLim(parseInt(cells.get(3 - cellOffset)));
            container.setCpuRq(parseInt(cells.get(4 - cellOffset)));
            container.setMemLim(parseInt(cells.get(5 - cellOffset)));
            container.setMemRq(parseInt(cells.get(6 - cellOffset)));
            container.setCpuMaxPercent(parseInt(cells.get(7 - cellOffset)));
            container.setCpuAvgPercent(parseInt(cells.get(8 - cellOffset)));
            container.setCpuMaxAbs(parseInt(cells.get(9 - cellOffset)));
            container.setMemMaxPercent(parseInt(cells.get(10 - cellOffset)));
            container.setMemAvgPercent(parseInt(cells.get(11 - cellOffset)));
            container.setMemMaxAbs(parseInt(cells.get(12 - cellOffset)));
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

