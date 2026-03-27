-- ============================================
-- Education Platform - Database Initialization
-- This script runs automatically on first MySQL startup
-- ============================================

-- Create databases for each microservice
CREATE DATABASE IF NOT EXISTS edu_user
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS edu_course
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- Grant permissions (for development environment)
-- In production, create specific users with limited permissions
GRANT ALL PRIVILEGES ON edu_user.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON edu_course.* TO 'root'@'%';

FLUSH PRIVILEGES;

-- Verify databases created
SELECT 'Databases initialized successfully!' AS status;
SHOW DATABASES LIKE 'edu_%';
