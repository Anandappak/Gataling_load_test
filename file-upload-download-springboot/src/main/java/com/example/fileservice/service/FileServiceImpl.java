package com.example.fileservice.service;

import com.example.fileservice.entity.FileUploadResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    private final Path uploadDirectory =
            Paths.get("uploads").toAbsolutePath().normalize();

    @Override
    public FileUploadResponse upload(MultipartFile file) {

        validateFile(file);

        try {
            Files.createDirectories(uploadDirectory);

            String originalFileName =
                    StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

            String extension = "";

            int lastDot = originalFileName.lastIndexOf('.');

            if (lastDot > 0) {
                extension = originalFileName.substring(lastDot);
            }

            String storedFileName =
                    UUID.randomUUID() + extension;

            Path targetPath =
                    uploadDirectory.resolve(storedFileName)
                            .normalize();

            if (!targetPath.startsWith(uploadDirectory)) {
                throw new IllegalArgumentException(
                        "Invalid file path"
                );
            }

            Files.copy(
                    file.getInputStream(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return new FileUploadResponse(
                    storedFileName,
                    originalFileName,
                    file.getContentType(),
                    file.getSize(),
                    targetPath.toString()
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to upload file",
                    e
            );
        }
    }

    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "File must not be empty"
            );
        }

        if (file.getOriginalFilename() == null) {
            throw new IllegalArgumentException(
                    "File name is required"
            );
        }
    }
}
