CREATE TABLE IF NOT EXISTS otp_challenge (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(254) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    otp_hash VARCHAR(64) NOT NULL,
    salt VARCHAR(32) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    last_sent_at DATETIME(6) NOT NULL,
    window_started_at DATETIME(6) NOT NULL,
    send_count INT NOT NULL DEFAULT 1,
    failed_attempts INT NOT NULL DEFAULT 0,
    consumed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_otp_email_purpose UNIQUE (email, purpose),
    INDEX idx_otp_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
