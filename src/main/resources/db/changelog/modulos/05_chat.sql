-- liquibase formatted sql

-- changeset awki:1_create_chat_tables
CREATE TABLE mensajes_chat (
    id UUID PRIMARY KEY,
    embarazo_id UUID NOT NULL,
    rol VARCHAR(20) NOT NULL,
    contenido TEXT NOT NULL,
    alarma_probable BOOLEAN DEFAULT false,
    desde_cache BOOLEAN DEFAULT false,
    fallback_usado BOOLEAN DEFAULT false,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_mensajes_chat_embarazo FOREIGN KEY (embarazo_id) REFERENCES embarazos(id) ON DELETE CASCADE
);

CREATE TABLE resumenes_clinicos (
    embarazo_id UUID PRIMARY KEY,
    contenido_resumen TEXT NOT NULL,
    generado_por_modelo VARCHAR(100),
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_resumenes_clinicos_embarazo FOREIGN KEY (embarazo_id) REFERENCES embarazos(id) ON DELETE CASCADE
);

CREATE INDEX idx_mensajes_chat_embarazo_created ON mensajes_chat(embarazo_id, created_at DESC);
