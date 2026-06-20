# 🎓 Smart Exam Authentication System

![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen.svg)
![MySQL](https://img.shields.io/badge/MySQL-Database-blue.svg)
![Security](https://img.shields.io/badge/Security-AES--256--GCM-red.svg)
![Status](https://img.shields.io/badge/Status-Production%20Ready-success.svg)

A multi-layered examination authentication platform designed to prevent impersonation and ensure secure candidate verification. The system integrates a robust three-factor authentication approach: **Biometric fingerprint matching**, **Dynamic QR code validation**, and **OTP verification**, all managed through role-based access for Students, Supervisors, and Administrators.

---

## 🌟 Key Features

*   **🛡️ Multi-Factor Authentication:** Secures exam hall entry using SHA-256 biometric fingerprint hashing and AES-256 encrypted QR Codes.
*   **👥 Role-Based Access Control (RBAC):** Dedicated dashboards with specific privileges for Students, Exam Supervisors, Institution Admins, and Super Admins.
*   **📱 Secure Hall Tickets:** Automated PDF generation (via OpenPDF) for admit cards embedding time-limited cryptographic QR tokens.
*   **🏫 Automated Seat Allocation:** Algorithmic assignment of exam supervisors to specific classrooms, enforcing cross-departmental invigilation policies to prevent bias.
*   **📊 Bulk Data Management:** Admin functionality to seamlessly import student batches via CSV using Apache POI integrations.
*   **💬 Real-Time Communication:** Integrated alerting system for fraud reporting and exam-hall supply requests.

---

## 🛠️ Technology Stack

### **Backend Architecture**
*   **Framework:** Java 21, Spring Boot 3.2.4
*   **Security:** Spring Security, JWT (HS512), AES-256-GCM Encryption
*   **Core Libraries:** Google ZXing (QR), SourceAFIS (Biometrics), OpenPDF
*   **Architecture:** Layered MVC (Model-View-Controller) with RESTful APIs

### **Frontend & Database**
*   **Frontend:** HTML5, CSS3, Vanilla JavaScript (Fetch API for dynamic rendering)
*   **Database:** MySQL (Relational Schema)
*   **ORM:** Spring Data JPA (Hibernate)

---

## 📂 Project Structure

```text
📦 final-year-project
 ┣ 📂 backend/            # Spring Boot REST API & Business Logic
 ┣ 📂 frontend/           # Vanilla JS UI, Dashboards, and Forms
 ┣ 📂 database/           # MySQL Schema and Seed Scripts
 ┣ 📂 csv/                # Sample CSV templates for bulk upload
 ┗ 📂 documentation_diagrams/ # Sequence and Architecture diagrams
```

---

## 🚀 Setup & Installation (Windows)

### 1. Database Configuration
1. Open MySQL Command Line or Workbench.
2. Create the database and import the schema:
   ```sql
   mysql -u root -p < database/schema.sql
   mysql -u root -p < database/seed.sql
   ```

### 2. Backend Setup (Java)
1. Set the AES secret key as an environment variable (base64 32 bytes):
   `setx AES_KEY_B64 "your_secret_key_here"`
2. Navigate to the backend directory and run the Spring Boot application:
   ```bash
   cd backend
   mvn clean package
   mvn spring-boot:run
   ```
*(The backend will start locally on `http://localhost:8080` or `8081`)*

### 3. Frontend Setup
1. Use any standard HTTP server to serve the static frontend files.
   ```bash
   python -m http.server 8000
   ```
2. Navigate to `http://localhost:8000/frontend/auth_portal.html` to log in.

---

## ⚠️ Notes for Evaluators
*   **Biometrics:** Fingerprint extraction requires a physical vendor SDK (e.g., Morpho/Mantra). For demo purposes, the backend simulates template extraction and performs the raw SHA-256 hash comparison internally.
*   **QR Security:** QR codes are strictly time-limited and uniquely bound to `studentId | examId | timestamp` to prevent forgery.
