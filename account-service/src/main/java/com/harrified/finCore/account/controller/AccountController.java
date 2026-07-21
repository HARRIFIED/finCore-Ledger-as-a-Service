/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.account.controller;

import com.harrified.finCore.account.dto.AccountResponse;
import com.harrified.finCore.account.dto.CreateAccountRequest;
import com.harrified.finCore.account.service.AccountService;
import com.harrified.finCore.platform.security.CallerContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Open a customer account for a party. The caller (from the gateway-injected identity)
     * must hold an OWNER/ADMIN membership on {@code partyId}; the account is backed by a
     * ledger account in ledger-core.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse open(CallerContext caller, @Valid @RequestBody CreateAccountRequest request) {
        return accountService.openAccount(caller, request);
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable UUID id) {
        return accountService.getAccount(id);
    }

    /** Probe: echoes the gateway-injected caller identity. */
    @GetMapping("/me")
    public CallerContext me(CallerContext caller) {
        return caller;
    }
}
