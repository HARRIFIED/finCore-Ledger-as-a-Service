/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.platform.tenancy;

/**
 * Holds the current request's tenant id (the licensee's program/brand — see
 * requirements §2.1) for the duration of a request. Populated by
 * {@link TenantContextFilter} from the gateway-injected {@code X-Program-Id} header and
 * read by {@link FinCoreTenantIdentifierResolver} so Hibernate scopes every query/insert.
 *
 * <p>Backed by a {@link ThreadLocal}: each (virtual) request thread has its own value, set
 * at the start of the request and cleared at the end. Never leave it set across requests.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }
    /** Set the current tenant. */
    public static void set(String tenantId) {
        CURRENT.set(tenantId);
    }

    /** The current tenant, or {@code null} if none is set (e.g. outside a request). */
    public static String get() {
        return CURRENT.get();
    }

    /** The current tenant, or throw if absent — for code paths that must be tenant-scoped. */
    public static String require() {
        String tenantId = CURRENT.get();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("No tenant in context (missing X-Program-Id?)");
        }
        return tenantId;
    }

    /** Clear the current tenant. MUST be called at the end of every request. */
    public static void clear() {
        CURRENT.remove();
    }
}
