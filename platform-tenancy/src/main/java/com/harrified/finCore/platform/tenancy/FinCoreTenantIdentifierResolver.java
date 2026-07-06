/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.platform.tenancy;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/**
 * Bridges {@link TenantContext} to Hibernate's discriminator (row-level) multi-tenancy.
 * Spring Boot detects this bean and wires it into Hibernate, so entities whose tenant
 * column is annotated {@code @TenantId} are automatically scoped on read and stamped on
 * write. Escalating to schema-/database-per-tenant later is a change of Hibernate strategy
 * here — not a change to business code (ADR-0009).
 */
public class FinCoreTenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    // Used only when there is no request tenant (startup, background threads). Real
    // tenant-scoped work always runs inside a request and resolves an actual tenant.
    static final String NO_TENANT = "__no_tenant__";

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenantId = TenantContext.get();
        return (tenantId == null || tenantId.isBlank()) ? NO_TENANT : tenantId;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
