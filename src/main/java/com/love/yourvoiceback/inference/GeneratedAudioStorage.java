package com.love.yourvoiceback.inference;

import com.love.yourvoiceback.common.exception.ApiException;
import com.love.yourvoiceback.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class GeneratedAudioStorage {

    private final Path storageRoot;

    public GeneratedAudioStorage(
            @Value("${app.generated-audio.storage-path:storage/generated-audios}") String storagePath
    ) {
        this.storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();
    }

    public String save(Long generatedAudioId, byte[] body, MediaType contentType) {
        try {
            Files.createDirectories(storageRoot);
            String extension = MediaType.valueOf("audio/mpeg").includes(contentType) ? "mp3" : "wav";
            String filename = generatedAudioId + "." + extension;
            Path target = storageRoot.resolve(filename);
            Files.write(target, body);
            return filename;
        } catch (IOException ex) {
            throw ApiException.error(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to store generated audio");
        }
    }

    public byte[] read(String storedFilename) {
        try {
            return Files.readAllBytes(storageRoot.resolve(storedFilename).normalize());
        } catch (IOException ex) {
            throw ApiException.error(ErrorCode.GENERATED_AUDIO_NOT_FOUND);
        }
    }

    public MediaType resolveContentType(String storedFilename) {
        return storedFilename.toLowerCase().endsWith(".mp3")
                ? MediaType.valueOf("audio/mpeg")
                : MediaType.valueOf("audio/wav");
    }
}
