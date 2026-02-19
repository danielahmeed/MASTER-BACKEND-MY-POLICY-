package com.mypolicy.pipeline.processing.service;

import com.mypolicy.pipeline.metadata.model.FieldMapping;
import com.mypolicy.pipeline.metadata.model.InsurerConfiguration;
import com.mypolicy.pipeline.metadata.service.MetadataService;
import com.mypolicy.pipeline.ingestion.service.IngestionService;
import com.mypolicy.pipeline.ingestion.dto.StatusUpdateRequest;
import com.mypolicy.pipeline.ingestion.model.IngestionStatus;
import com.mypolicy.pipeline.matching.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Processing Service: Excel/CSV parsing, field mapping, data transformation.
 * 
 * Consolidated Service: Part of data-pipeline-service.
 * OPTIMIZATION: Direct method calls to MetadataService and MatchingService (no HTTP overhead).
 */
@Service
@RequiredArgsConstructor
public class ProcessingService {

  private static final Logger log = LoggerFactory.getLogger(ProcessingService.class);

  private final MetadataService metadataService;    // Direct injection - no HTTP!
  private final IngestionService ingestionService;  // Direct injection - no HTTP!
  private final MatchingService matchingService;    // Direct injection - no HTTP!

  /**
   * Process uploaded file: parse Excel/CSV, apply mappings, transform data.
   * 
   * Consolidation Benefit: Metadata lookup is now a method call (< 1ms) instead of HTTP (~50ms).
   */
  public void processFile(String jobId, String filePath, String insurerId, String policyType) {
    log.info("[Processing] Starting file processing: jobId={}, insurerId={}, policyType={}", 
        jobId, insurerId, policyType);

    try {
      // Update job status to PROCESSING
      StatusUpdateRequest statusUpdate = new StatusUpdateRequest();
      statusUpdate.setStatus(IngestionStatus.PROCESSING);
      ingestionService.updateStatus(jobId, statusUpdate);

      // 1. Fetch Mapping Rules (DIRECT METHOD CALL - no HTTP!)
      log.debug("[Processing] Fetching metadata configuration for insurerId={}", insurerId);
      InsurerConfiguration config = metadataService.getConfiguration(insurerId);
      List<FieldMapping> mappings = metadataService.getMappingsForPolicyType(insurerId, policyType);

      if (mappings == null || mappings.isEmpty()) {
        throw new RuntimeException("No mappings found for policy type: " + policyType);
      }

      log.info("[Processing] Found {} field mappings for policyType={}", mappings.size(), policyType);

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

        log.info("[Processing] Processing {} rows from Excel", sheet.getLastRowNum());

        // Set total records for progress tracking
        ingestionService.setTotalRecords(jobId, sheet.getLastRowNum());

        // 3. Iterate Rows
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
          Row row = sheet.getRow(i);
          if (row == null)
            continue;

          Map<String, Object> standardRecord = new HashMap<>();

          for (FieldMapping mapping : mappings) {
            Integer colIndex = columnIndexMap.get(mapping.getSourceField());
            if (colIndex != null) {
              Cell cell = row.getCell(colIndex);
              Object cellValue = getCellValue(cell);
              standardRecord.put(mapping.getTargetField(), cellValue);
            } else if (mapping.isRequired()) {
              log.warn("[Processing] Required field '{}' not found in Excel headers", 
                  mapping.getSourceField());
            }
          }

          standardRecord.put("insurerId", insurerId);
          standardRecord.put("policyType", policyType);
          processedRecords.add(standardRecord);
        }

        log.info("[Processing] Parsed {} records, sending to Matching Engine", processedRecords.size());

        // 4. Send processed records to Matching Engine (DIRECT METHOD CALL - no HTTP!)
        for (Map<String, Object> record : processedRecords) {
          try {
            matchingService.processAndMatchPolicy(record);
          } catch (Exception e) {
            log.error("[Processing] Matching failed for record: {}", record.get("policyNumber"), e);
            // Continue processing other records
          }
        }

        log.info("[Processing] Processed and matched {} records successfully", processedRecords.size());

        // Update job status to COMPLETED
        StatusUpdateRequest completedUpdate = new StatusUpdateRequest();
        completedUpdate.setStatus(IngestionStatus.COMPLETED);
        ingestionService.updateStatus(jobId, completedUpdate);

        log.info("[Processing] File processing completed: jobId={}", jobId);

      } catch (IOException e) {
        log.error("[Processing] Error processing file: jobId={}", jobId, e);
        
        // Update job status to FAILED
        StatusUpdateRequest failedUpdate = new StatusUpdateRequest();
        failedUpdate.setStatus(IngestionStatus.FAILED);
        failedUpdate.setFailureReason("File processing failed: " + e.getMessage());
        ingestionService.updateStatus(jobId, failedUpdate);
        
        throw new RuntimeException("Error processing file", e);
      }

    } catch (Exception e) {
      log.error("[Processing] Processing failed for jobId={}", jobId, e);
      
      // Update job status to FAILED
      StatusUpdateRequest failedUpdate = new StatusUpdateRequest();
      failedUpdate.setStatus(IngestionStatus.FAILED);
      failedUpdate.setFailureReason(e.getMessage());
      ingestionService.updateStatus(jobId, failedUpdate);
      
      throw new RuntimeException("Processing failed", e);
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
