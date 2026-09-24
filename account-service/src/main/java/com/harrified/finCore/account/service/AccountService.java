/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.account.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harrified.finCore.account.client.LedgerClient;
import com.harrified.finCore.account.client.PartyClient;
import com.harrified.finCore.account.domain.entity.Account;
import com.harrified.finCore.account.domain.enums.AccountCurrency;
import com.harrified.finCore.account.domain.enums.AccountStatus;
import com.harrified.finCore.account.dto.AccountResponse;
import com.harrified.finCore.account.dto.CreateAccountRequest;
import com.harrified.finCore.account.repository.AccountRepository;
import com.harrified.finCore.platform.outbox.OutboxAppender;
import com.harrified.finCore.platform.security.CallerContext;
import com.harrified.finCore.platform.tenancy.TenantContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Opens and reads customer accounts. Opening connects three services: authorize the caller
 * against the owning party (party-service), create the backing ledger account (ledger-core),
 * then persist the account keyed on {@code partyId} and emit {@code account.opened}.
 */
@Service
public class AccountService {

    private static final String TOPIC = "account.opened";
    // Roles permitted to open an account for a party.
    private static final Set<String> CAN_OPEN = Set.of("OWNER", "ADMIN");

    private final AccountRepository accounts;
    private final PartyClient partyClient;
    private final LedgerClient ledgerClient;
    private final OutboxAppender outbox;
    private final ObjectMapper objectMapper;

    public AccountService(AccountRepository accounts, PartyClient partyClient, LedgerClient ledgerClient,
                          OutboxAppender outbox, ObjectProvider<ObjectMapper> objectMapper) {
        this.accounts = accounts;
        this.partyClient = partyClient;
        this.ledgerClient = ledgerClient;
        this.outbox = outbox;
        // Boot 4 doesn't expose a standalone ObjectMapper bean by default; fall back to our own.
        this.objectMapper = objectMapper.getIfAvailable(() -> new ObjectMapper().findAndRegisterModules());
    }

    @Transactional
    public AccountResponse openAccount(CallerContext caller, CreateAccountRequest request) {
        // 1. Authorize the caller against the owning party (ADR-0006).
        PartyClient.Access access = partyClient.access(request.partyId(), caller.subject());
        boolean canOpen = access.allowed() && access.roles().stream().anyMatch(CAN_OPEN::contains);
        if (!canOpen) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "subject " + caller.subject() + " may not open accounts for party " + request.partyId());
        }

        // 2. Currency must be one this service supports.
        AccountCurrency currency;
        try {
            currency = AccountCurrency.valueOf(request.currency());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Unsupported currency " + request.currency());
        }

        // 3. Create the backing ledger account (a customer wallet is a LIABILITY).
        String accountNumber = generateAccountNumber();
        UUID ledgerAccountId = ledgerClient.createLedgerAccount(accountNumber, "LIABILITY", request.currency());

        // 4. Persist the account, owned by the party. saveAndFlush forces the insert now so
        // Hibernate's @CreationTimestamp/@UpdateTimestamp (and @Version) are populated on the
        // returned entity — otherwise they generate at commit, after we build the response.
        Account account = accounts.saveAndFlush(Account.builder()
                .partyId(request.partyId())
                .ledgerAccountId(ledgerAccountId)
                .accountNumber(accountNumber)
                .accountName(request.accountName())
                .description(request.description())
                .type(request.type())
                .currency(currency)
                .status(AccountStatus.ACTIVE)
                .parentAccountId(request.parentAccountId())
                .metadata(toJson(request.metadata()))
                .tags(request.tags() == null ? new ArrayList<>() : new ArrayList<>(request.tags()))
                .build());

        // 5. Emit account.opened in the same transaction (outbox).
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("accountId", account.getId().toString());
        payload.put("partyId", account.getPartyId().toString());
        payload.put("ledgerAccountId", ledgerAccountId.toString());
        payload.put("currency", request.currency());
        outbox.append(TenantContext.require(), TOPIC, account.getId().toString(), TOPIC, payload);

        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID id) {
        return accounts.findById(id)
                .map(AccountResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown account " + id));
    }

    private String toJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid metadata");
        }
    }

    private String generateAccountNumber() {
        return "AC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase();
    }
}
