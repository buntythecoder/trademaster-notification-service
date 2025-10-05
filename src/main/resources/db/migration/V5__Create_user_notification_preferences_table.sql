-- V5__Create_user_notification_preferences_table.sql
-- Create user notification preferences and related tables

-- Create user_notification_preferences table
CREATE TABLE IF NOT EXISTS user_notification_preferences (
    preference_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(100) UNIQUE NOT NULL,
    notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    preferred_channel VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
    email_address VARCHAR(255),
    phone_number VARCHAR(20),
    quiet_hours_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    quiet_start_time TIME,
    quiet_end_time TIME,
    time_zone VARCHAR(50) DEFAULT 'UTC',
    frequency_limit_per_hour INTEGER,
    frequency_limit_per_day INTEGER,
    marketing_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    system_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    trading_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    account_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    language VARCHAR(10) DEFAULT 'en',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL
);

-- Create user_enabled_notification_types table for enabled notification channels
CREATE TABLE IF NOT EXISTS user_enabled_notification_types (
    preference_id VARCHAR(36) NOT NULL,
    notification_type VARCHAR(20) NOT NULL,
    PRIMARY KEY (preference_id, notification_type),
    CONSTRAINT fk_enabled_types_preference
        FOREIGN KEY (preference_id)
        REFERENCES user_notification_preferences(preference_id)
        ON DELETE CASCADE
);

-- Create user_notification_categories table for enabled notification categories
CREATE TABLE IF NOT EXISTS user_notification_categories (
    preference_id VARCHAR(36) NOT NULL,
    category VARCHAR(50) NOT NULL,
    PRIMARY KEY (preference_id, category),
    CONSTRAINT fk_categories_preference
        FOREIGN KEY (preference_id)
        REFERENCES user_notification_preferences(preference_id)
        ON DELETE CASCADE
);

-- Create indexes for user_notification_preferences as defined in the entity
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_preferences ON user_notification_preferences(user_id);
CREATE INDEX IF NOT EXISTS idx_preferences_enabled ON user_notification_preferences(notifications_enabled);
CREATE INDEX IF NOT EXISTS idx_preferences_channel ON user_notification_preferences(preferred_channel);
CREATE INDEX IF NOT EXISTS idx_preferences_created ON user_notification_preferences(created_at);

-- Add trigger to automatically update updated_at timestamp for preferences
CREATE OR REPLACE FUNCTION update_user_preferences_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_user_preferences_updated_at
    BEFORE UPDATE ON user_notification_preferences
    FOR EACH ROW
    EXECUTE FUNCTION update_user_preferences_updated_at();

-- Add constraints for enum-like values
ALTER TABLE user_notification_preferences
ADD CONSTRAINT chk_preferred_channel
CHECK (preferred_channel IN ('EMAIL', 'SMS', 'IN_APP', 'PUSH'));

ALTER TABLE user_enabled_notification_types
ADD CONSTRAINT chk_enabled_notification_type
CHECK (notification_type IN ('EMAIL', 'SMS', 'IN_APP', 'PUSH'));

ALTER TABLE user_notification_categories
ADD CONSTRAINT chk_enabled_category
CHECK (category IN ('AUTHENTICATION', 'TRANSACTIONAL', 'MARKETING', 'SYSTEM', 'TRADING', 'ACCOUNT', 'SUPPORT', 'COMPLIANCE', 'WELCOME', 'NOTIFICATION'));

-- Insert default preferences for system testing users
INSERT INTO user_notification_preferences (
    preference_id, user_id, notifications_enabled, preferred_channel,
    email_address, marketing_enabled, system_alerts_enabled,
    trading_alerts_enabled, account_alerts_enabled, language,
    created_by, updated_by
) VALUES
(
    gen_random_uuid()::text,
    'system_user_1',
    TRUE,
    'EMAIL',
    'system1@trademaster.com',
    FALSE,
    TRUE,
    TRUE,
    TRUE,
    'en',
    'system',
    'system'
),
(
    gen_random_uuid()::text,
    'test_user_1',
    TRUE,
    'EMAIL',
    'testuser1@example.com',
    FALSE,
    TRUE,
    TRUE,
    TRUE,
    'en',
    'system',
    'system'
);

-- Insert default enabled notification types for the test users
INSERT INTO user_enabled_notification_types (preference_id, notification_type)
SELECT preference_id, 'EMAIL' FROM user_notification_preferences WHERE user_id IN ('system_user_1', 'test_user_1')
UNION ALL
SELECT preference_id, 'IN_APP' FROM user_notification_preferences WHERE user_id IN ('system_user_1', 'test_user_1');

-- Insert default enabled categories for the test users
INSERT INTO user_notification_categories (preference_id, category)
SELECT preference_id, category
FROM user_notification_preferences
CROSS JOIN (VALUES ('SYSTEM'), ('TRADING'), ('ACCOUNT'), ('AUTHENTICATION')) AS categories(category)
WHERE user_id IN ('system_user_1', 'test_user_1');