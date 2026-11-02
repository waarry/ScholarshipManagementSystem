CREATE DATABASE IF NOT EXISTS scholarship_db;
USE scholarship_db;

CREATE TABLE IF NOT EXISTS users (
                                     user_id       INT AUTO_INCREMENT PRIMARY KEY,
                                     full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          ENUM('ADMIN','STUDENT') NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS student_profiles (
                                                student_id      INT PRIMARY KEY,
                                                phone           VARCHAR(20),
    date_of_birth   DATE,
    course          VARCHAR(80),
    education_level VARCHAR(50),
    percentage      DECIMAL(5,2),
    family_income   DECIMAL(12,2),
    address         VARCHAR(255),
    FOREIGN KEY (student_id) REFERENCES users(user_id)
    );

CREATE TABLE IF NOT EXISTS scholarships (
                                            scholarship_id  INT AUTO_INCREMENT PRIMARY KEY,
                                            name            VARCHAR(150) NOT NULL,
    description     TEXT,
    sponsor         VARCHAR(150),
    eligibility     TEXT,
    min_percentage  DECIMAL(5,2),
    income_limit    DECIMAL(12,2),
    eligible_course VARCHAR(80),
    education_level VARCHAR(50),
    amount          DECIMAL(12,2) NOT NULL,
    open_date       DATE NOT NULL,
    close_date      DATE NOT NULL,
    status          ENUM('DRAFT','PUBLISHED','INACTIVE') NOT NULL DEFAULT 'DRAFT',
    created_by      INT,
    FOREIGN KEY (created_by) REFERENCES users(user_id)
    );

CREATE TABLE IF NOT EXISTS required_documents (
                                                  doc_req_id     INT AUTO_INCREMENT PRIMARY KEY,
                                                  scholarship_id INT NOT NULL,
                                                  document_name  VARCHAR(100) NOT NULL,
    FOREIGN KEY (scholarship_id) REFERENCES scholarships(scholarship_id)
    );

CREATE TABLE IF NOT EXISTS applications (
                                            application_id  INT AUTO_INCREMENT PRIMARY KEY,
                                            student_id      INT NOT NULL,
                                            scholarship_id  INT NOT NULL,
                                            status ENUM('DRAFT','SUBMITTED','UNDER_REVIEW','DOCUMENTS_REQUESTED','APPROVED','REJECTED') NOT NULL DEFAULT 'DRAFT',
    statement       TEXT,
    decision_reason TEXT,
    submitted_at    TIMESTAMP NULL,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE (student_id, scholarship_id),
    FOREIGN KEY (student_id) REFERENCES users(user_id),
    FOREIGN KEY (scholarship_id) REFERENCES scholarships(scholarship_id)
    );

CREATE TABLE IF NOT EXISTS application_documents (
                                                     document_id    INT AUTO_INCREMENT PRIMARY KEY,
                                                     application_id INT NOT NULL,
                                                     document_name  VARCHAR(100) NOT NULL,
    file_path      VARCHAR(255) NOT NULL,
    uploaded_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (application_id) REFERENCES applications(application_id)
    );

CREATE TABLE IF NOT EXISTS status_history (
                                              history_id     INT AUTO_INCREMENT PRIMARY KEY,
                                              application_id INT NOT NULL,
                                              old_status     VARCHAR(30),
    new_status     VARCHAR(30) NOT NULL,
    note           TEXT,
    changed_by     INT NOT NULL,
    changed_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (application_id) REFERENCES applications(application_id),
    FOREIGN KEY (changed_by) REFERENCES users(user_id)
    );