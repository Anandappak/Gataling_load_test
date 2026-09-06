package com.example.fileservice.service;
import com.example.fileservice.entity.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    FileUploadResponse upload(MultipartFile file);
}

