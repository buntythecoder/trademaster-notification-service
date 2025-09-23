-- V1__Create_missing_audit_tables.sql
-- Create missing tables required by health check

-- Notification Audit Log table for comprehensive audit trail
CREATE TABLE IF NOT EXISTS notification_audit_log (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    correlation_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255),
    notification_id VARCHAR(36),
    action VARCHAR(100) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL
);

-- Notification Metrics table for performance and usage tracking
CREATE TABLE IF NOT EXISTS notification_metrics (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    metric_name VARCHAR(255) NOT NULL,
    metric_value DECIMAL(10,2) NOT NULL,
    metric_type VARCHAR(50) NOT NULL, -- COUNT, GAUGE, HISTOGRAM, etc.
    tags JSONB, -- For dimensional metrics
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Notification Errors table for error tracking and debugging
CREATE TABLE IF NOT EXISTS notification_errors (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    error_type VARCHAR(255) NOT NULL,
    error_message TEXT NOT NULL,
    error_code VARCHAR(50),
    notification_id VARCHAR(36),
    correlation_id VARCHAR(255),
    stack_trace TEXT,
    context JSONB, -- Additional error context
    resolved BOOLEAN DEFAULT FALSE,
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_audit_correlation ON notification_audit_log(correlation_id);
CREATE INDEX IF NOT EXISTS idx_audit_user ON notification_audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_created ON notification_audit_log(created_at);

CREATE INDEX IF NOT EXISTS idx_metrics_name ON notification_metrics(metric_name);
CREATE INDEX IF NOT EXISTS idx_metrics_timestamp ON notification_metrics(timestamp);
CREATE INDEX IF NOT EXISTS idx_metrics_type ON notification_metrics(metric_type);

CREATE INDEX IF NOT EXISTS idx_errors_type ON notification_errors(error_type);
CREATE INDEX IF NOT EXISTS idx_errors_correlation ON notification_errors(correlation_id);
CREATE INDEX IF NOT EXISTS idx_errors_created ON notification_errors(created_at);
CREATE INDEX IF NOT EXISTS idx_errors_resolved ON notification_errors(resolved);