package com.mypolicy.ingestion.service;

import com.mypolicy.ingestion.dto.JobStatusResponse;
import com.mypolicy.ingestion.model.IngestionJob;
import com.mypolicy.ingestion.model.IngestionStatus;
import com.mypolicy.ingestion.repository.IngestionJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IngestionService {

  private final IngestionJobRepository jobRepository;

  // In a real app, use S3. Here using local volume.
  private final String UPLOAD_DIR = "uploads/";

  public String submitJob(MultipartFile file, String insurerId, String policyType) throws IOException { // Added missing
                                                                                                        // closing brace
                                                                                                        // in previous
                                                                                                        // tool call or
                                                                                                        // logic issue
                                                                                                        // fix if any?
    // 1. Validate File
    if (file.isEmpty()) {
      throw new RuntimeException("File is empty");
    }

    // 2. Save File locally
    String jobId = UUID.randomUUID().toString();
    String originalName = file.getOriginalFilename();
    String extension = "";
    if (originalName != null && originalName.lastIndexOf(".") > 0) {
      extension = originalName.substring(originalName.lastIndexOf("."));
    }

    String storedFileName = jobId + extension;

    Path uploadPath = Paths.get(UPLOAD_DIR);
    if (!Files.exists(uploadPath)) {
      Files.createDirectories(uploadPath);
    }

    Path filePath = uploadPath.resolve(storedFileName);
    // Using try-with-resources for input stream
    try (InputStream inputStream = file.getInputStream()) {
      Files.copy(inputStream, filePath);
    }

    // 3. Create Job Entry in MongoDB
    IngestionJob job = new IngestionJob();
    job.setJobId(jobId);
    job.setFileName(originalName);
    job.setFileKey(filePath.toString());
    job.setInsurerId(insurerId);
    job.setPolicyType(policyType);
    job.setFileSize(file.getSize());
    job.setContentType(file.getContentType());
    job.setStatus(IngestionStatus.UPLOADED); // Initial Status
    job.setUploadedAt(LocalDateTime.now());

    jobRepository.save(job);

    return jobId;
  }

  public JobStatusResponse getJobStatus(String jobId) {
    return jobRepository.findById(jobId)
        .map(job -> JobStatusResponse.builder()
            .jobId(job.getJobId())
            .fileName(job.getFileName())
            .status(job.getStatus())
            .processed(job.getProcessedRecords())
            .total(job.getTotalRecords())
            .uploadedAt(job.getUploadedAt())
            .build())
        .orElseThrow(() -> new RuntimeException("Job not found"));
  }
}
