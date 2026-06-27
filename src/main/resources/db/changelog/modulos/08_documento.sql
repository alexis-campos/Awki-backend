--liquibase formatted sql

--changeset awki:08_documentos
CREATE TABLE documentos (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            embarazo_id UUID NOT NULL,
                            subido_por VARCHAR(50) NOT NULL,
                            subido_por_id UUID NOT NULL,
                            tipo_documento VARCHAR(50) NOT NULL,
                            nombre_archivo VARCHAR(200) NOT NULL,
                            content_type VARCHAR(100) NOT NULL,
                            tamano_bytes BIGINT NOT NULL,
                            storage_key VARCHAR(500) NOT NULL,
                            eliminado BOOLEAN NOT NULL DEFAULT FALSE,
                            created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                            updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                            CONSTRAINT fk_documentos_embarazo FOREIGN KEY (embarazo_id) REFERENCES embarazos(id)
);
--rollback DROP TABLE IF EXISTS documentos;

--changeset awki:08_idx_documentos_embarazo
CREATE INDEX idx_documentos_embarazo_id ON documentos(embarazo_id);
--rollback DROP INDEX IF EXISTS idx_documentos_embarazo_id;

--changeset awki:08_idx_documentos_eliminado
CREATE INDEX idx_documentos_eliminado ON documentos(eliminado);
--rollback DROP INDEX IF EXISTS idx_documentos_eliminado;