/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.ledger.service;

import com.harrified.finCore.ledger.domain.Balances;
import com.harrified.finCore.ledger.domain.DoubleEntryValidator;
import com.harrified.finCore.ledger.domain.PostingLine;
import com.harrified.finCore.ledger.domain.entity.JournalEntry;
import com.harrified.finCore.ledger.domain.entity.LedgerAccount;
import com.harrified.finCore.ledger.domain.entity.Posting;
import com.harrified.finCore.ledger.domain.enums.CoaType;
import com.harrified.finCore.ledger.domain.enums.Direction;
import com.harrified.finCore.ledger.domain.enums.EntryStatus;
import com.harrified.finCore.ledger.domain.enums.LedgerAccountStatus;
import com.harrified.finCore.ledger.domain.enums.NormalSide;
import com.harrified.finCore.ledger.dto.BalanceResponse;
import com.harrified.finCore.ledger.repository.DebitCreditSum;
import com.harrified.finCore.ledger.repository.JournalEntryRepository;
import com.harrified.finCore.ledger.repository.LedgerAccountRepository;
import com.harrified.finCore.ledger.repository.PostingRepository;
import com.harrified.finCore.ledger.snapshot.SnapshotStore;
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
    private final PostingRepository postings;
    private final SnapshotStore snapshots;
    private final OutboxAppender outbox;

    public LedgerService(LedgerAccountRepository accounts, JournalEntryRepository entries,
                         PostingRepository postings, SnapshotStore snapshots, OutboxAppender outbox) {
        this.accounts = accounts;
        this.entries = entries;
        this.postings = postings;
        this.snapshots = snapshots;
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

    /**
     * Current balances for an account (FR-3), derived by folding its postings (ADR-0005),
     * normalized by the account's CoA normal side. {@code held}/{@code pending} are 0 until
     * holds (Phase 4) and external settlement (Phase 7).
     */
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID accountId) {
        LedgerAccount account = accounts.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Unknown ledger account " + accountId));

        String tenantId = TenantContext.require();
        NormalSide side = account.getCoaType().normalSide();

        // Posted balance = snapshot ⊕ fold of postings newer than the snapshot (ADR-0005).
        SnapshotStore.SnapshotRow snapshot = snapshots.find(accountId, tenantId).orElse(null);
        long baseSeq = snapshot != null ? snapshot.asOfSeq() : 0L;
        long baseDebits = snapshot != null ? snapshot.debits() : 0L;
        long baseCredits = snapshot != null ? snapshot.credits() : 0L;

        DebitCreditSum postedDelta = postings.fold(accountId, tenantId, EntryStatus.POSTED.name(), baseSeq);
        long posted = Balances.signed(side,
                baseDebits + nz(postedDelta.getDebits()),
                baseCredits + nz(postedDelta.getCredits()));

        // Pending (in-flight, unsettled) is small — folded in full, no snapshot.
        DebitCreditSum pendingFold = postings.fold(accountId, tenantId, EntryStatus.PENDING.name(), 0L);
        long pending = Balances.signed(side, nz(pendingFold.getDebits()), nz(pendingFold.getCredits()));

        long held = 0L; // active holds fold in here once hold-service lands (Phase 4)
        long available = posted - held;

        return new BalanceResponse(accountId, account.getCurrency(), posted, available, held, pending);
    }

    /**
     * Point-in-time posted balance as of a value date (FR-20): folds POSTED postings with
     * {@code value_date <= asOf}. Reflects the booked balance effective on that date; holds
     * and pending are current-state concepts (Phase 4/7) and are reported as 0 here.
     */
    @Transactional(readOnly = true)
    public BalanceResponse getBalanceAsOf(UUID accountId, LocalDate asOf) {
        LedgerAccount account = accounts.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Unknown ledger account " + accountId));

        String tenantId = TenantContext.require();
        NormalSide side = account.getCoaType().normalSide();

        DebitCreditSum fold = postings.foldAsOf(accountId, tenantId, EntryStatus.POSTED.name(), asOf);
        long posted = Balances.signed(side, nz(fold.getDebits()), nz(fold.getCredits()));

        return new BalanceResponse(accountId, account.getCurrency(), posted, posted, 0L, 0L);
    }

    private static long nz(Long value) {
        return value == null ? 0L : value;
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

    /**
     * Reverse a posted entry (FR-7) by appending a NEW entry with debit/credit swapped and
     * {@code reversalOf} set — the original is never mutated (append-only, FR-17). The reversal
     * is value-dated today, so the current balance nets to zero while point-in-time before the
     * reversal still shows the original (history is preserved). Idempotent: reversing an entry
     * that already has a reversal returns that reversal.
     */
    @Transactional
    public JournalEntry reverseEntry(UUID originalId) {
        JournalEntry original = entries.findById(originalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Unknown journal entry " + originalId));

        Optional<JournalEntry> existingReversal = entries.findByReversalOf(originalId);
        if (existingReversal.isPresent()) {
            return existingReversal.get();
        }

        if (original.getStatus() != EntryStatus.POSTED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Only POSTED entries can be reversed; " + originalId + " is " + original.getStatus());
        }

        JournalEntry reversal = JournalEntry.builder()
                .idempotencyKey("reversal:" + originalId)
                .status(EntryStatus.POSTED)
                .bookingTime(Instant.now())
                .valueDate(LocalDate.now())
                .correlationId(original.getCorrelationId())
                .source("reversal")
                .reversalOf(originalId)
                .build();
        for (Posting leg : original.getPostings()) {
            reversal.addPosting(Posting.builder()
                    .ledgerAccountId(leg.getLedgerAccountId())
                    .direction(leg.getDirection() == Direction.DEBIT ? Direction.CREDIT : Direction.DEBIT)
                    .amountMinor(leg.getAmountMinor())
                    .currency(leg.getCurrency())
                    .build());
        }

        JournalEntry saved = entries.save(reversal);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reversalEntryId", saved.getId().toString());
        payload.put("originalEntryId", originalId.toString());
        outbox.append(TenantContext.require(), "ledger.entry.reversed", saved.getId().toString(),
                "ledger.entry.reversed", payload);

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
