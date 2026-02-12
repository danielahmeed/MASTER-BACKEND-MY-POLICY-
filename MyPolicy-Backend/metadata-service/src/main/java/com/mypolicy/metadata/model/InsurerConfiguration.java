package com.mypolicy.metadata.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
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
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
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

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb", name = "field_mappings")
  private Map<String, List<FieldMapping>> fieldMappings;

  @Column(name = "active", nullable = false)
  @Builder.Default
  private boolean active = true;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
