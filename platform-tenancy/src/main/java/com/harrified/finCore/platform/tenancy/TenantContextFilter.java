/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.platform.tenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Populates {@link TenantContext} from the gateway-injected {@code X-Program-Id} header for
 * the duration of the request, and clears it afterwards. Runs early so the tenant is
 * available before any persistence work.
 *
 * <p>The header is gateway-authoritative (injected from the verified token; see the
 * federation flow). This filter does not authenticate — it only propagates the tenant.
 */
public class TenantContextFilter extends OncePerRequestFilter implements Ordered {

    // Mirrors platform-security's GatewayHeaders.PROGRAM_ID (kept local to avoid a
    // cross-module dependency for a single stable constant).
    private static final String PROGRAM_ID_HEADER = "X-Program-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String tenantId = request.getHeader(PROGRAM_ID_HEADER);
            if (tenantId != null && !tenantId.isBlank()) {
                TenantContext.set(tenantId.trim());
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    public int getOrder() {
        // Early, so the tenant is set before downstream filters/handlers touch persistence.
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
