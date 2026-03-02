package com.mypolicy.pipeline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Data Pipeline Service - Consolidated Service
 * 
 * Combines 4 microservices into one:
 * - Ingestion Module (File upload and job tracking)
 * - Metadata Module (Insurer configurations)
 * - Processing Module (File parsing and transformation)
 * - Matching Module (Customer identity resolution)
 * 
 * Uses H2 (JPA) only - no MongoDB required.
 * 
 * @author MyPolicy Team
 * @version 1.0.0
 */
@SpringBootApplication(exclude = {
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class
})
@EnableFeignClients(basePackages = "com.mypolicy.pipeline.matching.client")
@EnableJpaRepositories(basePackages = {
    "com.mypolicy.pipeline.metadata.repository",
    "com.mypolicy.pipeline.ingestion.repository",
    "com.mypolicy.pipeline.portfolio.repository"
})
public class DataPipelineApplication {

  public static void main(String[] args) {
    SpringApplication.run(DataPipelineApplication.class, args);
    System.out.println("\n" +
        "========================================\n" +
        "  Data Pipeline Service Started!\n" +
        "========================================\n" +
        "  Port: 8082\n" +
        "  Modules:\n" +
        "    ✓ Ingestion  - File upload & tracking\n" +
        "    ✓ Metadata   - Insurer configurations\n" +
        "    ✓ Processing - File parsing & transform\n" +
        "    ✓ Matching   - Customer resolution\n" +
        "========================================\n");
  }

}
