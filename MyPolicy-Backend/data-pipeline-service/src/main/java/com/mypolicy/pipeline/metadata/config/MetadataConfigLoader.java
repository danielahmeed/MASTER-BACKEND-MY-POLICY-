package com.mypolicy.pipeline.metadata.config;

import com.mypolicy.pipeline.metadata.model.FieldMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;

/**
 * Loads metadata-driven field mappings from classpath YAML (insurer-field-mappings.yaml).
 * Maps source CSV/Excel headers to canonical schema without hard-coding.
 */
@Component
public class MetadataConfigLoader {

  private static final Logger log = LoggerFactory.getLogger(MetadataConfigLoader.class);
  private static final String CONFIG_PATH = "metadata/insurer-field-mappings.yaml";

  private Map<String, Map<String, Object>> insurersConfig;

  @PostConstruct
  public void load() {
    insurersConfig = new HashMap<>();
    try {
      ClassPathResource resource = new ClassPathResource(CONFIG_PATH);
      if (!resource.exists()) {
        log.warn("Metadata config not found: {}", CONFIG_PATH);
        return;
      }
      try (InputStream is = resource.getInputStream()) {
        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
        @SuppressWarnings("unchecked")
        Map<String, Object> root = yaml.load(is);
        if (root == null) return;
        Object ins = root.get("insurers");
        if (ins instanceof Map) {
          @SuppressWarnings("unchecked")
          Map<String, Object> insMap = (Map<String, Object>) ins;
          for (Map.Entry<String, Object> e : insMap.entrySet()) {
            if (e.getValue() instanceof Map) {
              @SuppressWarnings("unchecked")
              Map<String, Object> val = (Map<String, Object>) e.getValue();
              insurersConfig.put(e.getKey(), val);
            }
          }
        }
      }
      log.info("Loaded metadata config for {} insurers from {}", insurersConfig.size(), CONFIG_PATH);
    } catch (Exception e) {
      log.error("Failed to load metadata config from " + CONFIG_PATH, e);
    }
  }

  /**
   * Returns field mappings for the given insurer and policy type, or empty list if not found.
   */
  public List<FieldMapping> getMappings(String insurerId, String policyType) {
    if (insurersConfig == null) load();
    Map<String, Object> ins = insurersConfig.get(insurerId);
    if (ins == null) return Collections.emptyList();
    Object policyTypes = ins.get("policyTypes");
    if (!(policyTypes instanceof Map)) return Collections.emptyList();
    @SuppressWarnings("unchecked")
    Map<String, Object> ptMap = (Map<String, Object>) policyTypes;
    Object ptConfig = ptMap.get(policyType);
    if (!(ptConfig instanceof Map)) return Collections.emptyList();
    @SuppressWarnings("unchecked")
    Map<String, Object> config = (Map<String, Object>) ptConfig;
    Object mappings = config.get("fieldMappings");
    if (!(mappings instanceof List)) return Collections.emptyList();
    List<FieldMapping> result = new ArrayList<>();
    for (Object m : (List<?>) mappings) {
      if (m instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) m;
        FieldMapping fm = new FieldMapping();
        fm.setSourceField((String) map.get("sourceField"));
        fm.setTargetField((String) map.get("targetField"));
        fm.setDataType(map.get("dataType") != null ? map.get("dataType").toString() : "STRING");
        fm.setRequired(Boolean.TRUE.equals(map.get("required")));
        fm.setTransformFunction((String) map.get("transformFunction"));
        result.add(fm);
      }
    }
    return result;
  }

  /**
   * Resolves policy type from insurer config (e.g. HEALTH, MOTOR, TERM_LIFE) or returns default.
   */
  public String resolvePolicyType(String insurerId, String hintPolicyType) {
    if (insurerId == null || insurersConfig == null) return hintPolicyType != null ? hintPolicyType : "GENERIC";
    Map<String, Object> ins = insurersConfig.get(insurerId);
    if (ins == null) return hintPolicyType != null ? hintPolicyType : "GENERIC";
    Object policyTypes = ins.get("policyTypes");
    if (!(policyTypes instanceof Map)) return hintPolicyType != null ? hintPolicyType : "GENERIC";
    @SuppressWarnings("unchecked")
    Map<String, Object> ptMap = (Map<String, Object>) policyTypes;
    if (hintPolicyType != null && ptMap.containsKey(hintPolicyType)) return hintPolicyType;
    return ptMap.keySet().iterator().next();
  }

  /**
   * Returns list of configured insurer IDs.
   */
  public Set<String> getConfiguredInsurerIds() {
    if (insurersConfig == null) load();
    return insurersConfig != null ? insurersConfig.keySet() : Collections.emptySet();
  }
}
