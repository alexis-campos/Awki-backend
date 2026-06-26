--liquibase formatted sql

--changeset alonzo:03_embarazos
CREATE TABLE embarazos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    paciente_id UUID NOT NULL,
    medico_id UUID,
    fecha_ultima_menstruacion DATE NOT NULL,
    fecha_probable_parto DATE NOT NULL,
    fecha_probable_parto_eco DATE,
    semanas_gestacion_ingreso INTEGER NOT NULL,
    numero_gestacion INTEGER NOT NULL,
    numero_partos INTEGER NOT NULL DEFAULT 0,
    numero_abortos INTEGER NOT NULL DEFAULT 0,
    numero_cesareas INTEGER NOT NULL DEFAULT 0,
    embarazo_multiple BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(50) NOT NULL,
    fecha_fin DATE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_embarazos_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes(id),
    CONSTRAINT fk_embarazos_medico FOREIGN KEY (medico_id) REFERENCES medicos(id)
);
--rollback DROP TABLE IF EXISTS embarazos;

--changeset alonzo:03_antecedentes_clinicos
CREATE TABLE antecedentes_clinicos (
    embarazo_id UUID PRIMARY KEY,
    diabetes_previa BOOLEAN NOT NULL DEFAULT FALSE,
    hipertension_previa BOOLEAN NOT NULL DEFAULT FALSE,
    preeclampsia_previa BOOLEAN NOT NULL DEFAULT FALSE,
    enfermedad_renal BOOLEAN NOT NULL DEFAULT FALSE,
    enfermedad_autoinmune BOOLEAN NOT NULL DEFAULT FALSE,
    anemia_previa BOOLEAN NOT NULL DEFAULT FALSE,
    vih_positivo BOOLEAN NOT NULL DEFAULT FALSE,
    sifilis_previa BOOLEAN NOT NULL DEFAULT FALSE,
    trastorno_coagulacion BOOLEAN NOT NULL DEFAULT FALSE,
    edad_materna_riesgo BOOLEAN NOT NULL DEFAULT FALSE,
    residencia_altitud BOOLEAN NOT NULL DEFAULT FALSE,
    obesidad_pregestacional BOOLEAN NOT NULL DEFAULT FALSE,
    observaciones TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_antecedentes_embarazo FOREIGN KEY (embarazo_id) REFERENCES embarazos(id) ON DELETE CASCADE
);
--rollback DROP TABLE IF EXISTS antecedentes_clinicos;
