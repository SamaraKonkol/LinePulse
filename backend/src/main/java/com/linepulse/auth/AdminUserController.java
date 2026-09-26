package com.linepulse.auth;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AdminUserResponse create(@Valid @RequestBody AdminCreateUserRequest request) {
        return adminUserService.create(request);
    }

    @PatchMapping("/{userId}/role")
    AdminUserResponse updateRole(@PathVariable UUID userId, @Valid @RequestBody UpdateUserRoleRequest request, Principal principal) {
        return adminUserService.updateRole(userId, request, principal.getName());
    }

    @PatchMapping("/{userId}/status")
    AdminUserResponse updateStatus(@PathVariable UUID userId, @RequestBody UpdateUserStatusRequest request, Principal principal) {
        return adminUserService.updateStatus(userId, request, principal.getName());
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID userId, Principal principal) {
        adminUserService.delete(userId, principal.getName());
    }
}
