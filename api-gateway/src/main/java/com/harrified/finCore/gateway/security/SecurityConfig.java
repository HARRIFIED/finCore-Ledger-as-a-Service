/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * The gateway is an OAuth2 resource server. It validates inbound JWT signatures
 * against the configured issuer's JWKS (see {@code spring.security.oauth2.resourceserver.jwt}
 * in application.yaml) — by default the bundled iam-service, or the licensee's own IdP
 * when {@code AUTH_ISSUER_JWKS_URL} is pointed elsewhere.
 *
 * <p>It does NOT own users. Public auth endpoints (login/register) and the JWKS endpoint
 * are passed through unauthenticated so clients can obtain a token in the first place.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(ex -> ex
                        // CORS preflight never carries a token.
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        // Public so a client can authenticate / fetch keys through the gateway.
                        .pathMatchers("/api/v1/auth/**", "/api/v1/.well-known/**").permitAll()
                        .pathMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        // Everything else requires a valid token from the trusted issuer.
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                }))
                .build();
    }
}
