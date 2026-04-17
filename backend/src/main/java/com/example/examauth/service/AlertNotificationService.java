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
    public void sendSupervisorCredentials(String email, String name, String password) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Supervisor Account Created");

        String body = "Dear " + name + ",\n\n"
                + "Your supervisor account has been created.\n\n"
                + "Login Details:\n"
                + "Email: " + email + "\n"
                + "Password: " + password + "\n\n"
                + "Please login and change your password immediately.\n\n"
                + "Regards,\n"
                + "University Exam System";

        message.setText(body);

        try {
            mailSender.send(message);
        } catch (Exception e) {
            // Log the error but don't fail the transaction
            System.err.println("Failed to send email to " + email + ": " + e.getMessage());
        }
    }
}
