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
        
        // Извлекаем все строки tr (с атрибутами class и т.д.)
        Pattern trPattern = Pattern.compile("<tr([^>]*)>(.*?)</tr>", Pattern.DOTALL);
        Matcher trMatcher = trPattern.matcher(tbody);
        
        String currentDeploymentName = null;
        int currentPodCount = 0;
        Deployment currentDeployment = null;
        int cellOffset = 0;
        
        while (trMatcher.find()) {
            String trAttrs = trMatcher.group(1);
            String row = trMatcher.group(2);
            if (trAttrs != null && trAttrs.contains("totals-row")) {
                continue;
            }
            
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
                if (cells.size() < 16) continue;
                currentPodCount = parseInt(cells.get(0));
                currentDeploymentName = cells.get(1);
                currentDeployment = new Deployment();
                currentDeployment.setName(currentDeploymentName);
                currentDeployment.setPodCount(currentPodCount);
                currentDeployment.setStartTime(cells.size() >= 16 ? parseInt(cells.get(15)) : 0);
                currentDeployment.setContainers(new LinkedList<>());
                deployments.add(currentDeployment);
                cellOffset = 0;
            } else {
                cellOffset = 2;
                if (cells.size() < 13) continue;
            }
            
            if (currentDeployment == null || cells.size() < 15 - cellOffset) continue;
            
            // Порядок: Container, CpuRq, CpuLim, MemRq, MemLim, CpuMaxUse, CpuAvgUse, CpuAvgAbsUse, CpuMaxAbsUse, MemMaxUse, MemAvgUse, MemAbsUse, Троттлинг [, Время старта]
            int ci = 2 - cellOffset;
            Container container = new Container();
            container.setName(cells.get(ci));
            container.setCpuRq(parseInt(cells.get(ci + 1)));
            container.setCpuLim(parseInt(cells.get(ci + 2)));
            container.setMemRq(parseInt(cells.get(ci + 3)));
            container.setMemLim(parseInt(cells.get(ci + 4)));
            container.setCpuMaxPercent(parseInt(cells.get(ci + 5)));
            container.setCpuAvgPercent(parseInt(cells.get(ci + 6)));
            container.setCpuAvgAbsUse(parseInt(cells.get(ci + 7)));
            container.setCpuMaxAbsUse(parseInt(cells.get(ci + 8)));
            container.setMemMaxPercent(parseInt(cells.get(ci + 9)));
            container.setMemAvgPercent(parseInt(cells.get(ci + 10)));
            container.setMemMaxAbs(parseInt(cells.get(ci + 11)));
            container.setThrottlingPercent(parseInt(cells.get(ci + 12)));
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

