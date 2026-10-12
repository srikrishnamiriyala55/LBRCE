ALTER TABLE otp_challenge
    ADD COLUMN provider_message_id VARCHAR(255) NULL AFTER failed_attempts,
    ADD COLUMN delivery_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' AFTER provider_message_id;

CREATE INDEX idx_otp_delivery_status ON otp_challenge (delivery_status);
