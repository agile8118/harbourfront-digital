CREATE TABLE IF NOT EXISTS contact_submissions (
    id           SERIAL PRIMARY KEY,
    name         VARCHAR(255)             NOT NULL,
    email        VARCHAR(255)             NOT NULL,
    message      TEXT                     NOT NULL,
    ip_address   VARCHAR(45),
    submitted_at TIMESTAMPTZ DEFAULT NOW()
);