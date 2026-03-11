package com.mypolicy.pipeline.metadata.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Insurer Configuration Entity
 * Stores field mapping rules for different insurers
 * Part of Metadata Module in consolidated Data Pipeline Service
 * 
 * Stored in PostgreSQL (mypolicy_db) - insurer_configurations table
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "insurer_configurations")
public class InsurerConfiguration implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String configId;

  @Column(nullable = false, unique = true, name = "insurer_id")
  private String insurerId;

  @Column(nullable = false, name = "insurer_name")
  private String insurerName;

  @Type(JsonType.class)
  @Column(columnDefinition = "varchar(65535)", name = "field_mappings")
  private Map<String, List<FieldMapping>> fieldMappings;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public String getConfigId() { return configId; }
  public void setConfigId(String configId) { this.configId = configId; }
  public String getInsurerId() { return insurerId; }
  public void setInsurerId(String insurerId) { this.insurerId = insurerId; }
  public String getInsurerName() { return insurerName; }
  public void setInsurerName(String insurerName) { this.insurerName = insurerName; }
  public Map<String, List<FieldMapping>> getFieldMappings() { return fieldMappings; }
  public void setFieldMappings(Map<String, List<FieldMapping>> fieldMappings) { this.fieldMappings = fieldMappings; }
  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
