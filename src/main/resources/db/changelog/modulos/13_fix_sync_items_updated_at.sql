--liquibase formatted sql

--changeset awki:13-fix-sync-items-updated-at
ALTER TABLE sync_items_procesados
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW();
