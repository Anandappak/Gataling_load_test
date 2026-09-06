package com.example.fileservice.repository;

import com.example.fileservice.entity.FileChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileChunkRepository extends JpaRepository<FileChunk, Long> {
    Optional<FileChunk> findByFileIdAndChunkNumber(UUID fileId, int chunkNumber);
    List<FileChunk> findAllByFileIdOrderByChunkNumberAsc(UUID fileId);
}
