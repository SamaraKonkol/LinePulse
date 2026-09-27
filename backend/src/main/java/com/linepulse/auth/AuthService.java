package com.linepulse.auth;

import com.linepulse.common.UnauthorizedException;
import com.linepulse.organization.OrganizationService;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OrganizationService organizationService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, OrganizationService organizationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.organizationService = organizationService;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String registration = normalizeRegistration(request.registration());
        UserAccount user = userRepository.findByRegistrationIgnoreCase(registration)
                .filter(UserAccount::isActive)
                .orElseThrow(() -> new UnauthorizedException("Invalid registration or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid registration or password");
        }
        organizationService.ensureDefaultMembership(user);
        return new AuthResponse(jwtService.generate(user), AuthUserResponse.from(user));
    }

    private String normalizeRegistration(String registration) {
        return registration.trim().toUpperCase(Locale.ROOT);
    }
}
