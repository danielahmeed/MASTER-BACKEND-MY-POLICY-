package com.mypolicy.matching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MatchingEngineApplication {

  public static void main(String[] args) {
    SpringApplication.run(MatchingEngineApplication.class, args);
  }

}
