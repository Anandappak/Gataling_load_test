package com.example.fileservice.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "file_chunk",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_file_chunk_number",
                columnNames = {"file_id", "chunkNumber"}),
        indexes = @Index(name = "idx_chunk_file", columnList = "file_id, chunkNumber"))
public class FileChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private FileMetadata file;

    @Column(nullable = false)
    private int chunkNumber;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private byte[] data;

    public Long getId() { return id; }
    public FileMetadata getFile() { return file; }
    public void setFile(FileMetadata file) { this.file = file; }
    public int getChunkNumber() { return chunkNumber; }
    public void setChunkNumber(int chunkNumber) { this.chunkNumber = chunkNumber; }
    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
}
