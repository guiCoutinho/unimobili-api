package com.unimobili.agenda.auth;

import com.unimobili.agenda.auth.dto.LoginRequest;
import com.unimobili.agenda.auth.dto.MeResponse;
import com.unimobili.agenda.auth.dto.RefreshRequest;
import com.unimobili.agenda.auth.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Login, refresh de token e usuário atual")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Autentica por e-mail e senha e retorna access + refresh tokens (JWT).")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.senha());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar token", description = "Recebe um refresh token válido e emite novos tokens.")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @GetMapping("/me")
    @Operation(summary = "Usuário atual", description = "Retorna os dados do usuário autenticado.")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.me(jwt.getSubject());
    }
}
