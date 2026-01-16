package com.example.examauth.service;

import com.example.examauth.model.OtpEntity;
import com.example.examauth.repo.OtpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@Transactional
public class OtpService {

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private JavaMailSender mailSender; // ✅ Injects Gmail SMTP sender

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${twilio.account.sid}")
    private String twilioSid;

    @Value("${twilio.auth.token}")
    private String twilioAuthToken;

    @Value("${twilio.phone.number}")
    private String twilioPhoneNumber;

    @PostConstruct
    public void initTwilio() {
        if (twilioSid != null && !twilioSid.startsWith("ACxxxxx")) {
            Twilio.init(twilioSid, twilioAuthToken);
            System.out.println("✅ Twilio initialized successfully.");
        } else {
            System.out.println("⚠️ Twilio credentials are placeholders. SMS will be simulated.");
        }
    }

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
            message.setFrom(senderEmail); // 👈 Dynamically set from properties
            message.setSubject("ExamHub OTP Verification");
            message.setText(
                    "Hello,\n\nYour ExamHub verification OTP is: " + otp +
                            "\n\nThis code is valid for 5 minutes.\n" +
                            "Do not share it with anyone.\n\n" +
                            "- ExamHub Authentication System");

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

        try {
            if (twilioSid != null && !twilioSid.startsWith("ACxxxxx")) {
                Message.creator(
                        new PhoneNumber(phone),
                        new PhoneNumber(twilioPhoneNumber),
                        "Your ExamHub OTP is: " + otp).create();
                System.out.println("📱 SMS sent via Twilio to: " + phone);
            } else {
                System.out.println("📱 [SIMULATION] SMS to " + phone + ": " + otp);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Failed to send SMS to: " + phone);
            return false;
        }
    }

    // =====================================
    // VERIFY EMAIL OTP
    // =====================================
    public boolean verifyEmailOtp(String email, String otp) {
        Optional<OtpEntity> entity = otpRepository.findByEmail(email);
        if (entity.isEmpty())
            return false;

        OtpEntity data = entity.get();
        boolean valid = data.getOtp().equals(otp) && data.getExpiryTime().isAfter(LocalDateTime.now());
        if (valid)
            otpRepository.delete(data);
        return valid;
    }

    // =====================================
    // VERIFY PHONE OTP
    // =====================================
    public boolean verifyPhoneOtp(String phone, String otp) {
        Optional<OtpEntity> entity = otpRepository.findByPhone(phone);
        if (entity.isEmpty())
            return false;

        OtpEntity data = entity.get();
        boolean valid = data.getOtp().equals(otp) && data.getExpiryTime().isAfter(LocalDateTime.now());
        if (valid)
            otpRepository.delete(data);
        return valid;
    }
}
