# Chunk-Based File Upload/Download — Spring Boot + Maven + H2

Senior-level reference implementation for uploading a file in Base64-encoded chunks, storing metadata/chunks in H2, validating SHA-256, and downloading the reconstructed file.

## Architecture

Client
  |
  | 1. POST /api/v1/files/init
  v
File Metadata (H2)
  |
  | 2. POST /api/v1/files/{fileId}/chunks
  |    Base64 chunk
  v
File Chunk table (H2)
  |
  | 3. GET /api/v1/files/{fileId}
  v
Reconstruct chunks -> SHA-256 validation -> download

## Project structure

src/main/java/com/example/fileservice/
├── FileServiceApplication.java
├── controller/FileController.java
├── dto/
│   ├── InitUploadRequest.java
│   ├── InitUploadResponse.java
│   ├── ChunkUploadRequest.java
│   └── UploadResponse.java
├── entity/
│   ├── FileMetadata.java
│   ├── FileChunk.java
│   └── FileStatus.java
├── repository/
│   ├── FileMetadataRepository.java
│   └── FileChunkRepository.java
└── service/FileStorageService.java

## Run

Requirements:
- Java 17+
- Maven 3.9+

```bash
mvn clean test
mvn spring-boot:run
```

Or:

```bash
mvn clean package
java -jar target/file-service-1.0.0.jar
```

## API

### 1. Initialize upload

POST `/api/v1/files/init`

```json
{
  "fileName": "sample.pdf",
  "contentType": "application/pdf",
  "fileSize": 5242880,
  "checksum": "SHA256_HEX",
  "totalChunks": 5
}
```

Response:

```json
{
  "fileId": "uuid",
  "totalChunks": 5,
  "chunkSizeBytes": 1048576
}
```

### 2. Upload chunks

POST `/api/v1/files/{fileId}/chunks`

```json
{
  "chunkNumber": 0,
  "base64Data": "JVBERi0x..."
}
```

Chunk numbers are zero-based.

The backend:
- Base64 decodes the chunk
- validates maximum chunk size
- stores it as BLOB
- supports retrying the same chunk
- marks the file COMPLETED after all chunks exist

### 3. Download

GET `/api/v1/files/{fileId}`

The service:
- reads chunks ordered by chunkNumber
- reconstructs the file
- calculates SHA-256
- compares it with upload metadata
- returns the original file as an attachment

### 4. Metadata

GET `/api/v1/files/{fileId}/metadata`

## Generating SHA-256

Linux/macOS:

```bash
sha256sum sample.pdf
```

Windows PowerShell:

```powershell
(Get-FileHash .\sample.pdf -Algorithm SHA256).Hash
```

Use that hexadecimal SHA-256 value in the `checksum` field.

## Important design notes

1. Base64 increases payload size by roughly 33%. For production, `multipart/form-data` or a binary chunk endpoint is normally preferable.
2. This demo uses H2 BLOB storage because the requirement specifies H2. For production-scale files, object storage such as S3 is generally a better fit, with H2/PostgreSQL storing metadata.
3. The download implementation reconstructs the whole file in memory. For very large files, use streaming (`StreamingResponseBody`) or object-storage streaming.
4. The sample uses a file-backed H2 database (`./data/filedb`) so data survives application restarts.
5. Authentication/authorization, antivirus scanning, MIME validation, quotas, rate limiting, cleanup of abandoned uploads, and audit logging should be added for production.
6. The chunk size is configurable in `application.yml`; default is 1 MB.

## Example curl flow

Initialize:

```bash
curl -X POST http://localhost:8080/api/v1/files/init ^
  -H "Content-Type: application/json" ^
  -d "{"fileName":"sample.pdf","contentType":"application/pdf","fileSize":123456,"checksum":"YOUR_SHA256","totalChunks":1}"
```

Upload one Base64 chunk:

```bash
curl -X POST http://localhost:8080/api/v1/files/YOUR_FILE_ID/chunks ^
  -H "Content-Type: application/json" ^
  -d "{"chunkNumber":0,"base64Data":"YOUR_BASE64_CHUNK"}"
```

Download:

```bash
curl -OJ http://localhost:8080/api/v1/files/YOUR_FILE_ID
```

## Senior developer discussion points

- Idempotency: re-uploading the same chunk number replaces that chunk.
- Integrity: SHA-256 is checked after reconstruction.
- Persistence: metadata and chunks are separate tables.
- Ordering: chunks are always retrieved by chunkNumber.
- Transaction boundaries: each chunk upload is atomic.
- Concurrency: a unique `(file_id, chunk_number)` constraint prevents duplicate chunk rows.
- Scalability: for large files, move binary content to S3/object storage and keep only metadata in the relational database.
- Security: never trust the client-provided content type or filename without server-side validation.

## H2 console

Open:

`http://localhost:8080/h2-console`

JDBC URL:

`jdbc:h2:file:./data/filedb`

User: `sa`

Password: empty
