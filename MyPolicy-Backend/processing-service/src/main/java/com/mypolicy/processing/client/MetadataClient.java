package com.mypolicy.processing.client;

import com.mypolicy.processing.dto.InsurerConfigurationDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "metadata-service", url = "http://localhost:8083")
public interface MetadataClient {

  @GetMapping("/api/v1/metadata/config/{insurerId}")
  InsurerConfigurationDTO getConfiguration(@PathVariable("insurerId") String insurerId);
}
