package com.example.fileservice.service;

import com.example.fileservice.dto.*;
import com.example.fileservice.entity.*;
import com.example.fileservice.repository.FileChunkRepository;
import com.example.fileservice.repository.FileMetadataRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

@Service
public class FileStorageService {

    private final FileMetadataRepository metadataRepository;
    private final FileChunkRepository chunkRepository;
    private final int chunkSizeBytes;

    public FileStorageService(
            FileMetadataRepository metadataRepository,
            FileChunkRepository chunkRepository,
            @Value("${app.upload.chunk-size-bytes:1048576}")
            int chunkSizeBytes) {

        this.metadataRepository = metadataRepository;
        this.chunkRepository = chunkRepository;
        this.chunkSizeBytes = chunkSizeBytes;
    }

    @Transactional
    public InitUploadResponse init(InitUploadRequest request) {

        if (request.totalChunks() > 100_000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Too many chunks"
            );
        }

        UUID id = UUID.randomUUID();

        FileMetadata file = new FileMetadata();

        file.setId(id);
        file.setFileName(request.fileName());
        file.setContentType(request.contentType());
        file.setFileSize(request.fileSize());
        file.setTotalChunks(request.totalChunks());
        file.setChecksum(normalizeChecksum(request.checksum()));
        file.setStatus(FileStatus.UPLOADING);
        file.setCreatedAt(Instant.now());

        metadataRepository.save(file);

        return new InitUploadResponse(
                id,
                request.totalChunks(),
                chunkSizeBytes
        );
    }

    @Transactional
    public UploadResponse uploadChunk(
            UUID fileId,
            ChunkUploadRequest request) {

        FileMetadata file = getFile(fileId);

        if (file.getStatus() != FileStatus.UPLOADING) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Upload is not active"
            );
        }

        if (request.chunkNumber() < 0
                || request.chunkNumber() >= file.getTotalChunks()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid chunk number"
            );
        }

        final byte[] decoded;

        try {

            decoded = Base64.getDecoder()
                    .decode(request.base64Data());

        } catch (IllegalArgumentException ex) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid Base64 chunk",
                    ex
            );
        }

        if (decoded.length == 0
                || decoded.length > chunkSizeBytes) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chunk size is invalid"
            );
        }

        Optional<FileChunk> existing =
                chunkRepository.findByFileIdAndChunkNumber(
                        fileId,
                        request.chunkNumber()
                );

        FileChunk chunk =
                existing.orElseGet(FileChunk::new);

        chunk.setFile(file);
        chunk.setChunkNumber(request.chunkNumber());
        chunk.setData(decoded);

        chunkRepository.save(chunk);

        long uploaded =
                chunkRepository
                        .findAllByFileIdOrderByChunkNumberAsc(fileId)
                        .size();

        boolean completed =
                uploaded == file.getTotalChunks();

        if (completed) {

            file.setStatus(FileStatus.COMPLETED);

            metadataRepository.save(file);
        }

        return new UploadResponse(
                completed
                        ? "Upload completed"
                        : "Chunk uploaded",

                (int) uploaded,

                file.getTotalChunks(),

                completed
        );
    }

    @Transactional(readOnly = true)
    public DownloadFile download(UUID fileId) {

        FileMetadata file = getFile(fileId);

        if (file.getStatus() != FileStatus.COMPLETED) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "File upload is not completed"
            );
        }

        List<FileChunk> chunks =
                chunkRepository
                        .findAllByFileIdOrderByChunkNumberAsc(fileId);

        if (chunks.size() != file.getTotalChunks()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "File chunks are incomplete"
            );
        }

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            for (int i = 0; i < chunks.size(); i++) {

                FileChunk chunk = chunks.get(i);

                if (chunk.getChunkNumber() != i) {

                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Missing chunk " + i
                    );
                }

                output.write(chunk.getData());

                digest.update(chunk.getData());
            }

            byte[] content = output.toByteArray();

            String actualChecksum =
                    HexFormat.of()
                            .formatHex(digest.digest());

            if (!actualChecksum.equalsIgnoreCase(
                    normalizeChecksum(file.getChecksum()))) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Checksum validation failed"
                );
            }

            return new DownloadFile(
                    file.getFileName(),
                    file.getContentType(),
                    content
            );

        } catch (IOException
                 | NoSuchAlgorithmException ex) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to build file",
                    ex
            );
        }
    }

    @Transactional(readOnly = true)
    public FileMetadata getFile(UUID fileId) {

        return metadataRepository
                .findById(fileId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "File not found"
                        )
                );
    }

    private String normalizeChecksum(String checksum) {

        return checksum
                .trim()
                .replace("\"", "");
    }

    public record DownloadFile(
            String fileName,
            String contentType,
            byte[] content
    ) {
    }
}