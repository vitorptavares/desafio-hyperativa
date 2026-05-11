package com.hyperativa.desafio.config;

import com.hyperativa.desafio.domain.entity.AppUser;
import com.hyperativa.desafio.domain.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class DefaultUserSeeder implements CommandLineRunner {

    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin123";
    private static final String DEFAULT_ROLE = "ADMIN";

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public DefaultUserSeeder(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (users.findByUsername(DEFAULT_USERNAME).isPresent()) {
            return;
        }
        AppUser admin = AppUser.builder()
                .username(DEFAULT_USERNAME)
                .passwordHash(passwordEncoder.encode(DEFAULT_PASSWORD))
                .role(DEFAULT_ROLE)
                .build();
        users.save(admin);
    }
}
