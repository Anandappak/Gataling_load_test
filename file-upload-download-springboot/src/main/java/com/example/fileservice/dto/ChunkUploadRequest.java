package com.example.fileservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;

public record ChunkUploadRequest(
        @Min(0) int chunkNumber,
        @NotBlank String base64Data
) {}
