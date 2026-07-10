/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.ledger.service;

import com.harrified.finCore.ledger.domain.DoubleEntryValidator;
import com.harrified.finCore.ledger.domain.PostingLine;
import com.harrified.finCore.ledger.domain.entity.JournalEntry;
import com.harrified.finCore.ledger.domain.entity.LedgerAccount;
import com.harrified.finCore.ledger.domain.entity.Posting;
import com.harrified.finCore.ledger.domain.enums.CoaType;
import com.harrified.finCore.ledger.domain.enums.EntryStatus;
import com.harrified.finCore.ledger.domain.enums.LedgerAccountStatus;
import com.harrified.finCore.ledger.repository.JournalEntryRepository;
import com.harrified.finCore.ledger.repository.LedgerAccountRepository;
import com.harrified.finCore.platform.outbox.OutboxAppender;
import com.harrified.finCore.platform.tenancy.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service for the ledger core: create ledger accounts and post balanced,
 * idempotent journal entries. A post is one local DB transaction (all legs + the outbox
 * row commit together — the transactional-outbox guarantee).
 */
@Service
public class LedgerService {

    private static final String TOPIC = "ledger.entry.posted";

    private final LedgerAccountRepository accounts;
    private final JournalEntryRepository entries;
    private final OutboxAppender outbox;

    public LedgerService(LedgerAccountRepository accounts, JournalEntryRepository entries, OutboxAppender outbox) {
        this.accounts = accounts;
        this.entries = entries;
        this.outbox = outbox;
    }

    @Transactional
    public LedgerAccount createAccount(String reference, CoaType coaType, String currency) {
        LedgerAccount account = LedgerAccount.builder()
                .reference(reference)
                .coaType(coaType)
                .currency(currency.toUpperCase())
                .status(LedgerAccountStatus.ACTIVE)
                .build();
        return accounts.save(account);
    }

    @Transactional
    public JournalEntry postEntry(String idempotencyKey, LocalDate valueDate, List<PostingLine> lines,
                                  String correlationId, String source) {
        // Idempotent replay: same key → return the original entry, no new postings (FR-16).
        Optional<JournalEntry> existing = entries.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        DoubleEntryValidator.validate(lines);
        validateAccounts(lines);

        JournalEntry entry = JournalEntry.builder()
                .idempotencyKey(idempotencyKey)
                .status(EntryStatus.POSTED)
                .bookingTime(Instant.now())
                .valueDate(valueDate)
                .correlationId(correlationId)
                .source(source)
                .build();
        for (PostingLine line : lines) {
            entry.addPosting(Posting.builder()
                    .ledgerAccountId(line.ledgerAccountId())
                    .direction(line.direction())
                    .amountMinor(line.amountMinor())
                    .currency(line.currency().toUpperCase())
                    .build());
        }

        JournalEntry saved = entries.save(entry);

        // Written in THIS transaction, so the event commits atomically with the entry (FR-25).
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("entryId", saved.getId().toString());
        payload.put("status", saved.getStatus().name());
        payload.put("valueDate", valueDate.toString());
        payload.put("postingCount", lines.size());
        outbox.append(TenantContext.require(), TOPIC, saved.getId().toString(), TOPIC, payload);

        return saved;
    }

    /** Every referenced account must exist (in this tenant), be ACTIVE, and match the posting currency. */
    private void validateAccounts(List<PostingLine> lines) {
        Set<UUID> ids = lines.stream().map(PostingLine::ledgerAccountId).collect(Collectors.toSet());
        Map<UUID, LedgerAccount> byId = accounts.findAllById(ids).stream()
                .collect(Collectors.toMap(LedgerAccount::getId, a -> a));

        for (PostingLine line : lines) {
            LedgerAccount account = byId.get(line.ledgerAccountId());
            if (account == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Unknown ledger account " + line.ledgerAccountId());
            }
            if (account.getStatus() != LedgerAccountStatus.ACTIVE) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Ledger account " + account.getId() + " is " + account.getStatus());
            }
            if (!account.getCurrency().equalsIgnoreCase(line.currency())) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Posting currency " + line.currency() + " does not match account currency "
                                + account.getCurrency() + " for account " + account.getId());
            }
        }
    }
}
