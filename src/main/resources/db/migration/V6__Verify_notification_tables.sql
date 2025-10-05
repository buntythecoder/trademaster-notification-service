-- V6__Verify_notification_tables.sql
-- Verification script to ensure all required tables exist for notification-service entities

-- Verify all required notification service tables exist
DO $$
BEGIN
    -- Check for notification_history table
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'notification_history') THEN
        RAISE EXCEPTION 'CRITICAL: notification_history table is missing (expected from V3)';
    ELSE
        RAISE NOTICE 'SUCCESS: notification_history table exists';
    END IF;

    -- Check for notification_templates table
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'notification_templates') THEN
        RAISE EXCEPTION 'CRITICAL: notification_templates table is missing (expected from V4)';
    ELSE
        RAISE NOTICE 'SUCCESS: notification_templates table exists';
    END IF;

    -- Check for template_variables table
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'template_variables') THEN
        RAISE EXCEPTION 'CRITICAL: template_variables table is missing (expected from V4)';
    ELSE
        RAISE NOTICE 'SUCCESS: template_variables table exists';
    END IF;

    -- Check for template_optional_variables table
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'template_optional_variables') THEN
        RAISE EXCEPTION 'CRITICAL: template_optional_variables table is missing (expected from V4)';
    ELSE
        RAISE NOTICE 'SUCCESS: template_optional_variables table exists';
    END IF;

    -- Check for user_notification_preferences table
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_notification_preferences') THEN
        RAISE EXCEPTION 'CRITICAL: user_notification_preferences table is missing (expected from V5)';
    ELSE
        RAISE NOTICE 'SUCCESS: user_notification_preferences table exists';
    END IF;

    -- Check for user_enabled_notification_types table
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_enabled_notification_types') THEN
        RAISE EXCEPTION 'CRITICAL: user_enabled_notification_types table is missing (expected from V5)';
    ELSE
        RAISE NOTICE 'SUCCESS: user_enabled_notification_types table exists';
    END IF;

    -- Check for user_notification_categories table
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_notification_categories') THEN
        RAISE EXCEPTION 'CRITICAL: user_notification_categories table is missing (expected from V5)';
    ELSE
        RAISE NOTICE 'SUCCESS: user_notification_categories table exists';
    END IF;

    -- Check for notification_template_variables table (from V3)
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'notification_template_variables') THEN
        RAISE NOTICE 'INFO: notification_template_variables table is missing (optional from V3)';
    ELSE
        RAISE NOTICE 'SUCCESS: notification_template_variables table exists';
    END IF;

    -- Check for audit tables
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'notification_audit_log') THEN
        RAISE NOTICE 'WARNING: notification_audit_log table is missing (expected from V1)';
    ELSE
        RAISE NOTICE 'SUCCESS: notification_audit_log table exists';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'notification_metrics') THEN
        RAISE NOTICE 'WARNING: notification_metrics table is missing (expected from V1)';
    ELSE
        RAISE NOTICE 'SUCCESS: notification_metrics table exists';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'notification_errors') THEN
        RAISE NOTICE 'WARNING: notification_errors table is missing (expected from V1)';
    ELSE
        RAISE NOTICE 'SUCCESS: notification_errors table exists';
    END IF;

    RAISE NOTICE 'Table verification completed for notification-service';
END $$;

-- Verify that essential columns exist and have correct types
DO $$
BEGIN
    -- Verify notification_templates primary key
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'notification_templates'
        AND column_name = 'template_id'
        AND is_nullable = 'NO'
    ) THEN
        RAISE EXCEPTION 'CRITICAL: notification_templates.template_id column is missing or nullable';
    END IF;

    -- Verify user_notification_preferences primary key
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'user_notification_preferences'
        AND column_name = 'preference_id'
        AND is_nullable = 'NO'
    ) THEN
        RAISE EXCEPTION 'CRITICAL: user_notification_preferences.preference_id column is missing or nullable';
    END IF;

    -- Verify foreign key relationships
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'template_variables') THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name
            WHERE tc.table_name = 'template_variables'
            AND tc.constraint_type = 'FOREIGN KEY'
            AND kcu.column_name = 'template_id'
        ) THEN
            RAISE WARNING 'Foreign key constraint missing for template_variables.template_id';
        END IF;
    END IF;

    RAISE NOTICE 'Column and constraint verification completed for notification-service';
END $$;