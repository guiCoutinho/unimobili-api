package com.unimobili.agenda.auth;

import com.unimobili.agenda.auth.dto.MeResponse;
import com.unimobili.agenda.auth.dto.TokenResponse;
import com.unimobili.agenda.security.JwtService;
import com.unimobili.agenda.user.User;
import com.unimobili.agenda.user.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtDecoder jwtDecoder;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       JwtDecoder jwtDecoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtDecoder = jwtDecoder;
    }

    public TokenResponse login(String email, String senha) {
        User user = userRepository.findByEmailAndAtivoTrue(email)
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

        if (!passwordEncoder.matches(senha, user.getSenha())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }

        return issueTokens(user);
    }

    public TokenResponse refresh(String refreshToken) {
        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(refreshToken);
        } catch (JwtException ex) {
            throw new BadCredentialsException("Refresh token inválido");
        }

        if (!JwtService.TYPE_REFRESH.equals(jwt.getClaimAsString(JwtService.CLAIM_TYPE))) {
            throw new BadCredentialsException("Token informado não é um refresh token");
        }

        User user = userRepository.findByEmailAndAtivoTrue(jwt.getSubject())
                .orElseThrow(() -> new BadCredentialsException("Usuário não encontrado"));

        return issueTokens(user);
    }

    public MeResponse me(String email) {
        User user = userRepository.findByEmailAndAtivoTrue(email)
                .orElseThrow(() -> new BadCredentialsException("Usuário não encontrado"));
        return new MeResponse(user.getId(), user.getNome(), user.getEmail(), user.getRole());
    }

    private TokenResponse issueTokens(User user) {
        return new TokenResponse(
                jwtService.accessToken(user),
                jwtService.refreshToken(user),
                "Bearer",
                jwtService.accessTtl().toSeconds()
        );
    }
}
