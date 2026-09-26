package com.linepulse.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class DemoUserInitializerTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldSynchronizeExistingDemoUsersWithTheirIntendedRoles() throws Exception {
        UserAccount admin = existingUser("Administrador Antigo", "admin@linepulse.local", UserRole.OPERATOR);
        UserAccount technician = existingUser("Técnico Antigo", "technician@linepulse.local", UserRole.OPERATOR);
        UserAccount operator = existingUser("Operador Antigo", "operator@linepulse.local", UserRole.ADMIN);

        when(passwordEncoder.encode("DemoPass123!")).thenReturn("encoded-demo-password");
        when(userRepository.findByEmailIgnoreCase("admin@linepulse.local")).thenReturn(Optional.of(admin));
        when(userRepository.findByEmailIgnoreCase("technician@linepulse.local")).thenReturn(Optional.of(technician));
        when(userRepository.findByEmailIgnoreCase("operator@linepulse.local")).thenReturn(Optional.of(operator));

        DemoUserInitializer initializer = new DemoUserInitializer(userRepository, passwordEncoder, "DemoPass123!");
        initializer.run(null);

        assertDemoUser(admin, "Administrador Demo", UserRole.ADMIN);
        assertDemoUser(technician, "Técnico Demo", UserRole.TECHNICIAN);
        assertDemoUser(operator, "Operador Demo", UserRole.OPERATOR);
    }

    private UserAccount existingUser(String name, String email, UserRole role) {
        Instant now = Instant.now().minusSeconds(3600);
        return new UserAccount(
                UUID.randomUUID(),
                name,
                email,
                "old-password",
                role,
                false,
                now,
                now
        );
    }

    private void assertDemoUser(UserAccount user, String expectedName, UserRole expectedRole) {
        assertEquals(expectedName, user.getName());
        assertEquals(expectedRole, user.getRole());
        assertEquals("encoded-demo-password", user.getPasswordHash());
        assertTrue(user.isActive());
    }
}
