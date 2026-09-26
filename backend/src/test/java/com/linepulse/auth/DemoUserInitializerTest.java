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
        UserAccount admin = existingUser("Administrador Antigo", "ADM001", UserRole.OPERATOR);
        UserAccount technician = existingUser("Técnico Antigo", "TEC001", UserRole.OPERATOR);
        UserAccount operator = existingUser("Operador Antigo", "OPE001", UserRole.ADMIN);

        when(passwordEncoder.encode("DemoPass123!")).thenReturn("encoded-demo-password");
        when(userRepository.findByRegistrationIgnoreCase("ADM001")).thenReturn(Optional.of(admin));
        when(userRepository.findByRegistrationIgnoreCase("TEC001")).thenReturn(Optional.of(technician));
        when(userRepository.findByRegistrationIgnoreCase("OPE001")).thenReturn(Optional.of(operator));

        DemoUserInitializer initializer = new DemoUserInitializer(userRepository, passwordEncoder, "DemoPass123!");
        initializer.run(null);

        assertDemoUser(admin, "Administrador Demo", "ADM001", UserRole.ADMIN);
        assertDemoUser(technician, "Técnico Demo", "TEC001", UserRole.TECHNICIAN);
        assertDemoUser(operator, "Operador Demo", "OPE001", UserRole.OPERATOR);
    }

    private UserAccount existingUser(String name, String registration, UserRole role) {
        Instant now = Instant.now().minusSeconds(3600);
        return new UserAccount(
                UUID.randomUUID(),
                name,
                registration,
                "old-password",
                role,
                false,
                now,
                now
        );
    }

    private void assertDemoUser(UserAccount user, String expectedName, String expectedRegistration, UserRole expectedRole) {
        assertEquals(expectedName, user.getName());
        assertEquals(expectedRegistration, user.getRegistration());
        assertEquals(expectedRole, user.getRole());
        assertEquals("encoded-demo-password", user.getPasswordHash());
        assertTrue(user.isActive());
    }
}
