CREATE TABLE events (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    titulo           VARCHAR(255) NOT NULL,
    descricao        TEXT,
    data_hora_inicio TIMESTAMPTZ  NOT NULL,
    data_hora_fim    TIMESTAMPTZ  NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    external_user_id UUID         NOT NULL REFERENCES users(id),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by       UUID,
    updated_by       UUID
);

CREATE INDEX idx_events_external_periodo ON events (external_user_id, data_hora_inicio, data_hora_fim);
CREATE INDEX idx_events_status ON events (status);
