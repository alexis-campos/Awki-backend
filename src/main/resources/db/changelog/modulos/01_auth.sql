--liquibase formatted sql

--changeset awki:01_clinicas_stub
CREATE TABLE clinicas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
--rollback DROP TABLE IF EXISTS clinicas;

--changeset awki:01_usuarios
CREATE TABLE usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(50) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    clinica_id UUID REFERENCES clinicas(id),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_usuarios_email UNIQUE (email)
);
--rollback DROP TABLE IF EXISTS usuarios;

--changeset awki:01_pacientes
CREATE TABLE pacientes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    telefono VARCHAR(20),
    dni VARCHAR(20),
    fecha_nacimiento DATE NOT NULL,
    departamento VARCHAR(100),
    modo_uso VARCHAR(50) NOT NULL,
    consentimiento_ia BOOLEAN NOT NULL DEFAULT FALSE,
    consentimiento_fecha TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_pacientes_usuario UNIQUE (usuario_id),
    CONSTRAINT fk_pacientes_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);
--rollback DROP TABLE IF EXISTS pacientes;

--changeset awki:01_medicos
CREATE TABLE medicos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL,
    clinica_id UUID NOT NULL,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    cmp VARCHAR(20) NOT NULL,
    especialidad VARCHAR(100),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_medicos_usuario UNIQUE (usuario_id),
    CONSTRAINT uq_medicos_cmp UNIQUE (cmp),
    CONSTRAINT fk_medicos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT fk_medicos_clinica FOREIGN KEY (clinica_id) REFERENCES clinicas(id)
);
--rollback DROP TABLE IF EXISTS medicos;
