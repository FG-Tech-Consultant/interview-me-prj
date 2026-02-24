package com.interviewme.exports.service;

import com.interviewme.exports.config.ExportProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    private final ExportProperties exportProperties;

    @Override
    public String store(Long tenantId, String fileName, byte[] content) {
        try {
            Path directory = Path.of(exportProperties.getStoragePath(), tenantId.toString());
            Files.createDirectories(directory);
            Path filePath = directory.resolve(fileName);
            Files.write(filePath, content);
            log.info("File stored: path={}, size={} bytes", filePath, content.length);
            return filePath.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store file: " + fileName, e);
        }
    }

    @Override
    public byte[] retrieve(String fileUrl) {
        try {
            Path filePath = Path.of(fileUrl);
            if (!Files.exists(filePath)) {
                throw new RuntimeException("File not found: " + fileUrl);
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to retrieve file: " + fileUrl, e);
        }
    }

    @Override
    public void delete(String fileUrl) {
        try {
            Files.deleteIfExists(Path.of(fileUrl));
            log.info("File deleted: {}", fileUrl);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", fileUrl, e);
        }
    }
}
