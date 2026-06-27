package com.awki.documento.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DocumentoStorageService {

    public String subir(MultipartFile archivo, UUID embarazoId) {
        String nombreOriginal = archivo.getOriginalFilename() == null
                ? "documento"
                : archivo.getOriginalFilename();

        return "documentos/%s/%s-%s".formatted(
                embarazoId,
                UUID.randomUUID(),
                nombreOriginal
        );
    }

    public String generarUrlFirmada(String storageKey) {
        return "https://storage.awki.local/%s?signature=demo&expires=900".formatted(storageKey);
    }

    public LocalDateTime calcularExpiracionUrl() {
        return LocalDateTime.now().plusMinutes(15);
    }
}