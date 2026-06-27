package com.awki.documento.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentoStorageServiceTest {

    private final DocumentoStorageService storageService = new DocumentoStorageService();

    @Test
    void subir_debeGenerarStorageKeyConEmbarazoIdYNombreArchivo() {
        UUID embarazoId = UUID.randomUUID();

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "ecografia.pdf",
                "application/pdf",
                "contenido".getBytes()
        );

        String storageKey = storageService.subir(archivo, embarazoId);

        assertThat(storageKey).contains("documentos/" + embarazoId);
        assertThat(storageKey).contains("ecografia.pdf");
    }

    @Test
    void generarUrlFirmada_debeRetornarUrlTemporalDemo() {
        String url = storageService.generarUrlFirmada("documentos/demo.pdf");

        assertThat(url).contains("https://storage.awki.local/");
        assertThat(url).contains("signature=demo");
        assertThat(url).contains("expires=900");
    }

    @Test
    void calcularExpiracionUrl_debeRetornarFechaFutura() {
        assertThat(storageService.calcularExpiracionUrl()).isAfter(java.time.LocalDateTime.now());
    }
}
