package com.example.examauth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AlertNotificationService {

    @Autowired
    private JavaMailSender mailSender;

    @Async
    public void sendSupervisorCredentials(String email, String name, String password,
                                          String college, String university) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Welcome to ExamHub — Your Supervisor Account is Ready");

        String body = "Dear " + name + ",\n\n"
                + "We are delighted to inform you that your Supervisor account has been successfully created on the ExamHub platform.\n\n"
                + "━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
                + "         YOUR ACCOUNT DETAILS\n"
                + "━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
                + "Email       : " + email + "\n"
                + "Password    : " + password + "\n"
                + "College     : " + (college != null ? college : "—") + "\n"
                + "University  : " + (university != null ? university : "—") + "\n"
                + "━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n"
                + "IMPORTANT: This is a temporary password generated for your account.\n"
                + "You will be required to change your password on your very first login.\n\n"
                + "Please keep your credentials safe and do not share them with anyone.\n\n"
                + "If you have any questions, please contact your University Administration.\n\n"
                + "━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
                + "Warm Regards,\n"
                + "The ExamHub Team\n"
                + "Empowering Secure Examinations\n"
                + "━━━━━━━━━━━━━━━━━━━━━━━━━━━";

        message.setText(body);

        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + email + ": " + e.getMessage());
        }
    }

    // Backward-compatible overload (used by CSV import path)
    @Async
    public void sendSupervisorCredentials(String email, String name, String password) {
        sendSupervisorCredentials(email, name, password, null, null);
    }

    @Async
    public void sendStudentWelcomeEmail(String email, String name, String university, String college, String password) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Student Enrollment Account Created");

        String body = "Dear " + name + ",\n\n"
                + "Your student account has been created successfully.\n\n"
                + "University: " + (university != null ? university : "University") + "\n"
                + "College: " + (college != null ? college : "College") + "\n\n"
                + "Login Details:\n"
                + "Email: " + email + "\n"
                + "Password: Your PRN number (For security reasons, your initial password is set to your PRN)\n\n"
                + "Please login and change your password immediately as your first priority.\n\n"
                + "Regards,\n"
                + "Examhub Team";

        message.setText(body);

        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send welcome email to student " + email + ": " + e.getMessage());
        }
    }

    @Async
    public void sendAccessRemovedEmail(String email, String name, String role, String university) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("ExamHub — Account Access Removed");

        String roleLabel = (role != null && role.toUpperCase().contains("SUPERVISOR")) ? "Supervisor" : "Student";
        String body = "Dear " + name + ",\n\n"
                + "We are writing to inform you that your " + roleLabel + " account has been removed from the ExamHub platform"
                + (university != null && !university.isEmpty() ? " by " + university + "." : ".") + "\n\n"
                + "As a result of this action:\n"
                + "  - Your login access has been permanently revoked.\n"
                + "  - All associated data and records have been removed from the system.\n\n"
                + "If you believe this was done in error, please contact your university administration directly.\n\n"
                + "Regards,\n"
                + "Examhub Team";

        message.setText(body);

        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send access-removal email to " + email + ": " + e.getMessage());
        }
    }
}
