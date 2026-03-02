package com.mypolicy.pipeline.processing.service;

import com.mypolicy.pipeline.metadata.config.MetadataConfigLoader;
import com.mypolicy.pipeline.metadata.model.FieldMapping;
import com.mypolicy.pipeline.metadata.service.MetadataService;
import com.mypolicy.pipeline.ingestion.service.IngestionService;
import com.mypolicy.pipeline.ingestion.dto.ProgressUpdateRequest;
import com.mypolicy.pipeline.ingestion.dto.StatusUpdateRequest;
import com.mypolicy.pipeline.ingestion.model.IngestionStatus;
import com.mypolicy.pipeline.matching.service.MatchingService;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Processing Service: CSV/Excel parsing, metadata-driven field mapping, data massaging,
 * then customer matching and policy creation.
 */
@Service
public class ProcessingService {

  private static final Logger log = LoggerFactory.getLogger(ProcessingService.class);

  private final MetadataService metadataService;
  private final MetadataConfigLoader metadataConfigLoader;
  private final DataMassagingService dataMassagingService;
  private final IngestionService ingestionService;
  private final MatchingService matchingService;

  public ProcessingService(MetadataService metadataService, MetadataConfigLoader metadataConfigLoader,
      DataMassagingService dataMassagingService, IngestionService ingestionService, MatchingService matchingService) {
    this.metadataService = metadataService;
    this.metadataConfigLoader = metadataConfigLoader;
    this.dataMassagingService = dataMassagingService;
    this.ingestionService = ingestionService;
    this.matchingService = matchingService;
  }

  /**
   * Process uploaded file: parse CSV or Excel, apply mappings from YAML (or DB), massage data, match and create policies.
   * policyType can be null – then resolved from insurer config (e.g. HEALTH_INSURER -> HEALTH).
   */
  public void processFile(String jobId, String filePath, String insurerId, String policyTypeParam) {
    log.info("[Processing] Starting: jobId={}, insurerId={}, file={}", jobId, insurerId, filePath);

    try {
      StatusUpdateRequest statusUpdate = new StatusUpdateRequest();
      statusUpdate.setStatus(IngestionStatus.PROCESSING);
      ingestionService.updateStatus(jobId, statusUpdate);

      String policyType = policyTypeParam != null && !policyTypeParam.isBlank()
          ? policyTypeParam
          : metadataConfigLoader.resolvePolicyType(insurerId, null);

      List<FieldMapping> mappings = getMappings(insurerId, policyType);
      if (mappings == null || mappings.isEmpty()) {
        throw new RuntimeException("No field mappings for insurerId=" + insurerId + ", policyType=" + policyType
            + ". Add config in metadata/insurer-field-mappings.yaml or in DB.");
      }

      log.info("[Processing] Using {} field mappings for policyType={}", mappings.size(), policyType);

      List<Map<String, Object>> records = readAndMapFile(filePath, insurerId, policyType, mappings);
      ingestionService.setTotalRecords(jobId, records.size());

      int created = 0;
      List<Map<String, String>> verificationFailures = new ArrayList<>();
      for (Map<String, Object> record : records) {
        try {
          Optional<String> failureOpt = matchingService.processAndMatchPolicy(record);
          if (failureOpt.isEmpty()) {
            created++;
          } else {
            String policyNum = (String) record.get("policyNumber");
            Map<String, String> failure = new HashMap<>();
            failure.put("policyNumber", policyNum != null ? policyNum : "?");
            failure.put("reason", failureOpt.get());
            verificationFailures.add(failure);
          }
        } catch (Exception e) {
          log.error("[Processing] Matching failed for record: {}", record.get("policyNumber"), e);
          Map<String, String> failure = new HashMap<>();
          failure.put("policyNumber", String.valueOf(record.get("policyNumber")));
          failure.put("reason", "Unexpected error: " + e.getMessage());
          verificationFailures.add(failure);
        }
      }
      if (created > 0) {
        ProgressUpdateRequest progress = new ProgressUpdateRequest();
        progress.setProcessedRecordsDelta(created);
        ingestionService.updateProgress(jobId, progress);
      }
      if (!verificationFailures.isEmpty()) {
        ingestionService.addVerificationFailures(jobId, verificationFailures);
      }

      StatusUpdateRequest completedUpdate = new StatusUpdateRequest();
      completedUpdate.setStatus(IngestionStatus.COMPLETED);
      ingestionService.updateStatus(jobId, completedUpdate);
      log.info("[Processing] Completed jobId={}, total={}, created={}", jobId, records.size(), created);

    } catch (Exception e) {
      log.error("[Processing] Failed jobId={}", jobId, e);
      StatusUpdateRequest failedUpdate = new StatusUpdateRequest();
      failedUpdate.setStatus(IngestionStatus.FAILED);
      failedUpdate.setFailureReason(e.getMessage());
      ingestionService.updateStatus(jobId, failedUpdate);
      throw new RuntimeException("Processing failed: " + e.getMessage(), e);
    }
  }

  private List<FieldMapping> getMappings(String insurerId, String policyType) {
    try {
      List<FieldMapping> fromDb = metadataService.getMappingsForPolicyType(insurerId, policyType);
      if (fromDb != null && !fromDb.isEmpty()) return fromDb;
    } catch (Exception e) {
      log.debug("[Processing] No DB config for insurerId={}, using YAML", insurerId);
    }
    return metadataConfigLoader.getMappings(insurerId, policyType);
  }

  private List<Map<String, Object>> readAndMapFile(String filePath, String insurerId, String policyType,
      List<FieldMapping> mappings) throws IOException {
    String lower = filePath.toLowerCase();
    if (lower.endsWith(".csv")) {
      return readCsvAndMap(filePath, insurerId, policyType, mappings);
    }
    return readExcelAndMap(filePath, insurerId, policyType, mappings);
  }

  private List<Map<String, Object>> readCsvAndMap(String filePath, String insurerId, String policyType,
      List<FieldMapping> mappings) throws IOException {
    List<Map<String, Object>> records = new ArrayList<>();
    try (BufferedReader reader = Files.newBufferedReader(new File(filePath).toPath(), StandardCharsets.UTF_8)) {
      String firstLine = reader.readLine();
      if (firstLine == null || firstLine.isBlank()) {
        throw new IOException("CSV has no header row");
      }
      String[] headers = parseCsvLine(firstLine);
      Map<String, Integer> columnIndexMap = new HashMap<>();
      for (int i = 0; i < headers.length; i++) {
        String h = headers[i].trim();
        if (!h.isEmpty()) columnIndexMap.put(h, i);
      }
      String line;
      int rowNum = 0;
      while ((line = reader.readLine()) != null) {
        rowNum++;
        if (line.isBlank()) continue;
        String[] values = parseCsvLine(line);
        Map<String, Object> standardRecord = mapRowToStandard(values, columnIndexMap, mappings);
        standardRecord.put("insurerId", insurerId);
        standardRecord.put("policyType", policyType);
        records.add(standardRecord);
      }
      log.info("[Processing] CSV parsed: {} rows from {}", rowNum, filePath);
    }
    return records;
  }

  private List<Map<String, Object>> readExcelAndMap(String filePath, String insurerId, String policyType,
      List<FieldMapping> mappings) throws IOException {
    List<Map<String, Object>> records = new ArrayList<>();
    try (InputStream is = new FileInputStream(filePath);
         Workbook workbook = WorkbookFactory.create(is)) {
      Sheet sheet = workbook.getSheetAt(0);
      Row headerRow = sheet.getRow(0);
      if (headerRow == null) throw new IOException("Excel has no header row");
      Map<String, Integer> columnIndexMap = new HashMap<>();
      for (Cell cell : headerRow) {
        String val = getCellStringValue(cell);
        if (val != null && !val.isBlank()) columnIndexMap.put(val.trim(), cell.getColumnIndex());
      }
      int maxCol = columnIndexMap.values().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
      int rowCount = 0;
      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) continue;
        String[] values = new String[maxCol];
        for (int c = 0; c < row.getLastCellNum() && c < values.length; c++) {
          Cell cell = row.getCell(c);
          values[c] = getCellStringValue(cell);
          if (values[c] != null) values[c] = values[c].trim();
        }
        Map<String, Object> standardRecord = mapRowToStandard(values, columnIndexMap, mappings);
        standardRecord.put("insurerId", insurerId);
        standardRecord.put("policyType", policyType);
        records.add(standardRecord);
        rowCount++;
      }
      log.info("[Processing] Excel parsed: {} rows from {}", rowCount, filePath);
    }
    return records;
  }

  private Map<String, Object> mapRowToStandard(String[] values, Map<String, Integer> columnIndexMap,
      List<FieldMapping> mappings) {
    Map<String, Object> standardRecord = new HashMap<>();
    for (FieldMapping m : mappings) {
      Integer idx = columnIndexMap.get(m.getSourceField());
      Object raw = (idx != null && idx < values.length && values[idx] != null) ? values[idx].trim() : null;
      if (raw != null && raw.toString().isEmpty()) raw = null;
      Object value = m.getTransformFunction() != null && !m.getTransformFunction().isBlank()
          ? dataMassagingService.applyTransform(m.getTransformFunction(), raw)
          : raw;
      standardRecord.put(m.getTargetField(), value);
    }
    return standardRecord;
  }

  private Map<String, Object> mapRowToStandard(Map<String, Integer> columnIndexMap, Row row, List<FieldMapping> mappings) {
    Map<String, Object> standardRecord = new HashMap<>();
    for (FieldMapping m : mappings) {
      Integer idx = columnIndexMap.get(m.getSourceField());
      Object raw = null;
      if (idx != null) {
        Cell cell = row.getCell(idx);
        raw = getCellValue(cell);
      }
      Object value = m.getTransformFunction() != null && !m.getTransformFunction().isBlank()
          ? dataMassagingService.applyTransform(m.getTransformFunction(), raw)
          : raw;
      standardRecord.put(m.getTargetField(), value);
    }
    return standardRecord;
  }

  private static String[] parseCsvLine(String line) {
    List<String> tokens = new ArrayList<>();
    boolean inQuotes = false;
    StringBuilder current = new StringBuilder();
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (c == '"') inQuotes = !inQuotes;
      else if (c == ',' && !inQuotes) {
        tokens.add(current.toString().trim());
        current = new StringBuilder();
      } else current.append(c);
    }
    tokens.add(current.toString().trim());
    return tokens.toArray(new String[0]);
  }

  private static String getCellStringValue(Cell cell) {
    if (cell == null) return null;
    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue();
      case NUMERIC -> (cell.getNumericCellValue() == Math.floor(cell.getNumericCellValue()))
          ? String.valueOf((long) cell.getNumericCellValue()) : String.valueOf(cell.getNumericCellValue());
      case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
      default -> null;
    };
  }

  private Object getCellValue(Cell cell) {
    if (cell == null) return null;
    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue();
      case NUMERIC -> DateUtil.isCellDateFormatted(cell) ? cell.getLocalDateTimeCellValue() : cell.getNumericCellValue();
      case BOOLEAN -> cell.getBooleanCellValue();
      default -> null;
    };
  }
}
