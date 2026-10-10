-- ==============================================================================
-- Schema Definition: Online Project Management Tool (MySQL 8.x)
-- ==============================================================================
-- Satisfies Academic Requirements:
-- 1. Classes for Database Operations - 7 marks
-- 2. JDBC Database Connectivity foundation
-- 3. Clean DAO architecture
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS project_management 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE project_management;

-- ------------------------------------------------------------------------------
-- 1. Users Table (Supports Polymorphic User Subtypes: Admin, ProjectManager, TeamMember)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) DEFAULT 'password123',
    role VARCHAR(30) NOT NULL,
    department VARCHAR(50),
    avatar_initials VARCHAR(10),
    is_active BOOLEAN DEFAULT TRUE,
    
    -- Subclass-specific columns for OOP Polymorphism persistence
    admin_level VARCHAR(50) DEFAULT 'SYSTEM_ADMIN',
    is_superuser BOOLEAN DEFAULT FALSE,
    budget_approval_limit DECIMAL(12, 2) DEFAULT 0.00,
    certification_level VARCHAR(100) DEFAULT 'PMP',
    technical_specialization VARCHAR(100) DEFAULT 'General',
    sprint_velocity INT DEFAULT 30,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_users_email (email),
    INDEX idx_users_role (role),
    INDEX idx_users_username (username)
) ENGINE=InnoDB;

-- ------------------------------------------------------------------------------
-- 2. Projects Table
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'PLANNING',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    manager_id BIGINT,
    start_date DATE,
    end_date DATE,
    budget DECIMAL(12, 2) DEFAULT 0.00,
    spent_amount DECIMAL(12, 2) DEFAULT 0.00,
    progress_percentage INT DEFAULT 0,
    total_tasks INT DEFAULT 0,
    completed_tasks INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_project_manager FOREIGN KEY (manager_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_projects_manager (manager_id),
    INDEX idx_projects_status (status),
    INDEX idx_projects_priority (priority)
) ENGINE=InnoDB;

-- ------------------------------------------------------------------------------
-- 3. Project Team Members (Many-to-Many Join Table with Composite Primary Key)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS team_members (
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (project_id, user_id),
    CONSTRAINT fk_tm_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_tm_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_tm_user (user_id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------------------------
-- 4. Tasks Table
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'TODO',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    assignee_id BIGINT,
    reporter_id BIGINT,
    due_date DATE,
    estimated_hours INT DEFAULT 0,
    logged_hours INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_assignee FOREIGN KEY (assignee_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_task_reporter FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_tasks_project (project_id),
    INDEX idx_tasks_assignee (assignee_id),
    INDEX idx_tasks_status (status),
    INDEX idx_tasks_due_date (due_date)
) ENGINE=InnoDB;

-- ------------------------------------------------------------------------------
-- 5. Notifications Table
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(20) DEFAULT 'info',
    is_read BOOLEAN DEFAULT FALSE,
    action_link VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notifications_user_read (user_id, is_read)
) ENGINE=InnoDB;

-- ==============================================================================
-- Sample Initial Data Seed (Optional for fresh setup)
-- ==============================================================================

-- Seed Users
INSERT INTO users (id, username, full_name, email, password, role, department, avatar_initials, is_active, admin_level, is_superuser, budget_approval_limit, certification_level, technical_specialization, sprint_velocity)
VALUES 
    (1, 'alex.morgan', 'Alex Morgan', 'alex.morgan@company.com', 'password123', 'PROJECT_MANAGER', 'Engineering', 'AM', TRUE, 'SYSTEM_ADMIN', FALSE, 120000.00, 'PMP Certified', 'Software Engineering', 30),
    (2, 'sarah.chen', 'Sarah Chen', 'sarah.chen@company.com', 'password123', 'ADMIN', 'DevOps & Cloud', 'SC', TRUE, 'SUPER_ADMIN', TRUE, 0.00, 'PMP', 'Cloud Security', 30),
    (3, 'david.kim', 'David Kim', 'david.kim@company.com', 'password123', 'TEAM_MEMBER', 'Frontend', 'DK', TRUE, 'SYSTEM_ADMIN', FALSE, 0.00, 'PMP', 'React & Design Tokens', 45),
    (4, 'elena.rostova', 'Elena Rostova', 'elena.rostova@company.com', 'password123', 'TEAM_MEMBER', 'Backend & Core', 'ER', TRUE, 'SYSTEM_ADMIN', FALSE, 0.00, 'PMP', 'Java & Distributed Systems', 55),
    (5, 'marcus.vance', 'Marcus Vance', 'marcus.vance@company.com', 'password123', 'TEAM_MEMBER', 'UI/UX Design', 'MV', TRUE, 'SYSTEM_ADMIN', FALSE, 0.00, 'PMP', 'User Experience Architecture', 35),
    (6, 'priya.sharma', 'Priya Sharma', 'priya.sharma@company.com', 'password123', 'TEAM_MEMBER', 'QA & Testing', 'PS', TRUE, 'SYSTEM_ADMIN', FALSE, 0.00, 'PMP', 'Automated QA & Performance', 40)
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

-- Seed Projects
INSERT INTO projects (id, code, name, description, status, priority, manager_id, start_date, end_date, budget, spent_amount, progress_percentage, total_tasks, completed_tasks)
VALUES
    (1, 'PRJ-101', 'Cloud Infrastructure Modernization', 'Migrate legacy on-prem services to microservices on AWS with Kubernetes orchestration.', 'IN_PROGRESS', 'URGENT', 1, CURDATE() - INTERVAL 30 DAY, CURDATE() + INTERVAL 45 DAY, 85000.00, 52000.00, 68, 24, 16),
    (2, 'PRJ-102', 'NextGen Mobile Banking App', 'Redesign and launch cross-platform mobile client with biometric security and real-time alerts.', 'IN_PROGRESS', 'HIGH', 1, CURDATE() - INTERVAL 15 DAY, CURDATE() + INTERVAL 60 DAY, 120000.00, 48000.00, 42, 38, 16),
    (3, 'PRJ-103', 'Enterprise Business Analytics', 'Develop high-performance reporting dashboards with real-time streaming data ingestion.', 'PLANNING', 'MEDIUM', 1, CURDATE() + INTERVAL 5 DAY, CURDATE() + INTERVAL 90 DAY, 65000.00, 5000.00, 15, 18, 2),
    (4, 'PRJ-104', 'ISO-27001 Security Audit & Remediation', 'Comprehensive audit across all production APIs, penetration testing, and compliance documentation.', 'COMPLETED', 'HIGH', 1, CURDATE() - INTERVAL 75 DAY, CURDATE() - INTERVAL 5 DAY, 45000.00, 43500.00, 100, 14, 14),
    (5, 'PRJ-105', 'Customer Support Portal 2.0', 'Replatform the self-service ticket resolution portal with automated AI response workflows.', 'ON_HOLD', 'LOW', 1, CURDATE() - INTERVAL 40 DAY, CURDATE() + INTERVAL 20 DAY, 30000.00, 18000.00, 55, 12, 6)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Seed Team Members
INSERT INTO team_members (project_id, user_id)
VALUES
    (1, 3), (1, 4), (1, 5),
    (2, 5), (2, 3), (2, 6),
    (3, 4), (3, 6),
    (4, 3), (4, 4),
    (5, 5)
ON DUPLICATE KEY UPDATE assigned_at = CURRENT_TIMESTAMP;

-- Seed Tasks
INSERT INTO tasks (id, project_id, title, description, status, priority, assignee_id, reporter_id, due_date, estimated_hours, logged_hours)
VALUES
    (1, 1, 'Configure Kubernetes EKS Cluster & Ingress Controllers', 'Provision managed cluster with worker node groups and Istio service mesh.', 'IN_PROGRESS', 'URGENT', 4, 1, CURDATE() + INTERVAL 1 DAY, 40, 28),
    (2, 1, 'Database Disaster Recovery & Cross-Region Backup Drill', 'Implement automated WAL archiving and RDS Multi-AZ failover testing.', 'IN_PROGRESS', 'HIGH', 4, 1, CURDATE() - INTERVAL 1 DAY, 32, 12),
    (3, 2, 'Biometric Authentication & TouchID Integration', 'Implement FIDO2 passkey and biometric fallback routines for iOS/Android.', 'REVIEW', 'HIGH', 3, 1, CURDATE() + INTERVAL 2 DAY, 24, 22),
    (4, 2, 'Design System Component Tokens & Figma Alignment', 'Publish standardized token variables and cross-platform CSS properties.', 'DONE', 'MEDIUM', 5, 1, CURDATE() - INTERVAL 5 DAY, 16, 16),
    (5, 3, 'Data Warehouse Schema Design for Fact/Dimension Tables', 'Design star schema supporting high-throughput OLAP queries.', 'TODO', 'MEDIUM', 4, 1, CURDATE() + INTERVAL 14 DAY, 36, 0),
    (6, 4, 'Penetration Testing & Remediation of API Gateway Endpoints', 'Execute OWASP Top 10 penetration suite and apply rate limits.', 'DONE', 'HIGH', 4, 2, CURDATE() - INTERVAL 10 DAY, 28, 28),
    (7, 5, 'Customer Ticket Auto-Triage ML Model Pipeline', 'Build and deploy sentiment and categorization classifiers.', 'BLOCKED', 'LOW', 4, 1, CURDATE() + INTERVAL 21 DAY, 50, 8)
ON DUPLICATE KEY UPDATE title = VALUES(title);

-- Seed Notifications
INSERT INTO notifications (id, user_id, title, message, type, is_read, action_link)
VALUES
    (1, 1, 'Welcome to Project Management Tool', 'System initialized with demo project portfolio and sprint metrics.', 'info', FALSE, 'projects'),
    (2, 1, 'Sprint Review Scheduled', 'Sprint review meeting scheduled for Cloud Infrastructure project.', 'info', TRUE, 'projects?action=view&id=1'),
    (3, 4, 'Urgent Task Assigned', 'You have been assigned to: Configure Kubernetes EKS Cluster & Ingress Controllers.', 'warning', FALSE, 'tasks?action=view&id=1'),
    (4, 4, 'Overdue Alert', 'Task Database Disaster Recovery Drill is past its due date.', 'danger', FALSE, 'tasks?action=view&id=2')
ON DUPLICATE KEY UPDATE title = VALUES(title);
