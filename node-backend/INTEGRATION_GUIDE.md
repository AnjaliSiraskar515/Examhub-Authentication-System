# Exam Eligibility Upload - Integration Guide

## 📦 Required Dependencies

Add these to your `package.json`:

```bash
npm install multer xlsx
```

## 🔗 How to Integrate

### Step 1: Connect the Route to Your App

In your main `app.js` or `server.js`, add:

```javascript
const examEligibilityRoutes = require('./routes/examEligibilityRoutes');

// Add this route
app.use('/api/university', examEligibilityRoutes);
```

### Step 2: Database Configuration

Make sure you have a `config/db.js` file that exports a promisified MySQL connection:

```javascript
// Example config/db.js
const mysql = require('mysql2/promise');

const pool = mysql.createPool({
    host: process.env.DB_HOST || 'localhost',
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'exam_system',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
});

module.exports = pool;
```

**Update the path in controller if your db config is in a different location:**
```javascript
// In examEligibilityController.js line 2
const db = require('../config/db'); // Adjust path as needed
```

### Step 3: Create the Database Table

Run the SQL script in `database/create_exam_eligible_students_table.sql`:

```sql
mysql -u root -p your_database < database/create_exam_eligible_students_table.sql
```

Or execute it through your MySQL client.

### Step 4: Authentication Middleware (Optional but Recommended)

Uncomment the auth middleware in `routes/examEligibilityRoutes.js`:

```javascript
router.post(
    '/upload-exam-eligibility',
    authMiddleware,           // Uncomment this
    universityAdminOnly,      // Uncomment this
    upload.single('file'),
    examEligibilityController.uploadExamEligibility
);
```

Make sure you have middleware that:
- Validates JWT token
- Extracts user info and attaches to `req.user`
- Checks if user role is 'university_admin'

## 📝 API Usage

### Upload Excel File

**Endpoint:** `POST /api/university/upload-exam-eligibility`

**Headers:**
```
Content-Type: multipart/form-data
Authorization: Bearer <your_jwt_token>
```

**Body (form-data):**
- Key: `file`
- Value: Your Excel file (.xlsx or .xls)

**Excel Format:**

| prn_number | full_name | semester | exam_session | eligible_subjects | fee_status | eligibility_status |
|------------|-----------|----------|--------------|-------------------|------------|--------------------|
| PRN2024001 | John Doe  | Semester 6 | Winter 2024 | Mathematics,Physics,Chemistry | Paid | Eligible |
| PRN2024002 | Jane Smith | Semester 4 | Winter 2024 | Computer Science,Data Structures | Pending | Eligible |

**Response:**
```json
{
    "success": true,
    "message": "Excel file processed successfully",
    "data": {
        "total_records": 100,
        "inserted_records": 95,
        "skipped_duplicates": 5,
        "errors": []
    }
}
```

### Get Eligible Students

**Endpoint:** `GET /api/university/exam-eligibility/:examSession`

**Example:** `GET /api/university/exam-eligibility/Winter%202024`

**Response:**
```json
{
    "success": true,
    "count": 95,
    "data": [
        {
            "id": 1,
            "university_id": 1,
            "prn_number": "PRN2024001",
            "full_name": "John Doe",
            "semester": "Semester 6",
            "exam_session": "Winter 2024",
            "eligible_subjects": ["Mathematics", "Physics", "Chemistry"],
            "fee_status": "Paid",
            "eligibility_status": "Eligible",
            "created_at": "2024-02-12T16:00:00Z"
        }
    ]
}
```

## 🧪 Testing with Postman/cURL

### Using cURL:
```bash
curl -X POST http://localhost:3000/api/university/upload-exam-eligibility \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@/path/to/eligible_students.xlsx"
```

### Using Postman:
1. Set method to `POST`
2. URL: `http://localhost:3000/api/university/upload-exam-eligibility`
3. Go to "Body" tab
4. Select "form-data"
5. Add key `file` (change type to "File")
6. Upload your Excel file
7. Send request

## ⚠️ Important Notes

1. **File Size Limit:** Maximum 5MB (configurable in routes file)
2. **Duplicate Handling:** Duplicates based on `(prn_number + exam_session)` are automatically skipped
3. **University ID:** Currently defaults to `1` or from `req.user.university_id`. Update based on your auth system.
4. **Error Handling:** Rows with errors are logged in response but don't stop the entire process
5. **Database:** Uses `mysql2/promise` for async/await support

## 🔒 Security Recommendations

- ✅ Enable authentication middleware before production
- ✅ Validate university admin role
- ✅ Rate limit the upload endpoint
- ✅ Scan uploaded files for malware
- ✅ Log all upload activities for audit trail
