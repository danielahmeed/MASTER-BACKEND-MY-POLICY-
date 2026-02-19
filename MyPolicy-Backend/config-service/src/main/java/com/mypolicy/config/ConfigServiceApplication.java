package com.mypolicy.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Centralized Configuration Service for MyPolicy Backend.
 * 
 * Manages configuration files for all microservices from a single location.
 * Other services connect to this server to fetch their configurations.
 * 
 * Port: 8888 (Spring Cloud Config default)
 * 
 * Features:
 * - Centralized configuration management
 * - Environment-specific configs (dev, staging, prod)
 * - Dynamic configuration updates (without restart)
 * - Version control integration (Git/File system)
 * - Encrypted sensitive properties
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(ConfigServiceApplication.class, args);
    System.out.println("\n" +
        "╔═══════════════════════════════════════════════════════╗\n" +
        "║   Configuration Service Started Successfully! ✓      ║\n" +
        "╠═══════════════════════════════════════════════════════╣\n" +
        "║   Port: 8888                                         ║\n" +
        "║   Mode: File System (Local Development)             ║\n" +
        "║   Config Location: config-repo/                     ║\n" +
        "║                                                       ║\n" +
        "║   Services Managed:                                  ║\n" +
        "║     ✓ BFF Service           (port 8080)             ║\n" +
        "║     ✓ Customer Service      (port 8081)             ║\n" +
        "║     ✓ Data-Pipeline Service (port 8082)             ║\n" +
        "║     ✓ Policy Service        (port 8085)             ║\n" +
        "║                                                       ║\n" +
        "║   Test: http://localhost:8888/actuator/health       ║\n" +
        "╚═══════════════════════════════════════════════════════╝\n");
  }
}
