package com.example.fileservice.dto;

public record UploadResponse(String message, int uploadedChunks, int totalChunks, boolean completed) {}
