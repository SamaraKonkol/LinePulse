package com.linepulse.auth;

import com.linepulse.audit.AuditService;
import com.linepulse.common.ConflictException;
import com.linepulse.common.NotFoundException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {
    private static final Set<String> DEMO_REGISTRATIONS = Set.of("ADM001", "TEC001", "OPE001");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AdminUserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> findAll() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(UserAccount::getName, String.CASE_INSENSITIVE_ORDER))
                .map(AdminUserResponse::from)
                .toList();
    }

    @Transactional
    public AdminUserResponse create(AdminCreateUserRequest request) {
        String registration = normalizeRegistration(request.registration());
        if (userRepository.existsByRegistrationIgnoreCase(registration)) {
            throw new ConflictException("Employee registration is already registered");
        }

        Instant now = Instant.now();
        UserAccount saved = userRepository.save(new UserAccount(
                UUID.randomUUID(),
                request.name().trim(),
                registration,
                passwordEncoder.encode(request.password()),
                request.role(),
                true,
                now,
                now
        ));
        auditService.record("USER_CREATED", "USER", saved.getId(), "Usuário " + saved.getRegistration() + " criado como " + saved.getRole());
        return AdminUserResponse.from(saved);
    }

    @Transactional
    public AdminUserResponse updateRole(UUID userId, UpdateUserRoleRequest request, String actorRegistration) {
        UserAccount user = findUser(userId);
        validateEditable(user, actorRegistration);
        user.changeRole(request.role());
        auditService.record("USER_ROLE_CHANGED", "USER", user.getId(), "Perfil do cadastro " + user.getRegistration() + " alterado para " + request.role());
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse updateStatus(UUID userId, UpdateUserStatusRequest request, String actorRegistration) {
        UserAccount user = findUser(userId);
        validateEditable(user, actorRegistration);
        user.changeActive(request.active());
        auditService.record("USER_STATUS_CHANGED", "USER", user.getId(), "Cadastro " + user.getRegistration() + (request.active() ? " ativado" : " desativado"));
        return AdminUserResponse.from(user);
    }

    @Transactional
    public void delete(UUID userId, String actorRegistration) {
        UserAccount user = findUser(userId);
        validateEditable(user, actorRegistration);
        auditService.record("USER_DELETED", "USER", user.getId(), "Usuário " + user.getName() + " · cadastro " + user.getRegistration() + " excluído");
        userRepository.delete(user);
    }

    private UserAccount findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private void validateEditable(UserAccount user, String actorRegistration) {
        if (user.getRegistration().equalsIgnoreCase(actorRegistration)) {
            throw new ConflictException("You cannot change or delete your own administrative access");
        }
        if (DEMO_REGISTRATIONS.contains(user.getRegistration())) {
            throw new ConflictException("Demo accounts have fixed access");
        }
    }

    private String normalizeRegistration(String registration) {
        return registration.trim().toUpperCase(Locale.ROOT);
    }
}
