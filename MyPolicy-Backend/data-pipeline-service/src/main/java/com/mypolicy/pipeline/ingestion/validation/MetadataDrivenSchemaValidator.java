package com.mypolicy.pipeline.ingestion.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Validates uploaded CSV/Excel: non-empty file and presence of header row.
 * Strict column validation is driven by metadata config at processing time.
 */
@Component
public class MetadataDrivenSchemaValidator {

  private static final Logger log = LoggerFactory.getLogger(MetadataDrivenSchemaValidator.class);

  /**
   * Validates the file. policyType can be null.
   */
  public SchemaValidationResult validate(MultipartFile file, String insurerId, String policyType) {
    SchemaValidationResult result = new SchemaValidationResult();
    if (file == null || file.isEmpty()) {
      result.addHeaderError("file", "File is empty or missing");
      return result;
    }
    String filename = file.getOriginalFilename();
    if (filename == null || filename.isBlank()) {
      result.addHeaderError("file", "File name is missing");
      return result;
    }
    try {
      readHeaders(file, filename, result);
    } catch (Exception e) {
      log.warn("Schema validation error for {}: {}", filename, e.getMessage());
      result.addHeaderError("file", "Validation error: " + e.getMessage());
    }
    return result;
  }

  private void readHeaders(MultipartFile file, String filename, SchemaValidationResult result) throws Exception {
    int lastDot = filename.lastIndexOf('.');
    String ext = lastDot > 0 ? filename.substring(lastDot) : "";
    if (".csv".equalsIgnoreCase(ext)) {
      String content = new String(file.getBytes(), StandardCharsets.UTF_8);
      List<String> lines = content.lines().filter(l -> !l.isBlank()).limit(2).toList();
      if (lines.isEmpty()) {
        result.addHeaderError("file", "File has no content");
        return;
      }
      String[] parts = parseCsvLine(lines.get(0));
      if (parts.length == 0 || (parts.length == 1 && parts[0].isBlank())) {
        result.addHeaderError("file", "File has no header row");
      }
    } else {
      try (InputStream is = file.getInputStream()) {
        org.apache.poi.ss.usermodel.Workbook wb = org.apache.poi.ss.usermodel.WorkbookFactory.create(is);
        org.apache.poi.ss.usermodel.Sheet sheet = wb.getSheetAt(0);
        org.apache.poi.ss.usermodel.Row headerRow = sheet.getRow(0);
        if (headerRow == null || headerRow.getLastCellNum() <= 0) {
          result.addHeaderError("file", "File has no header row");
        }
      }
    }
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
}
