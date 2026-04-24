CREATE TABLE IF NOT EXISTS subscribers (
    id           SERIAL PRIMARY KEY,
    email        VARCHAR(255)  NOT NULL UNIQUE,
    token        UUID          NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    confirmed    BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);