--liquibase formatted sql
--changeset awki:07_alertas

CREATE TABLE alertas (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    embarazo_id             UUID            NOT NULL REFERENCES embarazos(id),
    paciente_id             UUID            REFERENCES pacientes(id),
    medico_id               UUID            REFERENCES medicos(id),
    clinica_id              UUID            NOT NULL REFERENCES clinicas(id),
    tipo_alerta             VARCHAR(50)     NOT NULL,
    nivel_urgencia          VARCHAR(20)     NOT NULL,
    descripcion             TEXT            NOT NULL,
    sintomas_disparadores   JSONB,
    origen                  VARCHAR(50)     NOT NULL,
    vista_por_medico        BOOLEAN         NOT NULL DEFAULT FALSE,
    fecha_vista             TIMESTAMP WITHOUT TIME ZONE,
    estado_entrega          VARCHAR(50)     NOT NULL,
    websocket_enviado       BOOLEAN         NOT NULL DEFAULT FALSE,
    sms_enviado             BOOLEAN         NOT NULL DEFAULT FALSE,
    whatsapp_enviado        BOOLEAN         NOT NULL DEFAULT FALSE,
    fcm_enviado             BOOLEAN         NOT NULL DEFAULT FALSE,
    intentos_sms            INTEGER         NOT NULL DEFAULT 0,
    intentos_whatsapp       INTEGER         NOT NULL DEFAULT 0,
    intentos_fcm            INTEGER         NOT NULL DEFAULT 0,
    latitud                 DECIMAL(10,7),
    longitud                DECIMAL(10,7),
    mensaje_libre           VARCHAR(500),
    created_at              TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE contactos_emergencia (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    paciente_id             UUID            NOT NULL REFERENCES pacientes(id),
    nombre                  VARCHAR(100)    NOT NULL,
    telefono                VARCHAR(20)     NOT NULL,
    parentesco              VARCHAR(50),
    canal_preferido         VARCHAR(30)     NOT NULL,
    activo                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE dispositivos_medico (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    medico_id               UUID            NOT NULL REFERENCES medicos(id),
    fcm_token               VARCHAR(255)    NOT NULL,
    updated_at              TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_medico_token UNIQUE (medico_id, fcm_token)
);

CREATE INDEX idx_alertas_embarazo_id ON alertas(embarazo_id);
CREATE INDEX idx_alertas_medico_id   ON alertas(medico_id);
CREATE INDEX idx_alertas_clinica_id  ON alertas(clinica_id);
CREATE INDEX idx_alertas_estado_entrega ON alertas(estado_entrega);
CREATE INDEX idx_contactos_emergencia_paciente_id ON contactos_emergencia(paciente_id);
CREATE INDEX idx_dispositivos_medico_medico_id ON dispositivos_medico(medico_id);
