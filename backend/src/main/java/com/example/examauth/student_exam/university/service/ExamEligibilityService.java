package com.example.examauth.student_exam.university.service;

import com.example.examauth.student_exam.dto.UploadResponseDTO;
import com.example.examauth.student_exam.dto.EligibilityCheckResponseDTO;
import com.example.examauth.student_exam.university.model.ExamEligibleStudent;
import com.example.examauth.student_exam.university.repo.ExamEligibleStudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for handling exam eligibility Excel uploads
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExamEligibilityService {

    private final ExamEligibleStudentRepository repository;

    /**
     * Process uploaded Excel file and insert eligible students into database
     * 
     * @param file         Uploaded Excel file
     * @param universityId University ID from authenticated admin
     * @return UploadResponseDTO with statistics
     * @throws IOException if file cannot be read
     */
    public UploadResponseDTO processExcelFile(MultipartFile file, Integer universityId) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            return new UploadResponseDTO(0, 0, 0, false, "File is empty");
        }

        // Validate file extension
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".xlsx")) {
            return new UploadResponseDTO(0, 0, 0, false, "Only .xlsx files are supported");
        }

        // Parse Excel file
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);

        // Statistics
        int totalRecords = 0;
        int insertedRecords = 0;
        int skippedDuplicates = 0;

        // Get header row to find column indices
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            workbook.close();
            return new UploadResponseDTO(0, 0, 0, false, "Excel file has no header row");
        }

        // Parse all headers - force STRING type, trim, and convert to lowercase
        Set<String> headerSet = new HashSet<>();
        Map<String, Integer> headerIndexMap = new HashMap<>();

        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                // Force cell to STRING type to handle formatting issues
                cell.setCellType(CellType.STRING);
                String headerValue = cell.getStringCellValue();

                if (headerValue != null && !headerValue.trim().isEmpty()) {
                    // Trim and convert to lowercase for case-insensitive matching
                    String normalizedHeader = headerValue.trim().toLowerCase();
                    headerSet.add(normalizedHeader);
                    headerIndexMap.put(normalizedHeader, i);
                }
            }
        }

        // Validate required columns exist
        List<String> requiredColumns = Arrays.asList("prn_number", "full_name", "exam_session");
        List<String> missingColumns = new ArrayList<>();

        for (String required : requiredColumns) {
            if (!headerSet.contains(required)) {
                missingColumns.add(required);
            }
        }

        if (!missingColumns.isEmpty()) {
            workbook.close();
            return new UploadResponseDTO(0, 0, 0, false,
                    "Missing required columns: " + String.join(", ", missingColumns));
        }

        // Map column names to indices using normalized headers
        int prnNumberCol = headerIndexMap.get("prn_number");
        int fullNameCol = headerIndexMap.get("full_name");
        int examSessionCol = headerIndexMap.get("exam_session");
        int semesterCol = headerIndexMap.getOrDefault("semester", -1);
        int eligibleSubjectsCol = headerIndexMap.getOrDefault("eligible_subjects", -1);
        int feeStatusCol = headerIndexMap.getOrDefault("fee_status", -1);
        int eligibilityStatusCol = headerIndexMap.getOrDefault("eligibility_status", -1);

        // Process each row (skip header)
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null)
                continue;

            totalRecords++;

            try {
                // Extract data from row
                String prnNumber = getCellValueAsString(row.getCell(prnNumberCol));
                String fullName = getCellValueAsString(row.getCell(fullNameCol));
                String semester = semesterCol != -1 ? getCellValueAsString(row.getCell(semesterCol)) : null;
                String examSession = getCellValueAsString(row.getCell(examSessionCol));
                String eligibleSubjectsStr = eligibleSubjectsCol != -1
                        ? getCellValueAsString(row.getCell(eligibleSubjectsCol))
                        : "";
                String feeStatus = feeStatusCol != -1 ? getCellValueAsString(row.getCell(feeStatusCol)) : "Pending";
                String eligibilityStatus = eligibilityStatusCol != -1
                        ? getCellValueAsString(row.getCell(eligibilityStatusCol))
                        : "Eligible";

                // Validate mandatory fields
                if (prnNumber == null || prnNumber.trim().isEmpty() ||
                        fullName == null || fullName.trim().isEmpty() ||
                        examSession == null || examSession.trim().isEmpty()) {
                    log.warn("Row {} skipped: Missing required fields", i + 1);
                    skippedDuplicates++;
                    continue;
                }

                // Check for duplicate
                if (repository.existsByPrnNumberAndExamSession(prnNumber.trim(), examSession.trim())) {
                    log.info("Row {} skipped: Duplicate entry for PRN {} and session {}",
                            i + 1, prnNumber, examSession);
                    skippedDuplicates++;
                    continue;
                }

                // Convert comma-separated subjects to List<String>
                List<String> eligibleSubjects = parseEligibleSubjects(eligibleSubjectsStr);

                // Create entity
                ExamEligibleStudent student = new ExamEligibleStudent();
                student.setUniversityId(universityId != null ? universityId : 1); // Default to 1 if not provided
                student.setPrnNumber(prnNumber.trim());
                student.setFullName(fullName.trim());
                student.setSemester(semester != null ? semester.trim() : null);
                student.setExamSession(examSession.trim());
                student.setEligibleSubjects(eligibleSubjects); // This automatically converts to JSON
                student.setFeeStatus(feeStatus != null && !feeStatus.trim().isEmpty() ? feeStatus.trim() : "Pending");
                student.setEligibilityStatus(
                        eligibilityStatus != null && !eligibilityStatus.trim().isEmpty() ? eligibilityStatus.trim()
                                : "Eligible");

                // Save to database
                repository.save(student);
                insertedRecords++;

            } catch (Exception e) {
                log.error("Error processing row {}: {}", i + 1, e.getMessage());
                skippedDuplicates++;
            }
        }

        workbook.close();

        // Return response
        String message = String.format("Excel file processed successfully. Total: %d, Inserted: %d, Skipped: %d",
                totalRecords, insertedRecords, skippedDuplicates);

        return new UploadResponseDTO(totalRecords, insertedRecords, skippedDuplicates, true, message);
    }

    /**
     * Find column index by header name (case-insensitive)
     */
    private int findColumnIndex(Row headerRow, String columnName) {
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String headerValue = getCellValueAsString(cell);
                if (headerValue != null && headerValue.trim().equalsIgnoreCase(columnName)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Get cell value as String regardless of cell type
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null)
            return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // Convert numeric to string without decimal point for integers
                    double numValue = cell.getNumericCellValue();
                    if (numValue == (long) numValue) {
                        return String.valueOf((long) numValue);
                    } else {
                        return String.valueOf(numValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return null;
        }
    }

    /**
     * Parse comma-separated eligible subjects into List<String>
     */
    private List<String> parseEligibleSubjects(String eligibleSubjectsStr) {
        if (eligibleSubjectsStr == null || eligibleSubjectsStr.trim().isEmpty()) {
            return new ArrayList<>();
        }

        return Arrays.stream(eligibleSubjectsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Check if student is eligible for exam session
     * 
     * @param prnNumber   PRN number of the student
     * @param examSession Exam session to check eligibility for
     * @return EligibilityCheckResponseDTO with eligibility status
     */
    public EligibilityCheckResponseDTO checkEligibility(
            String prnNumber, String examSession) {

        if (prnNumber == null || prnNumber.trim().isEmpty() ||
                examSession == null || examSession.trim().isEmpty()) {
            return new EligibilityCheckResponseDTO(
                    false, "Invalid PRN number or exam session");
        }

        boolean exists = repository.existsByPrnNumberAndExamSession(
                prnNumber.trim(), examSession.trim());

        if (exists) {
            return new EligibilityCheckResponseDTO(
                    true, "Student is eligible for this exam session");
        } else {
            return new EligibilityCheckResponseDTO(
                    false, "Student is not eligible for this exam session");
        }
    }
}
