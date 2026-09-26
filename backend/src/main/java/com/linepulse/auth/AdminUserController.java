package com.linepulse.auth;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    List<AdminUserResponse> findAll() {
        return adminUserService.findAll();
    }

    @PatchMapping("/{userId}/role")
    AdminUserResponse updateRole(@PathVariable UUID userId, @Valid @RequestBody UpdateUserRoleRequest request, Principal principal) {
        return adminUserService.updateRole(userId, request, principal.getName());
    }

    @PatchMapping("/{userId}/status")
    AdminUserResponse updateStatus(@PathVariable UUID userId, @RequestBody UpdateUserStatusRequest request, Principal principal) {
        return adminUserService.updateStatus(userId, request, principal.getName());
    }
}
