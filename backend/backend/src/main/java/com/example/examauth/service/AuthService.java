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
    //  REGISTER USER
    // =====================================================
    public User registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus("pending"); // Default status for supervisors/admins
        return userRepository.save(user);
    }

    // =====================================================
    //  FIND USER BY EMAIL
    // =====================================================
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // =====================================================
    //  FIND USER BY USERNAME (for student login)
    // =====================================================
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // =====================================================
    //  AUTHENTICATE (EMAIL OR USERNAME)
    // =====================================================
    public boolean authenticate(String identifier, String password) {
        // First try to find by email
        Optional<User> userOpt = userRepository.findByEmail(identifier);

        // If not found by email, try username
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(identifier);
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return passwordEncoder.matches(password, user.getPassword());
        }

        return false;
    }

    // =====================================================
    //  SAVE USER
    // =====================================================
    public User saveUser(User user) {
        return userRepository.save(user);
    }
}
