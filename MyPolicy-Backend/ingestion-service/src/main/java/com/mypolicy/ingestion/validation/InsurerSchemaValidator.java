package com.mypolicy.ingestion.validation;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Validates uploaded CSV/Excel files against the Insurer CSV schema.
 * Enforces required columns (with aliases), data types, and format rules.
 * Skipped for correction files.
 */
@Component
public class InsurerSchemaValidator {

  private static final Set<String> REQUIRED_COLUMNS = Set.of(
      "policyNumber", "customerName", "insurerId", "policyType", "sumAssured", "premiumAmount",
      "startDate", "endDate", "email", "mobileNumber", "panNumber", "dateOfBirth", "planName", "city");

  private static final Set<String> VALID_POLICY_TYPES = Set.of(
      "TERM_LIFE", "HEALTH", "MOTOR", "HOME", "TRAVEL");

  private static final Map<String, Set<String>> COLUMN_ALIASES = Map.ofEntries(
      Map.entry("policyNumber", Set.of("policyNumber", "PolicyNumber", "Policy Num", "PolicyNum")),
      Map.entry("customerName", Set.of("customerName", "CustomerName", "Customer Name")),
      Map.entry("insurerId", Set.of("insurerId", "InsurerId", "Insurer", "insurer")),
      Map.entry("policyType", Set.of("policyType", "PolicyType", "Policy Type")),
      Map.entry("sumAssured", Set.of("sumAssured", "SumAssured", "Coverage Amount", "IDV", "idv")),
      Map.entry("premiumAmount", Set.of("premiumAmount", "PremiumAmount", "AnnualPremium", "AnnualPrem")),
      Map.entry("startDate", Set.of("startDate", "StartDate", "PolicyStart", "PolicyStartDate")),
      Map.entry("endDate", Set.of("endDate", "EndDate", "PolicyEnd", "PolicyEndDate")),
      Map.entry("email", Set.of("email", "Email")),
      Map.entry("mobileNumber", Set.of("mobileNumber", "MobileNumber", "Mobile", "mobile")),
      Map.entry("panNumber", Set.of("panNumber", "PanNumber", "PAN", "pan")),
      Map.entry("dateOfBirth", Set.of("dateOfBirth", "DateOfBirth", "DOB", "dob")),
      Map.entry("planName", Set.of("planName", "PlanName", "Plan Name")),
      Map.entry("city", Set.of("city", "City")));

  private static final Pattern EMAIL_PATTERN = Pattern.compile(
      "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
  private static final Pattern INSURER_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_ ]{2,}$");
  private static final Pattern MOBILE_PATTERN = Pattern.compile("^[0-9]{10,15}$");

  private static final int MAX_ROWS_TO_VALIDATE = 50;

  /**
   * Validates the file against the Insurer CSV schema.
   *
   * @param file      The uploaded file (CSV or Excel)
   * @param insurerId Insurer ID from request (used if not in file; validated for format)
   * @return SchemaValidationResult with any errors; call isValid() to check
   */
  public SchemaValidationResult validate(MultipartFile file, String insurerId) {
    SchemaValidationResult result = new SchemaValidationResult();
    String filename = file.getOriginalFilename();
    if (filename == null) return result;

    try {
      String ext = filename.substring(filename.lastIndexOf('.'));
      if (".csv".equalsIgnoreCase(ext)) {
        validateCsv(file, insurerId, result);
      } else if (".xls".equalsIgnoreCase(ext) || ".xlsx".equalsIgnoreCase(ext)) {
        validateExcel(file, insurerId, result);
      }
    } catch (Exception e) {
      result.addHeaderError("file", "Failed to parse file: " + e.getMessage());
    }
    return result;
  }

  private void validateCsv(MultipartFile file, String insurerIdParam, SchemaValidationResult result)
      throws Exception {
    String content = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
    List<String> lines = content.lines().filter(l -> !l.isBlank()).toList();
    if (lines.isEmpty()) {
      result.addHeaderError("file", "File has no content");
      return;
    }

    String[] headers = parseCsvLine(lines.get(0));
    Map<String, Integer> headerToIndex = new HashMap<>();
    for (int i = 0; i < headers.length; i++) {
      headerToIndex.put(headers[i].trim(), i);
    }

    Map<String, Integer> standardToIndex = mapHeadersToStandard(headerToIndex, result);
    if (!result.isValid()) return;

    // Validate insurerId param if not in file
    if (!standardToIndex.containsKey("insurerId") && insurerIdParam != null) {
      validateInsurerIdFormat(insurerIdParam, 0, result);
    }

    int rowsToValidate = Math.min(lines.size() - 1, MAX_ROWS_TO_VALIDATE);
    for (int r = 1; r <= rowsToValidate; r++) {
      String[] values = parseCsvLine(lines.get(r));
      validateRow(values, standardToIndex, r + 1, result);
      if (result.getErrors().size() >= 20) break; // Cap errors
    }
  }

  private void validateExcel(MultipartFile file, String insurerIdParam, SchemaValidationResult result)
      throws Exception {
    try (InputStream is = file.getInputStream();
         Workbook wb = WorkbookFactory.create(is)) {
      Sheet sheet = wb.getSheetAt(0);
      Row headerRow = sheet.getRow(0);
      if (headerRow == null) {
        result.addHeaderError("file", "File has no header row");
        return;
      }

      Map<String, Integer> headerToIndex = new HashMap<>();
      for (int i = 0; i < headerRow.getLastCellNum(); i++) {
        Cell c = headerRow.getCell(i);
        String val = getCellStringValue(c);
        if (val != null && !val.isBlank()) headerToIndex.put(val.trim(), i);
      }

      Map<String, Integer> standardToIndex = mapHeadersToStandard(headerToIndex, result);
      if (!result.isValid()) return;

      if (!standardToIndex.containsKey("insurerId") && insurerIdParam != null) {
        validateInsurerIdFormat(insurerIdParam, 0, result);
      }

      int lastRow = Math.min(sheet.getLastRowNum(), MAX_ROWS_TO_VALIDATE);
      int maxCol = headerToIndex.values().stream().mapToInt(Integer::intValue).max().orElse(0);
      for (int r = 1; r <= lastRow; r++) {
        Row row = sheet.getRow(r);
        if (row == null) continue;
        String[] values = new String[maxCol + 1];
        for (int c = 0; c <= maxCol; c++) {
          Cell cell = row.getCell(c);
          values[c] = getCellStringValue(cell);
        }
        validateRow(values, standardToIndex, r + 1, result);
        if (result.getErrors().size() >= 20) break;
      }
    }
  }

  private Map<String, Integer> mapHeadersToStandard(Map<String, Integer> headerToIndex,
                                                     SchemaValidationResult result) {
    Map<String, Integer> standardToIndex = new HashMap<>();
    for (String std : REQUIRED_COLUMNS) {
      Integer idx = null;
      for (String alias : COLUMN_ALIASES.getOrDefault(std, Set.of(std))) {
        idx = headerToIndex.get(alias);
        if (idx != null) break;
      }
      if (idx == null) {
        result.addHeaderError(std, "Missing required column: " + std +
            " (or alias: " + String.join(", ", COLUMN_ALIASES.getOrDefault(std, Set.of(std))) + ")");
      } else {
        standardToIndex.put(std, idx);
      }
    }
    return standardToIndex;
  }

  private void validateRow(String[] values, Map<String, Integer> standardToIndex, int rowNum,
                           SchemaValidationResult result) {
    for (Map.Entry<String, Integer> e : standardToIndex.entrySet()) {
      String std = e.getKey();
      int idx = e.getValue();
      String val = idx < values.length && values[idx] != null ? values[idx].trim() : "";

      if (val.isBlank()) {
        result.addError(rowNum, std, "Value is required", val);
        continue;
      }

      switch (std) {
        case "policyNumber", "customerName", "planName", "city" -> { /* string, any non-empty ok */ }
        case "insurerId" -> validateInsurerIdFormat(val, rowNum, result);
        case "policyType" -> validatePolicyType(val, rowNum, result);
        case "sumAssured" -> validateSumAssured(val, rowNum, result);
        case "premiumAmount" -> validatePremiumAmount(val, rowNum, result);
        case "startDate", "endDate", "dateOfBirth" -> validateDate(val, rowNum, std, result);
        case "email" -> validateEmail(val, rowNum, result);
        case "mobileNumber" -> validateMobile(val, rowNum, result);
        case "panNumber" -> validatePan(val, rowNum, result);
        default -> { }
      }
    }
  }

  private void validateInsurerIdFormat(String val, int rowNum, SchemaValidationResult result) {
    if (!INSURER_ID_PATTERN.matcher(val).matches()) {
      result.addError(rowNum, "insurerId", "Must be alphanumeric with underscores (e.g. HDFC_LIFE)", val);
    }
  }

  private void validatePolicyType(String val, int rowNum, SchemaValidationResult result) {
    if (!VALID_POLICY_TYPES.contains(val.toUpperCase())) {
      result.addError(rowNum, "policyType",
          "Must be one of: TERM_LIFE, HEALTH, MOTOR, HOME, TRAVEL", val);
    }
  }

  private void validateSumAssured(String val, int rowNum, SchemaValidationResult result) {
    try {
      double n = Double.parseDouble(val.replace(",", "").trim());
      if (n <= 0) result.addError(rowNum, "sumAssured", "Must be greater than 0", val);
    } catch (NumberFormatException e) {
      result.addError(rowNum, "sumAssured", "Invalid number format", val);
    }
  }

  private void validatePremiumAmount(String val, int rowNum, SchemaValidationResult result) {
    try {
      double n = Double.parseDouble(val.replace(",", "").trim());
      if (n < 0) result.addError(rowNum, "premiumAmount", "Must be >= 0", val);
    } catch (NumberFormatException e) {
      result.addError(rowNum, "premiumAmount", "Invalid number format", val);
    }
  }

  private void validateDate(String val, int rowNum, String field, SchemaValidationResult result) {
    if (!parseDate(val)) result.addError(rowNum, field, "Invalid date format (use YYYY-MM-DD or YYYYMMDD)", val);
  }

  private boolean parseDate(String val) {
    if (val == null || val.isBlank()) return false;
    val = val.replace("-", "").replace("/", "").trim();
    if (val.length() == 8) {
      try {
        int y = Integer.parseInt(val.substring(0, 4));
        int m = Integer.parseInt(val.substring(4, 6));
        int d = Integer.parseInt(val.substring(6, 8));
        LocalDate.of(y, m, d);
        return true;
      } catch (Exception e) {
        return false;
      }
    }
    try {
      LocalDate.parse(val, DateTimeFormatter.ISO_LOCAL_DATE);
      return true;
    } catch (DateTimeParseException e) {
      return false;
    }
  }

  private void validateEmail(String val, int rowNum, SchemaValidationResult result) {
    if (!EMAIL_PATTERN.matcher(val).matches()) result.addError(rowNum, "email", "Invalid email format", val);
  }

  private void validateMobile(String val, int rowNum, SchemaValidationResult result) {
    String digits = val.replaceAll("[^0-9]", "");
    if (digits.length() < 10) result.addError(rowNum, "mobileNumber", "Must have at least 10 digits", val);
  }

  private void validatePan(String val, int rowNum, SchemaValidationResult result) {
    String cleaned = val.replaceAll("\\s", "");
    if (cleaned.length() != 10) result.addError(rowNum, "panNumber", "PAN must be 10 characters", val);
  }

  private String[] parseCsvLine(String line) {
    List<String> tokens = new ArrayList<>();
    boolean inQuotes = false;
    StringBuilder current = new StringBuilder();
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (c == '"') {
        inQuotes = !inQuotes;
      } else if ((c == ',' && !inQuotes)) {
        tokens.add(current.toString().trim());
        current = new StringBuilder();
      } else {
        current.append(c);
      }
    }
    tokens.add(current.toString().trim());
    return tokens.toArray(new String[0]);
  }

  private String getCellStringValue(Cell cell) {
    if (cell == null) return "";
    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue();
      case NUMERIC -> {
        if (DateUtil.isCellDateFormatted(cell)) {
          try {
            yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
          } catch (Exception e) {
            yield String.valueOf(cell.getNumericCellValue());
          }
        }
        double n = cell.getNumericCellValue();
        yield (n == Math.floor(n) && !Double.isInfinite(n))
            ? String.valueOf((long) n) : String.valueOf(n);
      }
      case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
      default -> "";
    };
  }
}
