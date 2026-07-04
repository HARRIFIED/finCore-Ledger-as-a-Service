/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.platform.security;

import java.util.List;

/**
 * The authenticated caller, derived entirely from the gateway-injected identity
 * headers (never from the request body). Controllers receive this by declaring a
 * {@link CallerContext} parameter; see {@link CallerContextArgumentResolver}.
 *
 * <p>{@code subject} and {@code programId} are treated as OPAQUE strings: with the
 * bundled iam-service they are UUIDs, but a federated licensee IdP may use any
 * format, so downstream code must not assume one. Reconciling an opaque subject
 * with entities that key on a UUID owner is part of the customer/party-boundary work.
 */
public record CallerContext(String subject, String programId, List<String> scopes) {

    public boolean hasScope(String scope) {
        return scopes.contains(scope);
    }
}
