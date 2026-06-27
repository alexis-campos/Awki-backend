package com.awki.epicrisis.service;

import com.awki.epicrisis.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalStorageServiceImpl implements DocumentStorageService {

    private final StorageProperties storageProperties;

    @Override
    public String guardarDocumento(byte[] contenido, String nombreArchivo) {
        try {
            Path targetDir = Paths.get(storageProperties.getLocalPath());
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            Path targetFile = targetDir.resolve(nombreArchivo);
            Files.write(targetFile, contenido);
            log.info("Archivo PDF guardado localmente en: {}", targetFile.toAbsolutePath());
            return nombreArchivo;
        } catch (IOException e) {
            log.error("Error guardando archivo en almacenamiento local: {}", e.getMessage());
            throw new RuntimeException("Error guardando el PDF de la epicrisis", e);
        }
    }

    @Override
    public byte[] obtenerDocumento(String nombreArchivo) {
        try {
            Path targetFile = Paths.get(storageProperties.getLocalPath()).resolve(nombreArchivo);
            if (!Files.exists(targetFile)) {
                throw new RuntimeException("El archivo de epicrisis solicitado no existe: " + nombreArchivo);
            }
            return Files.readAllBytes(targetFile);
        } catch (IOException e) {
            log.error("Error leyendo archivo del almacenamiento local: {}", e.getMessage());
            throw new RuntimeException("Error leyendo el PDF de la epicrisis", e);
        }
    }
}
