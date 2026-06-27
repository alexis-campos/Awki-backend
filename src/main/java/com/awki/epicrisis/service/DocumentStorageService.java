package com.awki.epicrisis.service;

public interface DocumentStorageService {
    String guardarDocumento(byte[] contenido, String nombreArchivo);
    byte[] obtenerDocumento(String urlRelativePath);
}
