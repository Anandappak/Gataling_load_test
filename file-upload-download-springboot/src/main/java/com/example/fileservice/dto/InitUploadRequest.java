package com.example.fileservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InitUploadRequest(
        @NotBlank String fileName,
        @NotBlank String contentType,
        @Min(1) long fileSize,
        @NotNull String checksum,
        @Min(1) int totalChunks
) {}
