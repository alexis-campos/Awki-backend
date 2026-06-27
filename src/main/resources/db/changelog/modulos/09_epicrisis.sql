-- liquibase formatted sql
-- changeset alonzo:create-epicrisis-and-jobs-tables

CREATE TABLE epicrisis (
    id UUID PRIMARY KEY,
    embarazo_id UUID NOT NULL,
    medico_id UUID NOT NULL,
    clinica_id UUID NOT NULL,
    motivo_derivacion VARCHAR(1000) NOT NULL,
    observaciones_adicionales VARCHAR(2000),
    contenido_json TEXT NOT NULL,
    url_pdf VARCHAR(500) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE epicrisis_jobs (
    id UUID PRIMARY KEY,
    embarazo_id UUID NOT NULL,
    medico_id UUID NOT NULL,
    clinica_id UUID NOT NULL,
    estado VARCHAR(20) NOT NULL,
    epicrisis_id UUID,
    mensaje_error TEXT,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_epicrisis_job_epicrisis FOREIGN KEY (epicrisis_id) REFERENCES epicrisis(id)
);

CREATE INDEX idx_epicrisis_embarazo ON epicrisis(embarazo_id);
CREATE INDEX idx_epicrisis_clinica ON epicrisis(clinica_id);
CREATE INDEX idx_epicrisis_jobs_clinica ON epicrisis_jobs(clinica_id);
