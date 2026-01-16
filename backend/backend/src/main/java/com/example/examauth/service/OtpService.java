package com.example.examauth.service;

import com.example.examauth.model.OtpEntity;
import com.example.examauth.repo.OtpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class OtpService {

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private JavaMailSender mailSender;  // ✅ Injects Gmail SMTP sender

    private String generateOtp() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    // =====================================
    // SEND EMAIL OTP
    // =====================================
    public boolean sendEmailOtp(String email) {
        String otp = generateOtp();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

        // Remove any old OTP for same email
        otpRepository.findByEmail(email).ifPresent(otpRepository::delete);
        otpRepository.save(new OtpEntity(email, null, otp, expiry));

        try {
            // ✅ Compose and send email
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setFrom("siraskar2005@gmail.com"); // 👈 Your email (important)
            message.setSubject("ExamHub OTP Verification");
            message.setText(
                "Hello,\n\nYour ExamHub verification OTP is: " + otp +
                "\n\nThis code is valid for 5 minutes.\n" +
                "Do not share it with anyone.\n\n" +
                "- ExamHub Authentication System"
            );

            // ✅ Send email
            mailSender.send(message);

            System.out.println("📧 OTP email sent to: " + email);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Failed to send OTP email to: " + email);
            return false;
        }
    }

    // =====================================
    // SEND PHONE OTP (placeholder for Twilio)
    // =====================================
    public boolean sendPhoneOtp(String phone) {
        String otp = generateOtp();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

        otpRepository.findByPhone(phone).ifPresent(otpRepository::delete);
        otpRepository.save(new OtpEntity(null, phone, otp, expiry));

        // TODO: Add Twilio integration later
        System.out.println("📱 OTP sent to phone (console only): " + phone + " -> " + otp);
        return true;
    }

    // =====================================
    // VERIFY EMAIL OTP
    // =====================================
    public boolean verifyEmailOtp(String email, String otp) {
        Optional<OtpEntity> entity = otpRepository.findByEmail(email);
        if (entity.isEmpty()) return false;

        OtpEntity data = entity.get();
        boolean valid = data.getOtp().equals(otp) && data.getExpiryTime().isAfter(LocalDateTime.now());
        if (valid) otpRepository.delete(data);
        return valid;
    }

    // =====================================
    // VERIFY PHONE OTP
    // =====================================
    public boolean verifyPhoneOtp(String phone, String otp) {
        Optional<OtpEntity> entity = otpRepository.findByPhone(phone);
        if (entity.isEmpty()) return false;

        OtpEntity data = entity.get();
        boolean valid = data.getOtp().equals(otp) && data.getExpiryTime().isAfter(LocalDateTime.now());
        if (valid) otpRepository.delete(data);
        return valid;
    }
}
