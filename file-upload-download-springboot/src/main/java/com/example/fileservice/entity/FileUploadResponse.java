package com.example.fileservice.entity;

public record FileUploadResponse(
        String fileName,
        String originalFileName,
        String contentType,
        long size,
        String location
) {
}
