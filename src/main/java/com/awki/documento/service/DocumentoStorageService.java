package com.awki.documento.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DocumentoStorageService {

    private final Path rootLocation = Paths.get("uploads");

    public String subir(MultipartFile archivo, UUID embarazoId) {
        String nombreOriginal = archivo.getOriginalFilename() == null
                ? "documento"
                : archivo.getOriginalFilename();

        String uniqueName = UUID.randomUUID().toString() + "-" + nombreOriginal;
        
        try {
            Path targetDir = this.rootLocation.resolve("documentos").resolve(embarazoId.toString());
            Files.createDirectories(targetDir);
            Files.copy(archivo.getInputStream(), targetDir.resolve(uniqueName));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el archivo localmente", e);
        }

        return "documentos/%s/%s".formatted(
                embarazoId,
                uniqueName
        );
    }

    public String generarUrlFirmada(String storageKey) {
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        return "%s/api/v1/documentos/file/%s".formatted(baseUrl, storageKey);
    }

    public LocalDateTime calcularExpiracionUrl() {
        return LocalDateTime.now().plusMinutes(15);
    }
}