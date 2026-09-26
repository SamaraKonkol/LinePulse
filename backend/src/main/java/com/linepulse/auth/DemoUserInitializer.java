package com.linepulse.auth;

import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "app.demo-users", name = "enabled", havingValue = "true")
public class DemoUserInitializer implements ApplicationRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;

    public DemoUserInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.demo-users.password}") String demoPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        synchronizeDemoUser("Administrador Demo", "admin@linepulse.local", UserRole.ADMIN);
        synchronizeDemoUser("Técnico Demo", "technician@linepulse.local", UserRole.TECHNICIAN);
        synchronizeDemoUser("Operador Demo", "operator@linepulse.local", UserRole.OPERATOR);
    }

    private void synchronizeDemoUser(String name, String email, UserRole role) {
        Instant now = Instant.now();
        String passwordHash = passwordEncoder.encode(demoPassword);

        userRepository.findByEmailIgnoreCase(email).ifPresentOrElse(
                existing -> {
                    existing.synchronizeDemoProfile(name, passwordHash, role, now);
                    userRepository.save(existing);
                },
                () -> userRepository.save(new UserAccount(
                        UUID.randomUUID(),
                        name,
                        email,
                        passwordHash,
                        role,
                        true,
                        now,
                        now
                ))
        );
    }
}
