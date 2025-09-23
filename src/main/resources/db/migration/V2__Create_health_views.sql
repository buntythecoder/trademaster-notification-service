-- V2__Create_health_views.sql
-- Create health and performance views required by health checks

-- Add missing columns to notification_errors table first
ALTER TABLE notification_errors
ADD COLUMN IF NOT EXISTS error_code VARCHAR(50),
ADD COLUMN IF NOT EXISTS notification_id VARCHAR(36),
ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(255),
ADD COLUMN IF NOT EXISTS stack_trace TEXT,
ADD COLUMN IF NOT EXISTS context JSONB,
ADD COLUMN IF NOT EXISTS resolved BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS resolved_by VARCHAR(100),
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Add missing columns to notification_metrics table
ALTER TABLE notification_metrics
ADD COLUMN IF NOT EXISTS metric_type VARCHAR(50) DEFAULT 'GAUGE',
ADD COLUMN IF NOT EXISTS tags JSONB,
ADD COLUMN IF NOT EXISTS timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Simple notification health summary view
CREATE VIEW v_notification_health_summary AS
SELECT
    'notification-service' as service_name,
    COUNT(DISTINCT nh.notification_id) as total_notifications,
    COUNT(DISTINCT CASE WHEN nh.status = 'SENT' THEN nh.notification_id END) as sent_notifications,
    COUNT(DISTINCT CASE WHEN nh.status = 'FAILED' THEN nh.notification_id END) as failed_notifications,
    ROUND(
        (COUNT(DISTINCT CASE WHEN nh.status = 'SENT' THEN nh.notification_id END) * 100.0) /
        NULLIF(COUNT(DISTINCT nh.notification_id), 0), 2
    ) as success_rate_percent,
    COUNT(DISTINCT ne.id) as total_errors,
    COUNT(DISTINCT CASE WHEN ne.resolved = false THEN ne.id END) as unresolved_errors,
    CURRENT_TIMESTAMP as last_updated
FROM notification_history nh
LEFT JOIN notification_errors ne ON ne.notification_id = nh.notification_id
WHERE nh.created_at >= CURRENT_TIMESTAMP - INTERVAL '24 hours';

-- Notification Performance Metrics View
CREATE VIEW v_notification_performance_metrics AS
SELECT
    'notification-service' as service_name,
    metric_name,
    metric_type,
    AVG(metric_value) as avg_value,
    MIN(metric_value) as min_value,
    MAX(metric_value) as max_value,
    COUNT(*) as sample_count,
    MAX(timestamp) as last_updated
FROM notification_metrics
WHERE timestamp >= CURRENT_TIMESTAMP - INTERVAL '1 hour'
GROUP BY metric_name, metric_type

UNION ALL

-- Add derived performance metrics from notification_history
SELECT
    'notification-service' as service_name,
    'notification_throughput' as metric_name,
    'GAUGE' as metric_type,
    COUNT(*)::decimal as avg_value,
    COUNT(*)::decimal as min_value,
    COUNT(*)::decimal as max_value,
    1 as sample_count,
    MAX(created_at) as last_updated
FROM notification_history
WHERE created_at >= CURRENT_TIMESTAMP - INTERVAL '1 hour'

UNION ALL

SELECT
    'notification-service' as service_name,
    'error_rate' as metric_name,
    'PERCENTAGE' as metric_type,
    ROUND(
        (COUNT(CASE WHEN status = 'FAILED' THEN 1 END) * 100.0) /
        NULLIF(COUNT(*), 0), 2
    ) as avg_value,
    0 as min_value,
    100 as max_value,
    COUNT(*) as sample_count,
    MAX(created_at) as last_updated
FROM notification_history
WHERE created_at >= CURRENT_TIMESTAMP - INTERVAL '1 hour';

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_notification_history_status_created
    ON notification_history(status, created_at);

CREATE INDEX IF NOT EXISTS idx_notification_metrics_timestamp
    ON notification_metrics(timestamp);

CREATE INDEX IF NOT EXISTS idx_notification_errors_resolved
    ON notification_errors(resolved);

CREATE INDEX IF NOT EXISTS idx_notification_errors_correlation
    ON notification_errors(correlation_id);

CREATE INDEX IF NOT EXISTS idx_notification_errors_created
    ON notification_errors(created_at);