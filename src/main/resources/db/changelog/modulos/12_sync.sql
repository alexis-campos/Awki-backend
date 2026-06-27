--liquibase formatted sql
--changeset awki:12_sync

CREATE TABLE dispositivos_sync (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    paciente_id             UUID            NOT NULL REFERENCES pacientes(id),
    device_id               VARCHAR(255)    NOT NULL,
    ultima_sincronizacion   TIMESTAMP WITHOUT TIME ZONE,
    created_at              TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_paciente_device UNIQUE (paciente_id, device_id)
);

CREATE TABLE sync_items_procesados (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id           VARCHAR(255)    NOT NULL,
    paciente_id         UUID            NOT NULL REFERENCES pacientes(id),
    offline_timestamp   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_device_paciente_ts UNIQUE (device_id, paciente_id, offline_timestamp)
);

ALTER TABLE mensajes_chat
    ADD COLUMN device_id            VARCHAR(255),
    ADD COLUMN offline_timestamp    TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE alertas
    ADD COLUMN alerta_retroactiva   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN fecha_retroactiva    TIMESTAMP WITHOUT TIME ZONE;

CREATE INDEX idx_dispositivos_sync_paciente ON dispositivos_sync(paciente_id);
CREATE INDEX idx_sync_items_device_paciente ON sync_items_procesados(device_id, paciente_id);
