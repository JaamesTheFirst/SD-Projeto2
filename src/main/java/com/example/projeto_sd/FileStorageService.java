package com.example.projeto_sd;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public String storeFile(MultipartFile file) throws IOException {
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        String extension = "";
        int idx = originalFilename.lastIndexOf('.');
        if (idx > 0) {
            extension = originalFilename.substring(idx);
        }

        String fileName = UUID.randomUUID().toString() + extension;

        Path targetLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.exists(targetLocation)) {
            Files.createDirectories(targetLocation);
        }

        Path destination = targetLocation.resolve(fileName);
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        return "uploads/" + fileName;
    }
}