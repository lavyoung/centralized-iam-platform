-- Client registration aggregate persistence for PostgreSQL 16.
-- Composite tenant foreign keys prevent child rows from crossing tenant boundaries.

CREATE TABLE iam_client_registration
(
    id                             BIGINT                      NOT NULL,
    tenant_id                      VARCHAR(64) COLLATE "C"     NOT NULL,
    client_id                      VARCHAR(128) COLLATE "C"    NOT NULL,
    display_name                   VARCHAR(128)                NOT NULL,
    client_secret_hash             VARCHAR(255) COLLATE "C" NULL,
    status                         VARCHAR(32) COLLATE "C"     NOT NULL,
    access_token_ttl_seconds       BIGINT                      NOT NULL,
    refresh_token_ttl_seconds      BIGINT                      NOT NULL,
    authorization_code_ttl_seconds BIGINT                      NOT NULL,
    registered_at                  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at                     TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_iam_client_registration PRIMARY KEY (id),
    CONSTRAINT uk_iam_client_registration_tenant_client UNIQUE (tenant_id, client_id),
    CONSTRAINT uk_iam_client_registration_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT ck_iam_client_registration_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_iam_client_registration_ttl CHECK (
        access_token_ttl_seconds > 0
            AND refresh_token_ttl_seconds > access_token_ttl_seconds
            AND authorization_code_ttl_seconds > 0
            AND authorization_code_ttl_seconds < access_token_ttl_seconds
        )
);

CREATE INDEX idx_iam_client_registration_tenant_status
    ON iam_client_registration (tenant_id, status);

CREATE TABLE iam_client_authentication_method
(
    id                     BIGINT                  NOT NULL,
    tenant_id              VARCHAR(64) COLLATE "C" NOT NULL,
    client_registration_id BIGINT                  NOT NULL,
    authentication_method  VARCHAR(64) COLLATE "C" NOT NULL,
    CONSTRAINT pk_iam_client_auth_method PRIMARY KEY (id),
    CONSTRAINT uk_iam_client_auth_method_value UNIQUE (tenant_id, client_registration_id, authentication_method),
    CONSTRAINT fk_iam_client_auth_method_registration FOREIGN KEY (tenant_id, client_registration_id)
        REFERENCES iam_client_registration (tenant_id, id) ON DELETE CASCADE
);

CREATE TABLE iam_client_authorization_grant_type
(
    id                     BIGINT                  NOT NULL,
    tenant_id              VARCHAR(64) COLLATE "C" NOT NULL,
    client_registration_id BIGINT                  NOT NULL,
    grant_type             VARCHAR(64) COLLATE "C" NOT NULL,
    CONSTRAINT pk_iam_client_grant_type PRIMARY KEY (id),
    CONSTRAINT uk_iam_client_grant_type_value UNIQUE (tenant_id, client_registration_id, grant_type),
    CONSTRAINT fk_iam_client_grant_type_registration FOREIGN KEY (tenant_id, client_registration_id)
        REFERENCES iam_client_registration (tenant_id, id) ON DELETE CASCADE
);

CREATE TABLE iam_client_redirect_uri
(
    id                     BIGINT                    NOT NULL,
    tenant_id              VARCHAR(64) COLLATE "C"   NOT NULL,
    client_registration_id BIGINT                    NOT NULL,
    redirect_uri           VARCHAR(2048) COLLATE "C" NOT NULL,
    redirect_uri_hash      BYTEA GENERATED ALWAYS AS (sha256(convert_to(redirect_uri, 'UTF8'))) STORED,
    CONSTRAINT pk_iam_client_redirect_uri PRIMARY KEY (id),
    CONSTRAINT uk_iam_client_redirect_uri_value UNIQUE (tenant_id, client_registration_id, redirect_uri_hash),
    CONSTRAINT fk_iam_client_redirect_uri_registration FOREIGN KEY (tenant_id, client_registration_id)
        REFERENCES iam_client_registration (tenant_id, id) ON DELETE CASCADE
);

CREATE TABLE iam_client_scope
(
    id                     BIGINT                   NOT NULL,
    tenant_id              VARCHAR(64) COLLATE "C"  NOT NULL,
    client_registration_id BIGINT                   NOT NULL,
    scope                  VARCHAR(128) COLLATE "C" NOT NULL,
    CONSTRAINT pk_iam_client_scope PRIMARY KEY (id),
    CONSTRAINT uk_iam_client_scope_value UNIQUE (tenant_id, client_registration_id, scope),
    CONSTRAINT fk_iam_client_scope_registration FOREIGN KEY (tenant_id, client_registration_id)
        REFERENCES iam_client_registration (tenant_id, id) ON DELETE CASCADE
);
