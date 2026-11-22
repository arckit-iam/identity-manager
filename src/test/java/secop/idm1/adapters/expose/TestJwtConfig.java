// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.expose;

import java.time.Instant;
import java.util.List;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/** Décodeur de test : les jetons « publisher » et « reader » portent le rôle ARC correspondant. */
@TestConfiguration
class TestJwtConfig {

    @Bean
    JwtDecoder jwtDecoder() {
        return token -> switch (token) {
            case "publisher" -> jwt(List.of("ARC_PUBLISHER"));
            case "reader" -> jwt(List.of("ARC_READER"));
            case "nobody" -> jwt(List.of());
            default -> throw new BadJwtException("jeton invalide");
        };
    }

    private static Jwt jwt(List<String> roles) {
        return Jwt.withTokenValue("t").header("alg", "RS256").subject("alice").claim("roles", roles)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
    }

    static String bearer(String token) {
        return "Bearer " + token;
    }
}
