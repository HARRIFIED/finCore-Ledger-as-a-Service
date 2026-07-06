/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.platform.tenancy;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TenantContextFilterTest {

    private final TenantContextFilter filter = new TenantContextFilter();

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void setsTenantFromHeaderDuringChainAndClearsAfter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Program-Id", "tenant-abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] tenantDuringChain = new String[1];
        FilterChain chain = (req, res) -> tenantDuringChain[0] = TenantContext.get();

        filter.doFilter(request, response, chain);

        assertEquals("tenant-abc", tenantDuringChain[0], "tenant should be set while the chain runs");
        assertNull(TenantContext.get(), "tenant must be cleared after the request");
    }

    @Test
    void noHeaderLeavesContextEmpty() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] tenantDuringChain = new String[1];
        FilterChain chain = (req, res) -> tenantDuringChain[0] = TenantContext.get();

        filter.doFilter(request, response, chain);

        assertNull(tenantDuringChain[0], "no header ⇒ no tenant in context");
    }
}
