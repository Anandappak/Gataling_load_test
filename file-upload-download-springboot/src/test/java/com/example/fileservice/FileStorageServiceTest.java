package com.example.fileservice;

import com.example.fileservice.dto.ChunkUploadRequest;
import com.example.fileservice.dto.InitUploadRequest;
import com.example.fileservice.dto.InitUploadResponse;
import com.example.fileservice.dto.UploadResponse;
import com.example.fileservice.repository.FileChunkRepository;
import com.example.fileservice.repository.FileMetadataRepository;
import com.example.fileservice.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @Test
    void checksumCanBeGeneratedForTestPayload() throws Exception {
        byte[] payload = "hello world".getBytes();
        String checksum = java.util.HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(payload));

        assertEquals(
                "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
                checksum
        );
    }
}
