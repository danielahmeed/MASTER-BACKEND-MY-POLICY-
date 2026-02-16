package com.mypolicy.bff.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "ingestion-service", url = "${ingestion.service.url}")
public interface IngestionClient {

  @PostMapping(value = "/api/v1/ingestion/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  Object uploadFile(@RequestPart("file") MultipartFile file,
      @RequestParam("insurerId") String insurerId,
      @RequestParam("uploadedBy") String uploadedBy);

  @GetMapping("/api/v1/ingestion/status/{jobId}")
  Object getJobStatus(@PathVariable("jobId") String jobId);
}
