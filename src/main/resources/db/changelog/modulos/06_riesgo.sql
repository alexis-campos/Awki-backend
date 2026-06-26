--liquibase formatted sql
--changeset awki:06_riesgo

CREATE TABLE evaluaciones_riesgo (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    embarazo_id     UUID        NOT NULL REFERENCES embarazos(id),
    nivel           VARCHAR(20) NOT NULL CHECK (nivel IN ('VERDE', 'AMARILLO', 'ROJO')),
    criterios_activos JSONB,
    descripcion_alerta TEXT,
    generar_alerta  BOOLEAN     NOT NULL DEFAULT FALSE,
    fuente_evaluacion VARCHAR(20) NOT NULL CHECK (fuente_evaluacion IN ('CONTROL', 'SINTOMAS', 'SOS')),
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_evaluaciones_riesgo_embarazo_id ON evaluaciones_riesgo(embarazo_id);
CREATE INDEX idx_evaluaciones_riesgo_nivel        ON evaluaciones_riesgo(nivel);
