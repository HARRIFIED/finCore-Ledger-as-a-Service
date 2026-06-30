/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Translates a validated JWT into the gateway-authoritative identity headers that
 * downstream services trust:
 *
 * <pre>
 *   X-Subject     ← jwt.sub        (opaque end-user id from the licensee's IdP)
 *   X-Program-Id  ← jwt.program_id (optional sub-program / brand segment)
 *   X-Scopes      ← jwt scopes     (normalised to a space-delimited string)
 * </pre>
 *
 * <p>Two security guarantees:
 * <ol>
 *   <li><b>Strip-then-set:</b> any inbound copy of these headers is removed first, so a
 *       client can never spoof an identity by sending them — they are set only from a
 *       cryptographically verified token.</li>
 *   <li>Claim names are configurable so an external (federated) IdP can map its own
 *       claim vocabulary without code changes.</li>
 * </ol>
 */
@Component
public class IdentityHeaderGlobalFilter implements GlobalFilter, Ordered {

    static final String H_SUBJECT = "X-Subject";
    static final String H_PROGRAM = "X-Program-Id";
    static final String H_SCOPES = "X-Scopes";

    private final String programClaim;
    private final String scopeClaim;

    public IdentityHeaderGlobalFilter(
            @Value("${gateway.identity.program-claim:program_id}") String programClaim,
            @Value("${gateway.identity.scope-claim:scope}") String scopeClaim) {
        this.programClaim = programClaim;
        this.scopeClaim = scopeClaim;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // (1) Strip client-supplied identity headers unconditionally — even for public
        //     routes that carry no token — so they can never be injected from outside.
        ServerHttpRequest stripped = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove(H_SUBJECT);
                    h.remove(H_PROGRAM);
                    h.remove(H_SCOPES);
                })
                .build();

        // (2) When the request is authenticated, set the headers from the verified JWT.
        return exchange.getPrincipal()
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(auth -> exchange.mutate().request(withIdentityHeaders(stripped, auth.getToken())).build())
                .defaultIfEmpty(exchange.mutate().request(stripped).build())
                .flatMap(chain::filter);
    }

    private ServerHttpRequest withIdentityHeaders(ServerHttpRequest stripped, Jwt jwt) {
        return stripped.mutate()
                .headers(h -> {
                    h.set(H_SUBJECT, jwt.getSubject());

                    String program = jwt.getClaimAsString(programClaim);
                    if (program != null && !program.isBlank()) {
                        h.set(H_PROGRAM, program);
                    }

                    String scopes = extractScopes(jwt);
                    if (!scopes.isBlank()) {
                        h.set(H_SCOPES, scopes);
                    }
                })
                .build();
    }

    /**
     * Normalises scopes to a single space-delimited string. Tolerates the common shapes:
     * the OAuth2 {@code scope} string, an array claim ({@code scp}/{@code roles}), or the
     * bundled iam-service's single {@code role} claim.
     */
    private String extractScopes(Jwt jwt) {
        Object configured = jwt.getClaims().get(scopeClaim);
        String fromConfigured = stringifyScopes(configured);
        if (!fromConfigured.isBlank()) {
            return fromConfigured;
        }
        // Fallbacks for IdPs that use a different claim than the configured one.
        for (String fallback : List.of("scp", "roles", "role")) {
            String value = stringifyScopes(jwt.getClaims().get(fallback));
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String stringifyScopes(Object raw) {
        if (raw instanceof String s) {
            return s.trim();
        }
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.joining(" "));
        }
        return "";
    }

    @Override
    public int getOrder() {
        // Run after Spring Security has authenticated the request (the JWT is then on the
        // exchange) but just before the routing filter forwards it downstream.
        return Ordered.LOWEST_PRECEDENCE - 1;
    }
}
