# Registration Debugging Guide

## ✅ Fixes Applied

Added comprehensive debugging and error handling to the student registration flow:

1. **Console Logging** - All API calls now log to browser console
2. **Exam Session Handling** - ExamSession is now properly passed from eligibility check to registration form
3. **Better Error Messages** - Detailed error messages showing exactly what went wrong
4. **Token Validation** - Checks for authentication token before every API call
5. **Loading States** - Submit button shows loading spinner during API calls

---

## 🔍 How to Debug Registration Issues

### Step 1: Open Browser Console
Press `F12` or `Ctrl+Shift+I` (Windows) / `Cmd+Option+I` (Mac)

### Step 2: Click "Register Now" and Watch Console

You'll see detailed logs like:
```
[REGISTRATION] Starting registration for exam ID: 1
[REGISTRATION] Exam details: {id: 1, title: "Advanced Java Programming", ...}
[REGISTRATION] Token found: Yes (length: 245)
[REGISTRATION] PRN: REG2022001
[REGISTRATION] Exam Session: Semester End 2026
[REGISTRATION] Calling eligibility API: http://localhost:8081/api/student/check-exam-eligibility?prnNumber=REG2022001&examSession=Semester%20End%202026
[REGISTRATION] Eligibility API response status: 200
[REGISTRATION] Eligibility response: {eligible: true, message: "..."}
[REGISTRATION] Student is eligible - showing registration form
```

### Step 3: Submit the Form

More logs will appear:
```
[FORM] Showing registration form for exam: Advanced Java Programming
[FORM] Exam session: Semester End 2026
[SUBMIT] Form submission started
[SUBMIT] Exam ID: 1
[SUBMIT] Exam Session: Semester End 2026
[SUBMIT] Form data to send: {examId: 1, prn: "REG2022001", fullName: "John Student", ...}
[SUBMIT] Token found: Yes
[SUBMIT] Calling registration API: http://localhost:8081/api/student/registrations
[SUBMIT] Registration API response status: 200
[SUBMIT] Registration successful: {...}
```

---

## ❌ Common Error Scenarios

### Error 1: "No authentication token found"
**Console shows:**
```
[REGISTRATION] Token found: No
```
**Fix:** Student needs to login first. The token is stored in localStorage after login.

### Error 2: "HTTP 403: Forbidden"
**Console shows:**
```
[SUBMIT] Registration forbidden (403)
```
**Reason:** Student is not eligible for this exam session.
**Fix:** University admin needs to upload the student's eligibility via Excel first.

### Error 3: "HTTP 401: Unauthorized"
**Console shows:**
```
[REGISTRATION] Eligibility API response status: 401
```
**Reason:** JWT token expired or invalid.
**Fix:** Student needs to logout and login again.

### Error 4: "Failed to fetch"
**Console shows:**
```
[SUBMIT] Registration error: TypeError: Failed to fetch
```
**Reason:** Backend server is not running or CORS issue.
**Fix:** 
- Start backend: `cd backend && mvn spring-boot:run`
- Check if running on `http://localhost:8081`

### Error 5: "Exam session mismatch"
**Console shows:**
```
[REGISTRATION] Exam Session: Semester End 2026
[REGISTRATION] Eligibility response: {eligible: false, message: "Student is not eligible for this exam session"}
```
**Reason:** The exam session in the database doesn't match the generated session string.
**Fix:** Ensure the Excel upload uses the same format: `"Semester End 2026"` (not `"Winter 2026"` or other variants)

---

## 🔧 Quick Test Checklist

### Before Testing:
- [ ] Backend is running (`http://localhost:8081`)
- [ ] Student is logged in (check localStorage for `jwtToken`)
- [ ] Student exists in `exam_eligible_students` table
- [ ] Exam session in database matches frontend format exactly

### Testing Steps:
1. Open student dashboard
2. Open browser console (F12)
3. Click "Register Now" on any exam
4. Check console logs for eligibility check
5. If eligible, fill form and submit
6. Check console logs for registration submission
7. Verify success message appears

---

## 📡 API Endpoints Being Called

### 1. Eligibility Check
```
GET http://localhost:8081/api/student/check-exam-eligibility
Parameters: 
  - prnNumber: REG2022001
  - examSession: Semester End 2026
Headers:
  - Authorization: Bearer <jwt_token>
```

### 2. Registration Submission
```
POST http://localhost:8081/api/student/registrations
Body:
{
  "examId": 1,
  "prn": "REG2022001",
  "fullName": "John Student",
  "course": "B.Tech Computer Science",
  "year": "Final Year",
  "examSession": "Semester End 2026"
}
Headers:
  - Authorization: Bearer <jwt_token>
  - Content-Type: application/json
```

---

## 🎯 What Was Fixed

1. **Added `examSession` parameter to `showRegistrationForm()`**
   - Now receives examSession from eligibility check
   - Stores it in modal's dataset for submission

2. **Updated `handleRegistrationSubmit()`**
   - Retrieves examSession from stored dataset
   - Uses it in the registration request

3. **Added comprehensive console logging**
   - Every step logs its progress
   - All API calls log URL, headers, and response
   - All errors log detailed information

4. **Better error handling**
   - Specific error messages for 403, 401, network errors
   - Shows user-friendly error modals
   - Submit button shows loading state

5. **Token validation throughout**
   - Checks for token before every API call
   - Shows clear error if token missing

---

## 🐛 Still Not Working?

**Check these in order:**

1. **Backend Console** - Look for errors when API is called
2. **Browser Network Tab** (F12 → Network) - See exact request/response
3. **Database** - Verify student exists in `exam_eligible_students` with correct session
4. **Backend Logs** - Check Spring Boot console for eligibility/registration errors
5. **CORS** - If cross-origin error, backend needs CORS configuration

**Share these details for help:**
- Console logs (copy full output)
- Network tab screenshot (for failed request)
- Backend error logs
- Database query result: `SELECT * FROM exam_eligible_students WHERE prn_number = 'YOUR_PRN'`

---

## ✅ Success Indicators

When working correctly, you'll see:
1. ✅ "Checking Eligibility..." modal appears
2. ✅ Modal closes automatically
3. ✅ Registration form appears with exam details
4. ✅ Form shows "Eligible" badge
5. ✅ Submit button shows "Submitting..." while processing
6. ✅ Success modal appears
7. ✅ Console shows all successful API calls

**No errors in console = Everything is working!**
