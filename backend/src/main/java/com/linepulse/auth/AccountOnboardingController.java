package com.linepulse.auth;

import com.linepulse.organization.OrganizationRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
public class AccountOnboardingController {
    private final AccountOnboardingService service;
    public AccountOnboardingController(AccountOnboardingService service) { this.service = service; }
    public record Invite(@NotBlank @Size(max=120) String name, @NotBlank @Size(max=40) String registration,
            @NotBlank @Email @Size(max=254) String email, @NotNull OrganizationRole role) {}
    public record Reset(@NotBlank @Email @Size(max=254) String email) {}
    public record Finish(@NotBlank @Size(min=43,max=43) String token, @NotBlank @Size(min=12,max=64) String password) {
        @AssertTrue(message="A senha deve ocupar no máximo 72 bytes UTF-8.")
        public boolean isPasswordLengthValid() { return password == null || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72; }
    }
    @PostMapping("/api/organization-members/invitations")
    public Map<String,String> invite(@Valid @RequestBody Invite input) { service.invite(input); return Map.of("message", "Convite enviado."); }
    @PostMapping("/api/auth/password-reset")
    public Map<String,String> reset(@Valid @RequestBody Reset input) { service.requestReset(input.email()); return Map.of("message", "Se houver uma conta ativa com esse e-mail, enviaremos as instruções."); }
    @PostMapping("/api/auth/complete-account")
    public Map<String,String> finish(@Valid @RequestBody Finish input) { service.finish(input.token(), input.password()); return Map.of("message", "Senha definida. Entre com seu cadastro."); }
}
