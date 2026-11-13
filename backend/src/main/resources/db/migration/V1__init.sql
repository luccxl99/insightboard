-- Modelo multi-tenant: schema compartilhado, isolamento por coluna tenant_id.

CREATE TABLE tenants (
    id          UUID PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL
);

CREATE TABLE users (
    id             UUID PRIMARY KEY,
    tenant_id      UUID         NOT NULL REFERENCES tenants (id),
    name           VARCHAR(120) NOT NULL,
    email          VARCHAR(180) NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,
    role           VARCHAR(20)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_users_tenant ON users (tenant_id);

CREATE TABLE datasets (
    id                 UUID PRIMARY KEY,
    tenant_id          UUID         NOT NULL REFERENCES tenants (id),
    name               VARCHAR(150) NOT NULL,
    original_filename  VARCHAR(255) NOT NULL,
    row_count          INTEGER      NOT NULL,
    columns            JSONB        NOT NULL,
    created_by         VARCHAR(180) NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_datasets_tenant ON datasets (tenant_id, created_at DESC);

CREATE TABLE dataset_rows (
    id           UUID PRIMARY KEY,
    dataset_id   UUID    NOT NULL REFERENCES datasets (id) ON DELETE CASCADE,
    tenant_id    UUID    NOT NULL,
    line_number  INTEGER NOT NULL,
    data         JSONB   NOT NULL
);
CREATE INDEX idx_rows_dataset ON dataset_rows (dataset_id, line_number);

CREATE TABLE audit_logs (
    id          UUID PRIMARY KEY,
    tenant_id   UUID         NOT NULL,
    actor       VARCHAR(180) NOT NULL,
    action      VARCHAR(60)  NOT NULL,
    entity      VARCHAR(60),
    entity_id   VARCHAR(64),
    detail      VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_audit_tenant ON audit_logs (tenant_id, created_at DESC);
