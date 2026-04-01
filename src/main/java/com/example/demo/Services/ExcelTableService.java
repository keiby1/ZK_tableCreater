package com.example.demo.Services;

import com.example.demo.DTO.Container;
import com.example.demo.DTO.Deployment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Выгрузка таблицы деплойментов в Excel (.xlsx), те же данные и порядок столбцов, что и в HTML-таблице.
 */
@Service
public class ExcelTableService {

    private static final int LAST_COL = 15;

    @Autowired
    private HtmlTableService htmlTableService;

    public byte[] buildDeploymentTableXlsx(List<Deployment> deployments, Long from, Long to) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Deployments");
            int rowIdx = 0;

            Row intervalRow = sheet.createRow(rowIdx++);
            intervalRow.createCell(0).setCellValue("Интервал выгрузки: " + htmlTableService.getIntervalLabel(from, to));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, LAST_COL));

            Row headerRow = sheet.createRow(rowIdx++);
            String[] headers = {
                    "Количество подов",
                    "Workload",
                    "Container",
                    "CpuRq",
                    "CpuLim",
                    "MemRq",
                    "MemLim",
                    "CpuMaxUse",
                    "CpuAvgUse",
                    "CpuAvgAbsUse",
                    "CpuMaxAbsUse",
                    "MemMaxUse",
                    "MemAvgUse",
                    "MemAbsUse",
                    "Троттлинг",
                    "Время старта"
            };
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            long sumCpuLim = 0;
            long sumCpuRq = 0;
            long sumMemLim = 0;
            long sumMemRq = 0;
            long sumCpuAvgAbsUse = 0;
            long sumCpuMaxAbsUse = 0;
            long sumMemAbsUse = 0;
            long sumCpuMaxUse = 0;
            long sumCpuAvgUse = 0;
            long sumMemMaxUse = 0;
            long sumMemAvgUse = 0;
            long sumThrottlingPercent = 0;
            int totalContainers = 0;

            List<Deployment> sortedDeployments = deployments.stream()
                    .sorted(Comparator.comparing(Deployment::getName))
                    .collect(Collectors.toList());

            for (Deployment deployment : sortedDeployments) {
                List<Container> sortedContainers = deployment.getContainers().stream()
                        .sorted(Comparator.comparing(Container::getName))
                        .collect(Collectors.toList());

                for (Container container : sortedContainers) {
                    Row row = sheet.createRow(rowIdx++);
                    int col = 0;
                    setLong(row, col++, deployment.getPodCount());
                    setString(row, col++, deployment.getName());
                    setString(row, col++, container.getName());
                    setLong(row, col++, container.getCpuRq());
                    setLong(row, col++, container.getCpuLim());
                    setLong(row, col++, container.getMemRq());
                    setLong(row, col++, container.getMemLim());
                    setPercent(row, col++, container.getCpuMaxPercent());
                    setPercent(row, col++, container.getCpuAvgPercent());
                    setLong(row, col++, container.getCpuAvgAbsUse());
                    setLong(row, col++, container.getCpuMaxAbsUse());
                    setPercent(row, col++, container.getMemMaxPercent());
                    setPercent(row, col++, container.getMemAvgPercent());
                    setLong(row, col++, container.getMemMaxAbs());
                    setPercent(row, col++, container.getThrottlingPercent());
                    setString(row, col, deployment.getStartTime() + " с");

                    sumCpuRq += container.getCpuRq();
                    sumCpuLim += container.getCpuLim();
                    sumMemRq += container.getMemRq();
                    sumMemLim += container.getMemLim();
                    sumCpuMaxUse += container.getCpuMaxPercent();
                    sumCpuAvgUse += container.getCpuAvgPercent();
                    sumCpuAvgAbsUse += container.getCpuAvgAbsUse();
                    sumCpuMaxAbsUse += container.getCpuMaxAbsUse();
                    sumMemMaxUse += container.getMemMaxPercent();
                    sumMemAvgUse += container.getMemAvgPercent();
                    sumMemAbsUse += container.getMemMaxAbs();
                    sumThrottlingPercent += container.getThrottlingPercent();
                    totalContainers++;
                }
            }

            Row totals = sheet.createRow(rowIdx++);
            int tc = 0;
            setString(totals, tc++, "—");
            setString(totals, tc++, "—");
            setString(totals, tc++, "Итого");
            setLong(totals, tc++, sumCpuRq);
            setLong(totals, tc++, sumCpuLim);
            setLong(totals, tc++, sumMemRq);
            setLong(totals, tc++, sumMemLim);
            if (totalContainers > 0) {
                setPercent(totals, tc++, Math.round((double) sumCpuMaxUse / totalContainers));
                setPercent(totals, tc++, Math.round((double) sumCpuAvgUse / totalContainers));
            } else {
                setString(totals, tc++, "—");
                setString(totals, tc++, "—");
            }
            setLong(totals, tc++, sumCpuAvgAbsUse);
            setLong(totals, tc++, sumCpuMaxAbsUse);
            if (totalContainers > 0) {
                setPercent(totals, tc++, Math.round((double) sumMemMaxUse / totalContainers));
                setPercent(totals, tc++, Math.round((double) sumMemAvgUse / totalContainers));
            } else {
                setString(totals, tc++, "—");
                setString(totals, tc++, "—");
            }
            setLong(totals, tc++, sumMemAbsUse);
            if (totalContainers > 0) {
                setPercent(totals, tc++, Math.round((double) sumThrottlingPercent / totalContainers));
            } else {
                setString(totals, tc++, "—");
            }
            setString(totals, tc, "—");

            for (int i = 0; i <= LAST_COL; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void setLong(Row row, int col, long value) {
        row.createCell(col).setCellValue(value);
    }

    private static void setString(Row row, int col, String value) {
        row.createCell(col).setCellValue(value != null ? value : "");
    }

    private static void setPercent(Row row, int col, long percent) {
        row.createCell(col).setCellValue(percent + "%");
    }

}
