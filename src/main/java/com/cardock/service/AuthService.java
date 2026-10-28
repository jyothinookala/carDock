package com.cardock.service;

import com.cardock.dto.RegisterRequest;
import com.cardock.dto.UserResponse;
import com.cardock.entity.AppUser;
import com.cardock.entity.UserRole;
import com.cardock.exception.ConflictException;
import com.cardock.exception.ResourceNotFoundException;
import com.cardock.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("An account with this email already exists");
        }

        String fullName = request.fullName().trim();
        if (fullName.isEmpty()) {
            throw new IllegalArgumentException("Full name cannot be blank");
        }

        AppUser user = new AppUser(
                fullName,
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                UserRole.CUSTOMER);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        return UserResponse.from(userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    public String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
