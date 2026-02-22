package com.example.examauth.service;

import com.example.examauth.model.User;
import com.example.examauth.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // =====================================================
    // REGISTER USER
    // =====================================================
    public User registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus("pending"); // Default status for supervisors/admins
        return userRepository.save(user);
    }

    // =====================================================
    // FIND USER BY EMAIL
    // =====================================================
    public Optional<User> findByEmail(String email) {
        return userRepository.findFirstByEmail(email);
    }

    // =====================================================
    // FIND USER BY PRN (for student login)
    // =====================================================
    public Optional<User> findByPrn(String prn) {
        return userRepository.findByPrn(prn);
    }

    // =====================================================
    // FIND USER BY PHONE
    // =====================================================
    public Optional<User> findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }

    // =====================================================
    // AUTHENTICATE (EMAIL OR USERNAME OR PHONE)
    // =====================================================
    public boolean authenticate(String identifier, String password) {
        // 1. Try Email
        Optional<User> userOpt = userRepository.findFirstByEmail(identifier);

        // 2. Try PRN
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByPrn(identifier);
        }

        // 3. Try Phone
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByPhoneNumber(identifier);
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String dbPassword = user.getPassword();

            // Allow matching of both hashed and legacy plain text passwords
            if (dbPassword != null && dbPassword.startsWith("$2a$")) {
                return passwordEncoder.matches(password, dbPassword);
            } else if (dbPassword != null) {
                return dbPassword.equals(password);
            }
        }

        return false;
    }

    // =====================================================
    // SAVE USER
    // =====================================================
    public User saveUser(User user) {
        return userRepository.save(user);
    }
}
