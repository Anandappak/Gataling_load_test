package com.example.fileservice.dto;

import java.util.UUID;

public record InitUploadResponse(UUID fileId, int totalChunks, int chunkSizeBytes) {}
