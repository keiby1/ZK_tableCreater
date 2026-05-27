package com.example.demo.Services;

import com.example.demo.DTO.DaCsvDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;

/**
 * Чтение выгрузки ДА из CSV или первого листа XLSX.
 */
@Service
public class DaResourceExportReaderService {

    @Autowired
    private DaCsvReaderService daCsvReaderService;

    @Autowired
    private DaXlsxReaderService daXlsxReaderService;

    public DaCsvDocument read(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            return daXlsxReaderService.read(file);
        }
        return daCsvReaderService.read(file);
    }
}
