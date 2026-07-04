/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.account.controller;

import com.harrified.finCore.platform.security.CallerContext;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    /**
     * Echoes the identity the gateway injected. The caller is derived from the verified
     * token's claims (X-Subject / X-Program-Id / X-Scopes) — never from the request body.
     * Serves as a verifiable probe of the federation wiring; account ownership will be
     * keyed off {@code caller.subject()} once CRUD lands in the Phase 2 build.
     */
    @GetMapping("/me")
    public CallerContext me(CallerContext caller) {
        return caller;
    }
}
