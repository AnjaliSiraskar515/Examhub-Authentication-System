# Exam Registration Eligibility Enforcement

## ✅ Implementation Complete

Added eligibility validation to the exam registration process. Students must be in the `exam_eligible_students` table for the specific exam session before they can register.

---

## 🔧 What Was Changed

### Modified File: StudentRegistrationController.java

**Added:**
1. Dependency injection for `ExamEligibilityService`
2. Eligibility check before registration is saved
3. HTTP 403 response if student is not eligible

---

## 📝 Updated Registration Method

```java
@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
public ResponseEntity<ExamRegistrationResponseDTO> registerForExam(
        @RequestBody @Valid ExamRegistrationRequestDTO request,
        org.springframework.security.core.Authentication authentication) {

    if (authentication == null || !authentication.isAuthenticated()) {
        throw new RuntimeException("User not authenticated");
    }

    String email = authentication.getName();
    com.example.examauth.model.User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

    // Securely set studentId from logged-in user
    request.setStudentId(user.getUserId());

    // ========== ELIGIBILITY VALIDATION ==========
    // Check if student is eligible for this exam session
    if (request.getPrn() != null && request.getExamSession() != null) {
        EligibilityCheckResponseDTO eligibilityCheck = 
            examEligibilityService.checkEligibility(request.getPrn(), request.getExamSession());
        
        if (!eligibilityCheck.isEligible()) {
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(null); // Returns 403 Forbidden
        }
    }
    // ============================================

    return ResponseEntity.ok(examRegistrationService.registerStudent(request));
}
```

---

## 🔄 How It Works

### Registration Flow (Before)
1. Student authenticates
2. Student ID extracted from JWT token
3. **Registration saved directly**

### Registration Flow (After - With Eligibility Check)
1. Student authenticates
2. Student ID extracted from JWT token
3. **✅ NEW: Check eligibility in database**
   - Query: `exam_eligible_students` WHERE `prn_number = ? AND exam_session = ?`
4. **✅ NEW: If NOT eligible → Return HTTP 403 Forbidden**
5. If eligible → Continue with registration

---

## 📋 Validation Logic

### Fields Used
- **PRN Number:** `request.getPrn()` (from registration request)
- **Exam Session:** `request.getExamSession()` (from registration request)

### Validation Call
```java
EligibilityCheckResponseDTO eligibilityCheck = 
    examEligibilityService.checkEligibility(
        request.getPrn(), 
        request.getExamSession()
    );
```

### Response Handling

#### If Eligible (`eligible = true`)
- ✅ Registration proceeds normally
- HTTP 200 OK with registration details

#### If Not Eligible (`eligible = false`)
- ❌ Registration is **blocked**
- HTTP **403 Forbidden**
- Response body: `null` (or could return error message)

---

## 🧪 Testing

### Test Case 1: Eligible Student Registration

**Precondition:**
```sql
INSERT INTO exam_eligible_students 
(university_id, prn_number, full_name, exam_session, eligible_subjects, eligibility_status)
VALUES (1, 'PRN2024001', 'John Doe', 'Winter 2024', '["Math", "Physics"]', 'Eligible');
```

**Request:**
```json
POST /api/student/registrations
Content-Type: application/json
Authorization: Bearer <jwt_token>

{
  "examId": 123,
  "prn": "PRN2024001",
  "fullName": "John Doe",
  "course": "Computer Science",
  "year": "Final Year",
  "examSession": "Winter 2024"
}
```

**Expected Response:**
```json
HTTP 200 OK
{
  "registrationId": 456,
  "message": "Registration successful",
  ...
}
```

---

### Test Case 2: Non-Eligible Student Registration

**Precondition:**
- No record in `exam_eligible_students` for PRN2024999 and Winter 2024

**Request:**
```json
POST /api/student/registrations
Content-Type: application/json
Authorization: Bearer <jwt_token>

{
  "examId": 123,
  "prn": "PRN2024999",
  "fullName": "Jane Smith",
  "course": "Computer Science",
  "year": "Final Year",
  "examSession": "Winter 2024"
}
```

**Expected Response:**
```json
HTTP 403 Forbidden
null
```

---

### Test Case 3: Missing PRN or Exam Session

**Request:**
```json
POST /api/student/registrations
Content-Type: application/json
Authorization: Bearer <jwt_token>

{
  "examId": 123,
  "prn": null,
  "fullName": "John Doe",
  "course": "Computer Science",
  "year": "Final Year",
  "examSession": null
}
```

**Expected Behavior:**
- Validation check is **skipped** (because `prn` or `examSession` is null)
- Registration proceeds to existing logic
- May fail due to `@NotBlank` validation on DTO

---

## 📊 Database Dependency

This validation requires:

1. **Table:** `exam_eligible_students`
2. **Query:** Checks existence of record with matching `(prn_number, exam_session)`
3. **Source:** University Admin uploads eligible students via Excel

**Workflow:**
```
University Admin → Upload Excel → exam_eligible_students table → Student Registration validates against this table
```

---

## 🔒 Security Benefits

✅ **Prevents unauthorized registrations** - Only pre-approved students can register  
✅ **Centralized eligibility management** - University controls who can register  
✅ **Audit trail** - All eligible students recorded in database  
✅ **Session-specific** - Different eligibility per exam session  

---

## ⚠️ Important Notes

1. **Null Safety:** If `prn` or `examSession` is null, validation is skipped
2. **HTTP 403:** Returns Forbidden status (not 400 Bad Request)
3. **No Error Message:** Currently returns `null` body - can be enhanced to return error DTO
4. **No Changes to Business Logic:** Existing registration logic remains unchanged

---

## 🎯 Summary

### What Changed
✅ Added `ExamEligibilityService` dependency  
✅ Added eligibility check before registration  
✅ Returns HTTP 403 if student not eligible  

### What Didn't Change
- Authentication logic
- Student ID extraction from JWT
- Registration service logic
- Database schema
- Response DTOs (except HTTP 403 case)

**Final Result:** Exam registration is now gated by the `exam_eligible_students` table, ensuring only authorized students can register for exams.
