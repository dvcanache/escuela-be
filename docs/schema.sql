-- Database Schema for Educational Management Platform
-- Version: 1.0
-- Architecture: Multi-Tenant (Row-Level Isolation)

CREATE DATABASE IF NOT EXISTS escuela_db;
USE escuela_db;

-- 1. Tenant Module: Institutional configuration
CREATE TABLE IF NOT EXISTS tenants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    domain_prefix VARCHAR(100) UNIQUE NOT NULL, -- e.g., 'colegio-norte'
    logo_url VARCHAR(2048),
    primary_color VARCHAR(7), -- Hex code for branding
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Security Module: Roles and Users
CREATE TABLE IF NOT EXISTS roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL -- ADMIN, PROFESOR, ESTUDIANTE, PADRE
);

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role_id INT NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES roles(id),
    UNIQUE KEY uk_user_tenant_username (tenant_id, username),
    UNIQUE KEY uk_user_tenant_email (tenant_id, email)
);

-- Relationship for Parents and Students
CREATE TABLE IF NOT EXISTS parent_student_relationship (
    parent_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    PRIMARY KEY (parent_id, student_id),
    CONSTRAINT fk_parent FOREIGN KEY (parent_id) REFERENCES users(id),
    CONSTRAINT fk_student FOREIGN KEY (student_id) REFERENCES users(id)
);

-- 3. Schedule Module: Academic sessions
CREATE TABLE IF NOT EXISTS schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    professor_id BIGINT NOT NULL,
    subject_name VARCHAR(150) NOT NULL,
    classroom VARCHAR(50),
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status ENUM('ACTIVE', 'CANCELLED', 'FREE_HOUR') DEFAULT 'ACTIVE',
    CONSTRAINT fk_schedule_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_schedule_professor FOREIGN KEY (professor_id) REFERENCES users(id)
);

-- 4. Attendance Module: Daily tracking
CREATE TABLE IF NOT EXISTS attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    schedule_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    presence_status ENUM('PRESENTE', 'AUSENTE', 'RETARDO', 'JUSTIFICADO') NOT NULL,
    teacher_notes TEXT,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attendance_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_attendance_schedule FOREIGN KEY (schedule_id) REFERENCES schedules(id),
    CONSTRAINT fk_attendance_student FOREIGN KEY (student_id) REFERENCES users(id)
);

-- 5. Access Control Module: Physical hardware logs
CREATE TABLE IF NOT EXISTS access_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    device_id VARCHAR(100) NOT NULL, -- Serial or ID of the turnstile
    user_id BIGINT NOT NULL,
    direction ENUM('ENTRADA', 'SALIDA') NOT NULL,
    event_timestamp DATETIME NOT NULL,
    CONSTRAINT fk_access_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_access_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Seed Roles
INSERT IGNORE INTO roles (name) VALUES ('ADMIN'), ('PROFESOR'), ('ESTUDIANTE'), ('PADRE');

-- Seed Tenants
INSERT IGNORE INTO tenants (id, name, domain_prefix, logo_url, primary_color) VALUES 
(1, 'Colegio del Norte', 'colegio-norte', 'http://logo.com/norte.png', '#FF0000'),
(2, 'Instituto del Sur', 'instituto-sur', 'http://logo.com/sur.png', '#0000FF');

-- Seed Users
-- Passwords are 'admin123' hashed with Argon2
INSERT IGNORE INTO users (id, tenant_id, username, email, password_hash, role_id, first_name, last_name) VALUES
(1, 1, 'admin_norte', 'admin@norte.edu', '$argon2id$v=19$m=16384,t=2,p=1$7v/T4ogKkhBy3qJqJkDIKw$1g1KyFaqCs6JFqaJrMMPR4LuqnoE6mJ0+4K5JD4d6gY', 1, 'Admin', 'Norte'),
(2, 2, 'admin_sur', 'admin@sur.edu', '$argon2id$v=19$m=16384,t=2,p=1$7v/T4ogKkhBy3qJqJkDIKw$1g1KyFaqCs6JFqaJrMMPR4LuqnoE6mJ0+4K5JFqaJrMMPR4LuqnoE6mJ0+4K5JD4d6gY', 1, 'Admin', 'Sur'),
(3, 1, 'estudiante_1', 'est@norte.edu', 'hash_pass', 3, 'Juan', 'Perez');

-- Indexes for performance
CREATE INDEX idx_user_tenant ON users(tenant_id);
CREATE INDEX idx_schedule_time ON schedules(start_time, end_time);
CREATE INDEX idx_attendance_student ON attendance(student_id);
CREATE INDEX idx_access_timestamp ON access_logs(event_timestamp);
