package com.unimobili.agenda.security;

import com.unimobili.agenda.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class JwtService {

    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_UID = "uid";
    public static final String CLAIM_TYPE = "type";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final JwtEncoder encoder;
    private final Clock clock;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtService(JwtEncoder encoder,
                      Clock clock,
                      @Value("${app.jwt.access-ttl}") Duration accessTtl,
                      @Value("${app.jwt.refresh-ttl}") Duration refreshTtl) {
        this.encoder = encoder;
        this.clock = clock;
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    public String accessToken(User user) {
        return token(user, TYPE_ACCESS, accessTtl);
    }

    public String refreshToken(User user) {
        return token(user, TYPE_REFRESH, refreshTtl);
    }

    public Duration accessTtl() {
        return accessTtl;
    }

    private String token(User user, String type, Duration ttl) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getEmail())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_UID, user.getId().toString())
                .claim(CLAIM_TYPE, type)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
