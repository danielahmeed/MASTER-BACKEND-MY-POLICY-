package com.mypolicy.pipeline.processing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Standardizes formats across insurer files: currency, dates, status codes, mobile.
 * Used after mapping so that canonical fields have consistent formats.
 */
@Service
public class DataMassagingService {

  private static final Logger log = LoggerFactory.getLogger(DataMassagingService.class);

  private static final Pattern DIGITS_ONLY = Pattern.compile("[^0-9]");
  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

  /**
   * Normalize date: accepts yyyyMMdd, yyyy-MM-dd, dd/MM/yyyy and returns ISO yyyy-MM-dd.
   */
  public String normalizeDate(Object value) {
    if (value == null) return null;
    String s = value.toString().trim();
    if (s.isEmpty()) return null;
    s = s.replace("/", "-").replaceAll("\\s", "");
    if (s.length() == 8 && s.matches("\\d{8}")) {
      try {
        return LocalDate.parse(s, YYYYMMDD).format(ISO);
      } catch (Exception e) {
        log.debug("Date parse failed for {}: {}", s, e.getMessage());
        return s;
      }
    }
    try {
      return LocalDate.parse(s, ISO).format(ISO);
    } catch (DateTimeParseException e) {
      try {
        return LocalDate.parse(s, DateTimeFormatter.ofPattern("dd-MM-yyyy")).format(ISO);
      } catch (DateTimeParseException e2) {
        log.debug("Date parse failed for {}: {}", s, e2.getMessage());
        return s;
      }
    }
  }

  /**
   * Normalize currency: strip symbols and commas, return numeric string or BigDecimal-safe string.
   */
  public String normalizeCurrency(Object value) {
    if (value == null) return "0";
    if (value instanceof Number) return value.toString();
    String s = value.toString().replaceAll("[^0-9.\\-]", "").trim();
    if (s.isEmpty()) return "0";
    return s;
  }

  /**
   * Parse currency to BigDecimal.
   */
  public BigDecimal parseCurrency(Object value) {
    String s = normalizeCurrency(value);
    try {
      return new BigDecimal(s);
    } catch (NumberFormatException e) {
      return BigDecimal.ZERO;
    }
  }

  /**
   * Normalize mobile: digits only, ensure country code handling (e.g. 91 prefix).
   */
  public String normalizeMobile(Object value) {
    if (value == null) return null;
    String s = DIGITS_ONLY.matcher(value.toString()).replaceAll("");
    if (s.isEmpty()) return null;
    if (s.length() == 10 && !s.startsWith("91")) return "91" + s;
    if (s.length() == 11 && s.startsWith("0")) return "91" + s.substring(1);
    return s;
  }

  /**
   * Normalize status codes to standard (ACTIVE, LAPSED, CANCELLED, etc.).
   */
  public String normalizeStatus(Object value) {
    if (value == null) return "ACTIVE";
    String s = value.toString().trim().toUpperCase();
    if (s.isEmpty()) return "ACTIVE";
    if (s.matches("ACTIVE|A|1|Y|YES|CURRENT")) return "ACTIVE";
    if (s.matches("LAPSED|LAPSE|L")) return "LAPSED";
    if (s.matches("CANCELLED|CANCEL|C|INACTIVE|N")) return "CANCELLED";
    if (s.matches("PENDING|P")) return "PENDING";
    return s;
  }

  /**
   * Apply transform by name (used from metadata transformFunction).
   */
  public Object applyTransform(String transformFunction, Object value) {
    if (transformFunction == null || transformFunction.isBlank()) return value;
    return switch (transformFunction) {
      case "normalizeDate" -> normalizeDate(value);
      case "normalizeCurrency" -> normalizeCurrency(value);
      case "normalizeMobile" -> normalizeMobile(value);
      case "normalizeStatus" -> normalizeStatus(value);
      default -> value;
    };
  }
}
