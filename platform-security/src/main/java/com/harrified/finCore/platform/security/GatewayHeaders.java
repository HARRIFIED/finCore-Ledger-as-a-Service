/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.platform.security;

/**
 * Names of the identity headers the API Gateway injects after it validates a JWT.
 * The gateway strips any inbound copies of these and re-sets them from the verified
 * token, so downstream services can trust them (see the IAM ↔ API Gateway flow docs).
 */
public final class GatewayHeaders {

    public static final String SUBJECT = "X-Subject";
    public static final String PROGRAM_ID = "X-Program-Id";
    public static final String SCOPES = "X-Scopes";

    private GatewayHeaders() {
    }
}
