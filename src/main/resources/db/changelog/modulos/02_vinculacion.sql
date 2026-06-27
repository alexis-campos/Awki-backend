--liquibase formatted sql

--changeset awki:02_vinculos_medico_paciente
CREATE TABLE vinculos_medico_paciente (
                                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                          medico_id UUID NOT NULL,
                                          paciente_id UUID NOT NULL,
                                          clinica_id UUID NOT NULL,
                                          estado VARCHAR(50) NOT NULL,
                                          vinculado_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                                          finalizado_at TIMESTAMP WITHOUT TIME ZONE,
                                          created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                                          updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                                          CONSTRAINT fk_vinculos_medico FOREIGN KEY (medico_id) REFERENCES medicos(id),
                                          CONSTRAINT fk_vinculos_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes(id),
                                          CONSTRAINT fk_vinculos_clinica FOREIGN KEY (clinica_id) REFERENCES clinicas(id)
);
--rollback DROP TABLE IF EXISTS vinculos_medico_paciente;