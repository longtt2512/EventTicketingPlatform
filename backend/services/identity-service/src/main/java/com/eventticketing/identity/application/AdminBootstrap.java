package com.eventticketing.identity.application;

import com.eventticketing.identity.domain.AccountStatus;
import com.eventticketing.identity.domain.UserEntity;
import com.eventticketing.identity.domain.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminBootstrap(UserRepository users, PasswordEncoder passwordEncoder,
                          @Value("${app.bootstrap-admin.email:}") String email,
                          @Value("${app.bootstrap-admin.password:}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (email.isBlank() || password.isBlank() || users.findByEmail(email.toLowerCase()).isPresent()) return;
        UserEntity admin = new UserEntity(email.toLowerCase(), passwordEncoder.encode(password), "Local Admin", "vi");
        admin.verifyEmail();
        admin.addRole("ADMIN");
        users.save(admin);
    }
}
