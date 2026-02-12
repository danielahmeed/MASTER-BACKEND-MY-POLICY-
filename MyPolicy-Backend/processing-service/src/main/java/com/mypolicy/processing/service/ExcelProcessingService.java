package com.mypolicy.processing.service;

import com.mypolicy.processing.client.MetadataClient;
import com.mypolicy.processing.dto.FieldMappingDTO;
import com.mypolicy.processing.dto.InsurerConfigurationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelProcessingService {

  private final MetadataClient metadataClient;

  // TODO: In real app, listen to Kafka event with jobId, filePath, insurerId
  public void processFile(String filePath, String insurerId, String policyType) {
    log.info("Starting processing for file: {}", filePath);

    // 1. Fetch Mapping Rules
    InsurerConfigurationDTO config = metadataClient.getConfiguration(insurerId);
    List<FieldMappingDTO> mappings = config.getFieldMappings().get(policyType);

    if (mappings == null) {
      throw new RuntimeException("No mappings found for policy type: " + policyType);
    }

    List<Map<String, Object>> processedRecords = new ArrayList<>();

    // 2. Read Excel
    try (InputStream is = new FileInputStream(filePath);
        Workbook workbook = WorkbookFactory.create(is)) {

      Sheet sheet = workbook.getSheetAt(0);
      Row headerRow = sheet.getRow(0);
      Map<String, Integer> columnIndexMap = new HashMap<>();

      // Map headers to column index
      for (Cell cell : headerRow) {
        columnIndexMap.put(cell.getStringCellValue(), cell.getColumnIndex());
      }

      // 3. Iterate Rows
      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null)
          continue;

        Map<String, Object> standardRecord = new HashMap<>();

        for (FieldMappingDTO mapping : mappings) {
          Integer colIndex = columnIndexMap.get(mapping.getSourceField());
          if (colIndex != null) {
            Cell cell = row.getCell(colIndex);
            standardRecord.put(mapping.getTargetField(), getCellValue(cell));
          }
        }

        standardRecord.put("insurerId", insurerId);
        standardRecord.put("policyType", policyType);
        processedRecords.add(standardRecord);
      }

      // 4. TODO: Send processed records to Matching Engine (via Kafka)
      log.info("Processed {} records successfully", processedRecords.size());
      // System.out.println(processedRecords); // Debug

    } catch (IOException e) {
      log.error("Error processing file", e);
      throw new RuntimeException("Error processing file", e);
    }
  }

  private Object getCellValue(Cell cell) {
    if (cell == null)
      return null;
    switch (cell.getCellType()) {
      case STRING:
        return cell.getStringCellValue();
      case NUMERIC:
        if (DateUtil.isCellDateFormatted(cell))
          return cell.getDateCellValue();
        return cell.getNumericCellValue();
      case BOOLEAN:
        return cell.getBooleanCellValue();
      default:
        return null;
    }
  }
}
