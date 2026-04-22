package com.love.yourvoiceback.inference;

import com.love.yourvoiceback.common.exception.ApiException;
import com.love.yourvoiceback.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class GeneratedAudioStorage {

    private final StorageMode storageMode;
    private final Path generatedAudioStorageRoot;
    private final Path voiceSourceStorageRoot;
    private final String bucket;
    private final String generatedPrefix;
    private final String sourcePrefix;
    private final S3Client s3Client;

    public GeneratedAudioStorage(
            @Value("${app.audio-storage.mode:local}") String storageMode,
            @Value("${app.generated-audio.storage-path:storage/generated-audios}") String generatedAudioStoragePath,
            @Value("${app.voice-source.storage-path:storage/voice-sources}") String voiceSourceStoragePath,
            @Value("${app.audio-storage.s3.bucket:}") String bucket,
            @Value("${app.audio-storage.s3.region:ap-northeast-2}") String region,
            @Value("${app.audio-storage.s3.generated-prefix:voices/generated}") String generatedPrefix,
            @Value("${app.audio-storage.s3.source-prefix:voices/source}") String sourcePrefix
    ) {
        this.storageMode = StorageMode.from(storageMode);
        this.generatedAudioStorageRoot = Paths.get(generatedAudioStoragePath).toAbsolutePath().normalize();
        this.voiceSourceStorageRoot = Paths.get(voiceSourceStoragePath).toAbsolutePath().normalize();
        this.bucket = bucket;
        this.generatedPrefix = normalizePrefix(generatedPrefix);
        this.sourcePrefix = normalizePrefix(sourcePrefix);

        if (this.storageMode == StorageMode.S3) {
            if (!StringUtils.hasText(bucket)) {
                throw ApiException.error(ErrorCode.INTERNAL_SERVER_ERROR, "S3 bucket is not configured");
            }
            this.s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .build();
        } else {
            this.s3Client = null;
        }
    }

    public String saveGeneratedAudio(String requestHash, byte[] body, MediaType contentType) {
        String extension = resolveExtension(contentType);
        String filename = requestHash + "." + extension;

        if (storageMode == StorageMode.S3) {
            String objectKey = generatedPrefix + "/" + filename;
            putS3Object(objectKey, body, contentType);
            return objectKey;
        }

        try {
            Files.createDirectories(generatedAudioStorageRoot);
            Path target = generatedAudioStorageRoot.resolve(filename).normalize();
            Files.write(target, body);
            return filename;
        } catch (IOException ex) {
            throw ApiException.error(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to store generated audio");
        }
    }

    public StoredAudioObject saveVoiceSourceAudio(Long userId, MultipartFile file) {
        byte[] body = readMultipartFile(file);
        String safeFilename = sanitizeFilename(file.getOriginalFilename());
        String objectName = UUID.randomUUID() + "-" + safeFilename;
        String contentType = StringUtils.hasText(file.getContentType()) ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String checksum = sha256Hex(body);

        if (storageMode == StorageMode.S3) {
            String objectKey = sourcePrefix + "/" + userId + "/" + objectName;
            putS3Object(objectKey, body, MediaType.parseMediaType(contentType));
            return new StoredAudioObject(objectKey, contentType, file.getSize(), checksum);
        }

        try {
            Path userDirectory = voiceSourceStorageRoot.resolve(String.valueOf(userId)).normalize();
            Files.createDirectories(userDirectory);
            Path target = userDirectory.resolve(objectName).normalize();
            Files.write(target, body);
            String location = String.valueOf(userId) + "/" + objectName;
            return new StoredAudioObject(location, contentType, file.getSize(), checksum);
        } catch (IOException ex) {
            throw ApiException.error(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to store voice source audio");
        }
    }

    public byte[] read(String storedFilename) {
        if (storageMode == StorageMode.S3) {
            try {
                return s3Client.getObjectAsBytes(
                        GetObjectRequest.builder()
                                .bucket(bucket)
                                .key(storedFilename)
                                .build()
                ).asByteArray();
            } catch (NoSuchKeyException ex) {
                throw ApiException.error(ErrorCode.GENERATED_AUDIO_NOT_FOUND);
            } catch (S3Exception ex) {
                throw ApiException.error(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to read stored audio");
            }
        }

        try {
            return Files.readAllBytes(generatedAudioStorageRoot.resolve(storedFilename).normalize());
        } catch (IOException ex) {
            throw ApiException.error(ErrorCode.GENERATED_AUDIO_NOT_FOUND);
        }
    }

    public MediaType resolveContentType(String storedFilename) {
        return storedFilename.toLowerCase().endsWith(".mp3")
                ? MediaType.valueOf("audio/mpeg")
                : MediaType.valueOf("audio/wav");
    }

    private void putS3Object(String objectKey, byte[] body, MediaType contentType) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(objectKey)
                            .contentType(contentType.toString())
                            .build(),
                    RequestBody.fromBytes(body)
            );
        } catch (S3Exception ex) {
            throw ApiException.error(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to store audio in S3");
        }
    }

    private byte[] readMultipartFile(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw ApiException.error(ErrorCode.INVALID_REQUEST, "Failed to read uploaded audio file");
        }
    }

    private String resolveExtension(MediaType contentType) {
        return MediaType.valueOf("audio/mpeg").includes(contentType) ? "mp3" : "wav";
    }

    private String sanitizeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return UUID.randomUUID() + ".bin";
        }

        String sanitized = filename.replace("\\", "/");
        int slashIndex = sanitized.lastIndexOf('/');
        if (slashIndex >= 0) {
            sanitized = sanitized.substring(slashIndex + 1);
        }

        sanitized = sanitized.replaceAll("[^A-Za-z0-9._-]", "_");
        if (!StringUtils.hasText(sanitized)) {
            return UUID.randomUUID() + ".bin";
        }
        return sanitized;
    }

    private String normalizePrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return "audio";
        }
        return prefix.replace("\\", "/").replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private String sha256Hex(byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(body));
        } catch (NoSuchAlgorithmException ex) {
            throw ApiException.error(ErrorCode.INTERNAL_SERVER_ERROR, "SHA-256 is not available");
        }
    }

    public record StoredAudioObject(
            String storagePath,
            String contentType,
            long fileSize,
            String checksum
    ) {
    }

    private enum StorageMode {
        LOCAL,
        S3;

        private static StorageMode from(String value) {
            if (!StringUtils.hasText(value)) {
                return LOCAL;
            }
            return "s3".equalsIgnoreCase(value.trim()) ? S3 : LOCAL;
        }
    }
}
