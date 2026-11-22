-- Identites humaines, cle metier : le matricule (employee_id).
CREATE TABLE identity (
    id          BIGSERIAL PRIMARY KEY,
    employee_id VARCHAR(100) NOT NULL UNIQUE,
    first_name  VARCHAR(255),
    last_name   VARCHAR(255),
    email       VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Comptes utilisateurs. Les metadonnees sont libres cote base (JSONB) : leurs champs autorises sont decrits
-- par le type de compte, defini dans la configuration du deploiement (pas de migration par nouveau champ).
CREATE TABLE account (
    id          BIGSERIAL PRIMARY KEY,
    identifier  VARCHAR(255) NOT NULL UNIQUE,
    type        VARCHAR(100) NOT NULL,
    identity_id BIGINT REFERENCES identity (id) ON DELETE SET NULL,
    source      VARCHAR(20)  NOT NULL,
    metadata    JSONB        NOT NULL DEFAULT '{}'::jsonb,
    job_id      UUID,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_account_identity ON account (identity_id);
CREATE INDEX idx_account_type ON account (type);
CREATE INDEX idx_account_metadata ON account USING GIN (metadata);
