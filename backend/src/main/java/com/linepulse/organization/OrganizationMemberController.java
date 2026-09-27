package com.linepulse.organization;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organization-members")
public class OrganizationMemberController {
    private final OrganizationMemberService memberService;

    public OrganizationMemberController(OrganizationMemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    List<OrganizationMemberResponse> list() {
        return memberService.list();
    }

    @PostMapping
    OrganizationMemberResponse create(@Valid @RequestBody CreateOrganizationMemberRequest request) {
        return memberService.create(request);
    }

    @PatchMapping("/{userId}/role")
    OrganizationMemberResponse changeRole(@PathVariable UUID userId, @Valid @RequestBody UpdateOrganizationMemberRoleRequest request) {
        return memberService.changeRole(userId, request);
    }

    @PatchMapping("/{userId}/status")
    OrganizationMemberResponse changeStatus(@PathVariable UUID userId, @RequestBody UpdateOrganizationMemberStatusRequest request) {
        return memberService.changeStatus(userId, request);
    }
}
