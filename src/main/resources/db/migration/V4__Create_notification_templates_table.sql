-- V4__Create_notification_templates_table.sql
-- Create notification templates and user preferences tables

-- Create notification_templates table
CREATE TABLE IF NOT EXISTS notification_templates (
    template_id VARCHAR(36) PRIMARY KEY,
    template_name VARCHAR(100) UNIQUE NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    notification_type VARCHAR(20) NOT NULL,
    category VARCHAR(50) NOT NULL,
    subject_template VARCHAR(500),
    content_template TEXT NOT NULL,
    html_template TEXT,
    version INTEGER NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    default_priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    rate_limit_per_hour INTEGER,
    tags VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL
);

-- Create template_variables table for required variables
CREATE TABLE IF NOT EXISTS template_variables (
    template_id VARCHAR(36) NOT NULL,
    variable_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (template_id, variable_name),
    CONSTRAINT fk_template_variables_template
        FOREIGN KEY (template_id)
        REFERENCES notification_templates(template_id)
        ON DELETE CASCADE
);

-- Create template_optional_variables table for optional variables
CREATE TABLE IF NOT EXISTS template_optional_variables (
    template_id VARCHAR(36) NOT NULL,
    variable_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (template_id, variable_name),
    CONSTRAINT fk_template_optional_variables_template
        FOREIGN KEY (template_id)
        REFERENCES notification_templates(template_id)
        ON DELETE CASCADE
);

-- Create indexes for notification_templates as defined in the entity
CREATE UNIQUE INDEX IF NOT EXISTS idx_template_name ON notification_templates(template_name);
CREATE INDEX IF NOT EXISTS idx_template_category ON notification_templates(category);
CREATE INDEX IF NOT EXISTS idx_template_type ON notification_templates(notification_type);
CREATE INDEX IF NOT EXISTS idx_template_active ON notification_templates(active);
CREATE INDEX IF NOT EXISTS idx_template_created ON notification_templates(created_at);

-- Add trigger to automatically update updated_at timestamp for templates
CREATE OR REPLACE FUNCTION update_notification_templates_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_notification_templates_updated_at
    BEFORE UPDATE ON notification_templates
    FOR EACH ROW
    EXECUTE FUNCTION update_notification_templates_updated_at();

-- Add constraints for enum-like values
ALTER TABLE notification_templates
ADD CONSTRAINT chk_notification_type
CHECK (notification_type IN ('EMAIL', 'SMS', 'IN_APP', 'PUSH'));

ALTER TABLE notification_templates
ADD CONSTRAINT chk_template_category
CHECK (category IN ('AUTHENTICATION', 'TRANSACTIONAL', 'MARKETING', 'SYSTEM', 'TRADING', 'ACCOUNT', 'SUPPORT', 'COMPLIANCE', 'WELCOME', 'NOTIFICATION'));

ALTER TABLE notification_templates
ADD CONSTRAINT chk_default_priority
CHECK (default_priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT'));

-- Insert some default templates for system functionality
INSERT INTO notification_templates (
    template_id, template_name, display_name, description, notification_type,
    category, subject_template, content_template, created_by, updated_by
) VALUES
(
    gen_random_uuid()::text,
    'welcome_email',
    'Welcome Email Template',
    'Default welcome email for new users',
    'EMAIL',
    'WELCOME',
    'Welcome to TradeMaster, {{userName}}!',
    'Dear {{userName}},\n\nWelcome to TradeMaster! Your account has been successfully created.\n\nBest regards,\nTradeMaster Team',
    'system',
    'system'
),
(
    gen_random_uuid()::text,
    'order_confirmation',
    'Order Confirmation Template',
    'Template for order confirmation notifications',
    'EMAIL',
    'TRADING',
    'Order Confirmation - {{orderType}} {{symbol}}',
    'Dear {{userName}},\n\nYour {{orderType}} order for {{quantity}} shares of {{symbol}} has been {{status}}.\n\nOrder ID: {{orderId}}\nPrice: {{price}}\n\nThank you for using TradeMaster.',
    'system',
    'system'
),
(
    gen_random_uuid()::text,
    'system_maintenance',
    'System Maintenance Notification',
    'Template for system maintenance announcements',
    'EMAIL',
    'SYSTEM',
    'TradeMaster System Maintenance - {{maintenanceDate}}',
    'Dear User,\n\nWe will be performing scheduled maintenance on {{maintenanceDate}} from {{startTime}} to {{endTime}}.\n\nDuring this time, some services may be unavailable.\n\nThank you for your understanding.',
    'system',
    'system'
);