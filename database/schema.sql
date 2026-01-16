CREATE DATABASE IF NOT EXISTS exam_authentication_db;
USE exam_authentication_db;
CREATE TABLE users ( user_id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255), email VARCHAR(255) UNIQUE, password VARCHAR(255), role ENUM('student','supervisor','admin','super_admin') DEFAULT 'student', status VARCHAR(50), biometric_hash VARCHAR(2000), photo_path VARCHAR(500), created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP );
CREATE TABLE documents ( doc_id INT AUTO_INCREMENT PRIMARY KEY, user_id INT, doc_type VARCHAR(100), doc_path VARCHAR(500), verified_status ENUM('pending','verified','flagged','rejected') DEFAULT 'pending', confidence FLOAT, ai_extracted_name VARCHAR(255), uploaded_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP );
CREATE TABLE exams ( exam_id INT AUTO_INCREMENT PRIMARY KEY, exam_name VARCHAR(255), institution_name VARCHAR(255), date DATE, start_time TIME, duration_minutes INT, mode ENUM('online','offline'), location VARCHAR(255), status ENUM('upcoming','completed') DEFAULT 'upcoming' );
CREATE TABLE qr_codes ( qr_id INT AUTO_INCREMENT PRIMARY KEY, user_id INT, exam_id INT, token VARCHAR(2000), generated_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP );
CREATE TABLE attendance ( id INT AUTO_INCREMENT PRIMARY KEY, exam_id INT, student_id INT, supervisor_id INT, marked_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP );
CREATE TABLE fraud_logs ( log_id INT AUTO_INCREMENT PRIMARY KEY, user_id INT, exam_id INT, description TEXT, severity ENUM('low','medium','high') DEFAULT 'low', detected_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP, escalated_to_super_admin BOOLEAN DEFAULT FALSE );
CREATE TABLE otp_verifications ( id INT AUTO_INCREMENT PRIMARY KEY, email VARCHAR(255), otp_code VARCHAR(10), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, expires_at TIMESTAMP, verified BOOLEAN DEFAULT FALSE );
