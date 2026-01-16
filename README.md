<<<<<<< HEAD
# Exam Authentication System - Advanced Final Package
This package contains a full project skeleton for an exam authentication system using QR, AES, fingerprint and AI OCR verification.
## Contents
- frontend/: UI files (student, supervisor, admin, profile, register)
- backend/: Spring Boot skeleton (controllers, models, util)
- ai_verification/: Flask microservice for OCR & biometric stub
- database/: schema.sql and seed.sql
## Quick start (Windows)
1. Create uploads folder:
   C:\Users\Anjali\OneDrive\Desktop\BE Project\exam_auth_uploads
2. Import DB:
   mysql -u root -p < database/schema.sql
   mysql -u root -p < database/seed.sql
3. Set AES key (base64 32 bytes) as environment variable AES_KEY_B64.
4. Start AI microservice:
   cd ai_verification
   pip install -r requirements.txt
   python app.py
5. Start backend:
   cd backend
   mvn clean package
   mvn spring-boot:run
6. Serve frontend (for camera access use localhost or HTTPS):
   python -m http.server 8000
   Open http://localhost:8000/frontend/supervisor_dashboard.html
## Notes
- Fingerprint capture requires vendor SDK or local agent to capture real templates.
- This package uses simulated fingerprint templates for demo flows ('SAMPLE' string).
- Replace demo logic with vendor matching for production.
=======
# final-year-project
An advanced examination authentication system that ensures secure candidate verification and prevents impersonation using modern web technologies.
>>>>>>> 19403cf0c039a4be702737d667119335922edbf6
