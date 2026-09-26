package com.linepulse.auth;

import com.linepulse.audit.AuditService;
import com.linepulse.common.ConflictException;
import com.linepulse.common.NotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {
    private final UserRepository userRepository;
    private final AuditService auditService;

    public AdminUserService(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
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
    public AdminUserResponse updateRole(UUID userId, UpdateUserRoleRequest request, String actorEmail) {
        UserAccount user = findUser(userId);
        validateEditable(user, actorEmail);
        user.changeRole(request.role());
        auditService.record("USER_ROLE_CHANGED", "USER", user.getId(), "Perfil de " + user.getEmail() + " alterado para " + request.role());
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse updateStatus(UUID userId, UpdateUserStatusRequest request, String actorEmail) {
        UserAccount user = findUser(userId);
        validateEditable(user, actorEmail);
        user.changeActive(request.active());
        auditService.record("USER_STATUS_CHANGED", "USER", user.getId(), "Usuário " + user.getEmail() + (request.active() ? " ativado" : " desativado"));
        return AdminUserResponse.from(user);
    }

    private UserAccount findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private void validateEditable(UserAccount user, String actorEmail) {
        if (user.getEmail().equalsIgnoreCase(actorEmail)) {
            throw new ConflictException("You cannot change your own administrative access");
        }
        if (user.getEmail().endsWith("@linepulse.local")) {
            throw new ConflictException("Demo accounts have fixed roles");
        }
    }
}
