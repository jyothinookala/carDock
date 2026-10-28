package com.cardock.config;

import com.cardock.entity.AppUser;
import com.cardock.entity.UserRole;
import com.cardock.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
@ConditionalOnProperty(name = "cardock.admin-bootstrap.enabled", havingValue = "true")
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = System.getenv("CARDOCK_ADMIN_EMAIL");
        String password = System.getenv("CARDOCK_ADMIN_PASSWORD");
        if (email == null || email.isBlank() || password == null || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(
                    "Set CARDOCK_ADMIN_EMAIL and a CARDOCK_ADMIN_PASSWORD of at least 12 characters "
                            + "and no more than 72 UTF-8 bytes.");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException("That email already exists; the bootstrap will not change its role.");
        }

        userRepository.save(new AppUser(
                "CarDock Administrator",
                normalizedEmail,
                passwordEncoder.encode(password),
                UserRole.ADMIN));
    }
}
