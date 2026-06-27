-- liquibase formatted sql

-- changeset awki:14-1
CREATE TABLE reportes_sintomas (
    id UUID PRIMARY KEY,
    embarazo_id UUID NOT NULL,
    bienestar VARCHAR(50) NOT NULL,
    movimientos VARCHAR(100) NOT NULL,
    hinchazon VARCHAR(100) NOT NULL,
    sintomas_detalle TEXT,
    notas TEXT,
    es_critico BOOLEAN NOT NULL DEFAULT FALSE,
    sintomas_clinicos JSONB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_reportes_embarazo FOREIGN KEY (embarazo_id) REFERENCES embarazos(id)
);

CREATE INDEX idx_reportes_sintomas_embarazo ON reportes_sintomas(embarazo_id);
