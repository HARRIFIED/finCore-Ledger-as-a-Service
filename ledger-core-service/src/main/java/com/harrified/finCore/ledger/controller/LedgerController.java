/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.ledger.controller;

import com.harrified.finCore.ledger.domain.PostingLine;
import com.harrified.finCore.ledger.domain.entity.JournalEntry;
import com.harrified.finCore.ledger.domain.entity.LedgerAccount;
import com.harrified.finCore.ledger.dto.BalanceResponse;
import com.harrified.finCore.ledger.dto.CreateLedgerAccountRequest;
import com.harrified.finCore.ledger.dto.LedgerAccountResponse;
import com.harrified.finCore.ledger.dto.PostEntryRequest;
import com.harrified.finCore.ledger.dto.PostEntryResponse;
import com.harrified.finCore.ledger.service.LedgerService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ledger")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public LedgerAccountResponse createAccount(@Valid @RequestBody CreateLedgerAccountRequest request) {
        LedgerAccount account = ledgerService.createAccount(request.reference(), request.coaType(), request.currency());
        return new LedgerAccountResponse(account.getId(), account.getReference(), account.getCoaType(),
                account.getCurrency(), account.getStatus());
    }

    @PostMapping("/entries")
    @ResponseStatus(HttpStatus.CREATED)
    public PostEntryResponse postEntry(@Valid @RequestBody PostEntryRequest request) {
        List<PostingLine> lines = request.postings().stream()
                .map(p -> new PostingLine(p.ledgerAccountId(), p.direction(), p.amountMinor(), p.currency()))
                .toList();
        JournalEntry entry = ledgerService.postEntry(
                request.idempotencyKey(), request.valueDate(), lines, request.correlationId(), request.source());
        return new PostEntryResponse(entry.getId(), entry.getStatus().name());
    }

    @PostMapping("/entries/{id}/reverse")
    @ResponseStatus(HttpStatus.CREATED)
    public PostEntryResponse reverse(@PathVariable UUID id) {
        JournalEntry reversal = ledgerService.reverseEntry(id);
        return new PostEntryResponse(reversal.getId(), reversal.getStatus().name());
    }

    /**
     * Current balance, or the point-in-time posted balance as of a value date when
     * {@code asOf} is supplied (e.g. {@code ?asOf=2026-07-01}).
     */
    @GetMapping("/accounts/{id}/balance")
    public BalanceResponse balance(@PathVariable UUID id,
                                   @RequestParam(required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return asOf == null ? ledgerService.getBalance(id) : ledgerService.getBalanceAsOf(id, asOf);
    }
}
