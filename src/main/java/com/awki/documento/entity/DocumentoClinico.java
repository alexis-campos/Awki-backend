package com.awki.documento.entity;

import com.awki.common.BaseEntity;
import com.awki.embarazo.entity.Embarazo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "documentos")
public class DocumentoClinico extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "embarazo_id", nullable = false)
    private Embarazo embarazo;

    @Enumerated(EnumType.STRING)
    @Column(name = "subido_por", nullable = false)
    private SubidoPor subidoPor;

    @Column(name = "subido_por_id", nullable = false)
    private UUID subidoPorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false)
    private TipoDocumento tipoDocumento;

    @Column(name = "nombre_archivo", nullable = false, length = 200)
    private String nombreArchivo;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(nullable = false)
    private boolean eliminado = false;
}