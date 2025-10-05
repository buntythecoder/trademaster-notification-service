-- V3__Create_notification_history_table.sql
-- Create the core notification_history table and related structures

-- Create notification_history table
CREATE TABLE IF NOT EXISTS notification_history (
    notification_id VARCHAR(36) PRIMARY KEY,
    correlation_id VARCHAR(36),
    notification_type VARCHAR(20) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    email_recipient VARCHAR(255),
    phone_recipient VARCHAR(50),
    subject VARCHAR(500),
    content TEXT NOT NULL,
    template_name VARCHAR(100),
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    external_message_id VARCHAR(255),
    error_message VARCHAR(1000),
    scheduled_at TIMESTAMP,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    read_at TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retry_attempts INTEGER,
    reference_id VARCHAR(100),
    reference_type VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL
);

-- Create notification_template_variables table for template variables
CREATE TABLE IF NOT EXISTS notification_template_variables (
    notification_id VARCHAR(36) NOT NULL,
    variable_key VARCHAR(100) NOT NULL,
    variable_value VARCHAR(1000),
    PRIMARY KEY (notification_id, variable_key),
    CONSTRAINT fk_template_variables_notification
        FOREIGN KEY (notification_id)
        REFERENCES notification_history(notification_id)
        ON DELETE CASCADE
);

-- Create indexes for performance as defined in the entity
CREATE INDEX IF NOT EXISTS idx_notification_recipient ON notification_history(recipient);
CREATE INDEX IF NOT EXISTS idx_notification_type ON notification_history(notification_type);
CREATE INDEX IF NOT EXISTS idx_notification_status ON notification_history(status);
CREATE INDEX IF NOT EXISTS idx_notification_created ON notification_history(created_at);
CREATE INDEX IF NOT EXISTS idx_notification_correlation ON notification_history(correlation_id);

-- Additional performance indexes
CREATE INDEX IF NOT EXISTS idx_notification_status_created ON notification_history(status, created_at);
CREATE INDEX IF NOT EXISTS idx_notification_type_status ON notification_history(notification_type, status);
CREATE INDEX IF NOT EXISTS idx_notification_sent_at ON notification_history(sent_at);
CREATE INDEX IF NOT EXISTS idx_notification_priority ON notification_history(priority);
CREATE INDEX IF NOT EXISTS idx_notification_reference ON notification_history(reference_id, reference_type);

-- Create index on template variables for lookups
CREATE INDEX IF NOT EXISTS idx_template_variables_key ON notification_template_variables(variable_key);

-- Add trigger to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_notification_history_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_notification_history_updated_at
    BEFORE UPDATE ON notification_history
    FOR EACH ROW
    EXECUTE FUNCTION update_notification_history_updated_at();