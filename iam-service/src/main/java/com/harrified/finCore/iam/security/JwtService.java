/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.iam.security;

import com.harrified.finCore.iam.domain.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtKeyProvider keyProvider;

    @Value("${jwt.expiry-seconds:900}")
    private long expirySeconds;

    // The issuer this IdP stamps on its tokens. When iam-service is the bundled IdP, the
    // gateway validates against this issuer's JWKS; federated IdPs stamp their own.
    @Value("${jwt.issuer:http://localhost:8092}")
    private String issuer;

    public JwtService(JwtEncoder jwtEncoder, JwtKeyProvider keyProvider) {
        this.jwtEncoder = jwtEncoder;
        this.keyProvider = keyProvider;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expirySeconds))
                // Emitted as program_id to match the gateway's X-Program-Id header. In the
                // licensed model a "tenant" is the licensee's sub-program / brand segment.
                .claim("program_id", user.getTenantId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    // Returns a raw opaque token to be given to the client.
    // Store hashToken(rawToken) in the database — never the raw value.
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    // SHA-256 hex of the raw token — safe to persist in the DB.
    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public long getExpirySeconds() {
        return expirySeconds;
    }

    // Returns the JWKS as a plain Map so the controller can serialise it directly to JSON.
    public Map<String, Object> buildJwks() {
        return keyProvider.getJwkSet().toJSONObject();
    }
}
