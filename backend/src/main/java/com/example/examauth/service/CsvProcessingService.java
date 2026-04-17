package com.example.examauth.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvProcessingService {

    public List<String[]> parseCsv(MultipartFile file) throws Exception {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isFirstLine = true;
            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header
                }
                if (line.trim().isEmpty())
                    continue;
                String[] columns = line.split(",", -1);

                // Trim all elements
                for (int i = 0; i < columns.length; i++) {
                    columns[i] = columns[i].trim();
                }

                rows.add(columns);
            }
        }
        return rows;
    }
}
