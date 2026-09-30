package com.linepulse.platform;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "PLATFORM_BOOTSTRAP_ENABLED", havingValue = "true")
public class PlatformBootstrap implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;
    private final PlatformTotp totp;
    private final String email, password;
    public PlatformBootstrap(JdbcTemplate jdbc, PasswordEncoder passwords, PlatformTotp totp,
            @Value("${PLATFORM_BOOTSTRAP_EMAIL:}") String email, @Value("${PLATFORM_BOOTSTRAP_PASSWORD:}") String password) {
        this.jdbc = jdbc; this.passwords = passwords; this.totp = totp; this.email = email; this.password = password;
    }
    @Override @Transactional
    public void run(ApplicationArguments arguments) {
        totp.validateConfiguration();
        jdbc.execute("SELECT pg_advisory_xact_lock(72839124)");
        String normalized = email.trim().toLowerCase(java.util.Locale.ROOT);
        if (!normalized.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") || normalized.length() > 254)
            throw new IllegalStateException("Configure PLATFORM_BOOTSTRAP_EMAIL.");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE platform_admin=TRUE", Long.class) > 0) {
            if (jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE platform_admin=TRUE AND LOWER(email)=? AND registration='PLATFORM001'", Long.class, normalized) != 1)
                throw new IllegalStateException("A plataforma já possui um administrador diferente.");
            return; // Never overwrite an existing account or password on restart.
        }
        if (password.length() < 12 || password.length() > 64 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new IllegalStateException("A senha inicial deve ter 12 a 64 caracteres e até 72 bytes UTF-8.");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE LOWER(email)=? OR UPPER(registration)='PLATFORM001'", Long.class, normalized) > 0)
            throw new IllegalStateException("O e-mail ou cadastro inicial já está em uso. Nenhuma conta foi promovida.");
        jdbc.update("INSERT INTO users (id,name,registration,password_hash,role,active,email,platform_admin,created_at,updated_at) VALUES (?, 'Administração LinePulse','PLATFORM001',?,'OPERATOR',TRUE,?,TRUE,NOW(),NOW())", UUID.randomUUID(), passwords.encode(password), normalized);
        jdbc.update("INSERT INTO platform_audit_events (actor_registration,action,detail) VALUES ('SYSTEM','PLATFORM_BOOTSTRAPPED','Primeiro administrador criado pela configuração do servidor.')");
    }
}
