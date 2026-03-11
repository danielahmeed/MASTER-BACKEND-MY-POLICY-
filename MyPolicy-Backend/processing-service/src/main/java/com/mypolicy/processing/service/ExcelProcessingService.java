package com.mypolicy.processing.service;

import com.mypolicy.processing.client.CustomerClient;
import com.mypolicy.processing.client.MetadataClient;
import com.mypolicy.processing.client.PolicyClient;
import com.mypolicy.processing.dto.FieldMappingDTO;
import com.mypolicy.processing.dto.InsurerConfigurationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelProcessingService {

  private final MetadataClient metadataClient;
  private final PolicyClient policyClient;
  private final CustomerClient customerClient;

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

  /**
   * Process correction file: UPDATE existing records by Policy Number and PAN.
   * Expects columns: policyNumber (or Policy Number), PAN/panNumber, and corrected fields.
   */
  public void processCorrectionFile(String filePath, String insurerId) {
    log.info("Starting correction processing for file: {}, insurerId: {}", filePath, insurerId);

    Path path = Paths.get(filePath);
    if (!Files.exists(path)) {
      throw new RuntimeException("File not found: " + filePath);
    }

    String lower = filePath.toLowerCase();
    List<Map<String, String>> rows;
    if (lower.endsWith(".csv")) {
      rows = readCsvRows(path);
    } else {
      rows = readExcelRows(path);
    }

    if (rows.isEmpty()) {
      log.warn("Correction file has no data rows");
      return;
    }

    int updated = 0;
    int skipped = 0;
    String reason = "Bulk correction via file: " + path.getFileName();

    for (Map<String, String> row : rows) {
      String policyNumber = get(row, "policyNumber", "Policy Number", "PolicyNum");
      String pan = get(row, "panNumber", "PAN", "pan");

      if (policyNumber == null || policyNumber.isBlank()) {
        log.warn("Skipping row - missing policyNumber");
        skipped++;
        continue;
      }

      try {
        Map<String, Object> policyMap = policyClient.getPolicyByNumberAndInsurer(policyNumber, insurerId);
        String policyId = (String) policyMap.get("id");
        String customerId = (String) policyMap.get("customerId");

        // Customer corrections
        Map<String, Object> customerPayload = new HashMap<>();
        String mobile = get(row, "mobileNumber", "Mobile", "mobile");
        if (mobile != null && !mobile.isBlank()) customerPayload.put("mobileNumber", mobile);
        String customerName = get(row, "customerName", "Customer Name", "firstName");
        if (customerName != null && !customerName.isBlank()) {
          String[] parts = customerName.trim().split("\\s+", 2);
          customerPayload.put("firstName", parts[0]);
          if (parts.length > 1) customerPayload.put("lastName", parts[1]);
        }
        String email = get(row, "email", "Email");
        if (email != null && !email.isBlank()) customerPayload.put("email", email);
        String address = get(row, "address", "Address");
        if (address != null && !address.isBlank()) customerPayload.put("address", address);
        customerPayload.put("reason", reason);

        if (customerPayload.size() > 1) {
          customerClient.correctCustomer(customerId, customerPayload);
        }

        // Policy corrections
        Map<String, Object> policyPayload = new HashMap<>();
        String planName = get(row, "planName", "Plan Name");
        if (planName != null && !planName.isBlank()) policyPayload.put("planName", planName);
        policyPayload.put("reason", reason);

        if (policyPayload.size() > 1) {
          policyClient.correctPolicy(policyId, policyPayload);
        }

        updated++;
      } catch (Exception e) {
        log.warn("Skipping row policyNumber={}: {}", policyNumber, e.getMessage());
        skipped++;
      }
    }

    log.info("Correction complete: {} updated, {} skipped", updated, skipped);
  }

  private String get(Map<String, String> row, String... keys) {
    for (String k : keys) {
      String v = row.get(k);
      if (v != null && !v.isBlank()) return v.trim();
    }
    return null;
  }

  private List<Map<String, String>> readCsvRows(Path path) {
    try {
      List<String> lines = Files.readAllLines(path);
      if (lines.isEmpty()) return List.of();
      String[] headers = lines.get(0).split(",", -1);
      for (int i = 0; i < headers.length; i++) headers[i] = headers[i].trim();
      List<Map<String, String>> rows = new ArrayList<>();
      for (int i = 1; i < lines.size(); i++) {
        String[] values = lines.get(i).split(",", -1);
        Map<String, String> row = new HashMap<>();
        for (int j = 0; j < headers.length && j < values.length; j++) {
          row.put(headers[j], values[j] != null ? values[j].trim() : "");
        }
        rows.add(row);
      }
      return rows;
    } catch (IOException e) {
      throw new RuntimeException("Failed to read CSV: " + e.getMessage());
    }
  }

  private List<Map<String, String>> readExcelRows(Path path) {
    try (InputStream is = new FileInputStream(path.toFile());
        Workbook wb = WorkbookFactory.create(is)) {
      Sheet sheet = wb.getSheetAt(0);
      Row headerRow = sheet.getRow(0);
      if (headerRow == null) return List.of();
      String[] headers = new String[headerRow.getLastCellNum()];
      for (int i = 0; i < headers.length; i++) {
        Cell c = headerRow.getCell(i);
        headers[i] = c != null ? strVal(c) : "";
      }
      List<Map<String, String>> rows = new ArrayList<>();
      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) continue;
        Map<String, String> map = new HashMap<>();
        for (int j = 0; j < headers.length; j++) {
          Cell c = row.getCell(j);
          map.put(headers[j], c != null ? strVal(c) : "");
        }
        rows.add(map);
      }
      return rows;
    } catch (IOException e) {
      throw new RuntimeException("Failed to read Excel: " + e.getMessage());
    }
  }

  private String strVal(Cell c) {
    if (c == null) return "";
    return switch (c.getCellType()) {
      case STRING -> c.getStringCellValue();
      case NUMERIC -> DateUtil.isCellDateFormatted(c) ? c.getDateCellValue().toString() : String.valueOf((long) c.getNumericCellValue());
      default -> "";
    };
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
