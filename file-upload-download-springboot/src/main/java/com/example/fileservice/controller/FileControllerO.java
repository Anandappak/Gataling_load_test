package com.example.fileservice.controller;

import com.example.fileservice.dto.*;
import com.example.fileservice.entity.FileMetadata;
import com.example.fileservice.service.FileStorageService;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files3")
public class FileControllerO {

    private final FileStorageService service;

    public FileControllerO(FileStorageService service) {
        this.service = service;
    }

    @PostMapping("/init")
    public InitUploadResponse init(@Valid @RequestBody InitUploadRequest request) {
        return service.init(request);
    }

    @PostMapping("/{fileId}/chunks")
    public UploadResponse uploadChunk(
            @PathVariable UUID fileId,
            @Valid @RequestBody ChunkUploadRequest request) {
        return service.uploadChunk(fileId, request);
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<ByteArrayResource> download(@PathVariable UUID fileId) {
        FileStorageService.DownloadFile file = service.download(fileId);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName())
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new ByteArrayResource(file.content()));
    }

    @GetMapping("/{fileId}/metadata")
    public FileMetadata metadata(@PathVariable UUID fileId) {
        return service.getFile(fileId);
    }
}
