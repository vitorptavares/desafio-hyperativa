CREATE TABLE app_user (
    id              CHAR(36)     NOT NULL PRIMARY KEY,
    username        VARCHAR(60)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(30)  NOT NULL DEFAULT 'USER',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE batch (
    id              CHAR(36)     NOT NULL PRIMARY KEY,
    name            VARCHAR(60)  NOT NULL,
    batch_date      DATE         NOT NULL,
    batch_number    VARCHAR(20)  NOT NULL UNIQUE,
    record_count    INT          NOT NULL,
    source          VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE card (
    id              CHAR(36)     NOT NULL PRIMARY KEY,
    card_hash       VARCHAR(64)  NOT NULL UNIQUE,
    sequence_number VARCHAR(10),
    batch_id        CHAR(36)     NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_card_batch FOREIGN KEY (batch_id) REFERENCES batch(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_card_batch_id ON card(batch_id);
