package com.example.examauth.student_exam.service;

import com.example.examauth.model.User;
import com.example.examauth.repo.UserRepository;
import com.example.examauth.service.QrService;
import com.example.examauth.student_exam.dto.AdmitCardDTO;
import com.example.examauth.student_exam.model.ExamRegistration;
import com.example.examauth.student_exam.repo.ExamRegistrationRepository;
import com.example.examauth.student_exam.university.model.UniversityExam;
import com.example.examauth.student_exam.university.model.UniversityExamSubject;
import com.example.examauth.student_exam.university.repo.UniversityExamRepository;
import com.example.examauth.student_profile.model.StudentProfile;
import com.example.examauth.student_profile.repo.StudentProfileRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdmitCardService {

    private final ExamRegistrationRepository examRegistrationRepository;
    private final UniversityExamRepository universityExamRepository;
    private final UserRepository userRepository;
    private final QrService qrService;
    private final StudentProfileRepository studentProfileRepository;

    public AdmitCardDTO getAdmitCard(Long registrationId) {
        System.out.println("Fetching admit card for regId: " + registrationId);

        ExamRegistration registration = examRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));

        if (registration.getRegistrationStatus() != ExamRegistration.RegistrationStatus.APPROVED || !Boolean.TRUE.equals(registration.getHallTicketReleased())) {
             throw new RuntimeException("Hall Ticket not available");
        }

        User student = userRepository.findById(registration.getStudentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        UniversityExam exam = universityExamRepository.findById(registration.getExamId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));

        String seatNumber = buildSeatNumber(registration);
        if (registration.getQrCode() == null) {
            String qrData = "REG:" + registration.getId()
                          + "|PRN:" + registration.getPrn()
                          + "|EXAM:" + exam.getId();

            String qrImage = generateQrPngBase64(qrData);

            registration.setQrCode(qrImage);
            examRegistrationRepository.save(registration);
        }

        AdmitCardDTO dto = new AdmitCardDTO();
        dto.setStudentName(student.getName());
        dto.setSeatNumber(seatNumber);
        dto.setPrn(registration.getPrn());
        dto.setCourse(firstNonBlank(registration.getCourse(), student.getMajor(), exam.getCourse(), "N/A"));
        dto.setSemester(firstNonBlank(student.getSemester(), exam.getSemester(), "N/A"));
        dto.setExamName(firstNonBlank(exam.getSessionName(), exam.getExamName(), registration.getExamSession(), "N/A"));
        dto.setCenterName(buildCenterName(exam));
        dto.setSubjects(buildSubjects(exam, registration));
        dto.setQrCode(registration.getQrCode());
        return dto;
    }

    private List<AdmitCardDTO.SubjectScheduleDTO> buildSubjects(UniversityExam exam, ExamRegistration registration) {
        List<AdmitCardDTO.SubjectScheduleDTO> list = new ArrayList<>();
        String date = exam.getSchedule() != null && exam.getSchedule().getExamDate() != null
                ? exam.getSchedule().getExamDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "TBD";
        String time = exam.getSchedule() != null && exam.getSchedule().getStartTime() != null
                ? exam.getSchedule().getStartTime() + " - "
                        + (exam.getSchedule().getEndTime() != null ? exam.getSchedule().getEndTime() : "TBD")
                : firstNonBlank(exam.getReportingTime(), "TBD");

        List<String> selected = registration.getSelectedSubjects() != null ? registration.getSelectedSubjects()
                : List.of();
        List<UniversityExamSubject> examSubjects = exam.getSubjects() != null ? exam.getSubjects() : List.of();

        if (!selected.isEmpty()) {
            for (String subjectName : selected) {
                list.add(new AdmitCardDTO.SubjectScheduleDTO(subjectName, date, time));
            }
            return list;
        }

        for (UniversityExamSubject subject : examSubjects) {
            list.add(new AdmitCardDTO.SubjectScheduleDTO(subject.getSubjectName(), date, time));
        }

        if (list.isEmpty()) {
            list.add(
                    new AdmitCardDTO.SubjectScheduleDTO(firstNonBlank(exam.getExamName(), "Exam Subject"), date, time));
        }
        return list;
    }

    private String buildCenterName(UniversityExam exam) {
        String code = firstNonBlank(exam.getCenterCode(), "N/A");
        String name = firstNonBlank(exam.getCenterName(), "Center Not Assigned");
        return code + " - " + name;
    }

    private String buildSeatNumber(ExamRegistration registration) {
        return "S" + String.format("%08d", registration.getId());
    }

    /** PNG image as raw Base64 (no data-URL prefix) for JSON clients. */
    private String generateQrPngBase64(String payload) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(payload, BarcodeFormat.QR_CODE, 220, 220);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (WriterException | IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate QR code");
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
