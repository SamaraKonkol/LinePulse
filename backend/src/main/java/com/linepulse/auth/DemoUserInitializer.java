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
        createIfMissing("Administrador Demo", "admin@linepulse.local", UserRole.ADMIN);
        createIfMissing("Técnico Demo", "technician@linepulse.local", UserRole.TECHNICIAN);
        createIfMissing("Operador Demo", "operator@linepulse.local", UserRole.OPERATOR);
    }

    private void createIfMissing(String name, String email, UserRole role) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        Instant now = Instant.now();
        userRepository.save(new UserAccount(
                UUID.randomUUID(),
                name,
                email,
                passwordEncoder.encode(demoPassword),
                role,
                true,
                now,
                now
        ));
    }
}
