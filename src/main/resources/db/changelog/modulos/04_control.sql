--liquibase formatted sql

--changeset awki:04_controles_prenatales
CREATE TABLE controles_prenatales (
                                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                      embarazo_id UUID NOT NULL,
                                      medico_id UUID NOT NULL,
                                      numero_control INTEGER NOT NULL,
                                      fecha_control DATE NOT NULL,
                                      semanas_gestacion INTEGER NOT NULL,
                                      peso_kg DECIMAL(5,2) NOT NULL,
                                      talla_cm DECIMAL(5,1),
                                      imc DECIMAL(4,2),
                                      presion_arterial_sistolica INTEGER NOT NULL,
                                      presion_arterial_diastolica INTEGER NOT NULL,
                                      altura_uterina_cm DECIMAL(4,1),
                                      frecuencia_cardiaca_fetal INTEGER,
                                      presentacion_fetal VARCHAR(50),
                                      hemoglobina_gdl DECIMAL(4,2),
                                      proteinuria VARCHAR(50),
                                      glucosa_mgdl DECIMAL(5,2),
                                      movimientos_fetales_reporte VARCHAR(50),
                                      edemas VARCHAR(50),
                                      proxima_cita DATE,
                                      nivel_riesgo_calculado VARCHAR(50) NOT NULL,
                                      observaciones_medico TEXT,
                                      created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                                      updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                                      CONSTRAINT fk_controles_embarazo FOREIGN KEY (embarazo_id) REFERENCES embarazos(id),
                                      CONSTRAINT fk_controles_medico FOREIGN KEY (medico_id) REFERENCES medicos(id)
);
--rollback DROP TABLE IF EXISTS controles_prenatales;

--changeset awki:04_idx_controles_embarazo
CREATE INDEX idx_controles_embarazo_id ON controles_prenatales(embarazo_id);
--rollback DROP INDEX IF EXISTS idx_controles_embarazo_id;