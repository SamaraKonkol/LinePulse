package com.linepulse.auth;

import com.linepulse.common.UnauthorizedException;
import com.linepulse.organization.OrganizationService;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    @org.springframework.beans.factory.annotation.Autowired
    private com.linepulse.platform.PlatformTotp platformTotp;
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
        LoginAttemptTracker.checkAllowed(registration);

        UserAccount user = userRepository.findByRegistrationIgnoreCase(registration)
                .filter(UserAccount::isActive)
                .orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            LoginAttemptTracker.recordFailure(registration);
            throw new UnauthorizedException("Invalid registration or password");
        }

        if (user.isPlatformAdmin() && !platformTotp.consume(user.getId(), request.otp())) {
            LoginAttemptTracker.recordFailure(registration);
            throw new UnauthorizedException("Invalid registration, password or authenticator code");
        }
        LoginAttemptTracker.recordSuccess(registration);
        organizationService.ensureDefaultMembership(user);
        return new AuthResponse(jwtService.generate(user), AuthUserResponse.from(user));
    }

    private String normalizeRegistration(String registration) {
        return registration.trim().toUpperCase(Locale.ROOT);
    }
}
