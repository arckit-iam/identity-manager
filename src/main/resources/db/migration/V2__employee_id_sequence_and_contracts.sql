-- Matricules generes : I<6 chiffres>. La sequence ne fait que compter, le prefixe est applique par l'application.
CREATE SEQUENCE employee_id_seq START 1 MAXVALUE 999999 NO CYCLE;

-- Contrats d'une identite. Le type (salarie, prestataire...) est defini par la configuration du deploiement ;
-- les metadonnees propres au type sont stockees en JSONB.
CREATE TABLE contract (
    id          BIGSERIAL PRIMARY KEY,
    identity_id BIGINT       NOT NULL REFERENCES identity (id) ON DELETE CASCADE,
    type        VARCHAR(100) NOT NULL,
    start_date  DATE         NOT NULL,
    end_date    DATE,
    metadata    JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT contract_period CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_contract_identity ON contract (identity_id);
CREATE INDEX idx_contract_type ON contract (type);