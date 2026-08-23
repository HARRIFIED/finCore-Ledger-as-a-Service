/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.transfer.service;

import com.harrified.finCore.platform.outbox.OutboxAppender;
import com.harrified.finCore.platform.security.CallerContext;
import com.harrified.finCore.platform.tenancy.TenantContext;
import com.harrified.finCore.transfer.client.AccountClient;
import com.harrified.finCore.transfer.client.LedgerClient;
import com.harrified.finCore.transfer.client.PartyClient;
import com.harrified.finCore.transfer.domain.entity.Transfer;
import com.harrified.finCore.transfer.domain.enums.TransferStatus;
import com.harrified.finCore.transfer.dto.CreateTransferRequest;
import com.harrified.finCore.transfer.dto.TransferResponse;
import com.harrified.finCore.transfer.repository.TransferRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Executes internal transfers (FR-5 internal). One transfer authorizes the caller against the
 * source account's party, resolves both accounts to their ledger accounts, checks funds, posts
 * one balanced double-entry in ledger-core, records the transfer, and emits {@code transfer.completed}.
 * All inter-service calls are gRPC (ADR-0010).
 */
@Service
public class TransferService {

    private static final String TOPIC = "transfer.completed";
    // Roles permitted to move money out of an account's party.
    private static final Set<String> CAN_INITIATE = Set.of("OWNER", "ADMIN", "INITIATOR");
    private static final String ACTIVE = "ACTIVE";

    private final TransferRepository transfers;
    private final PartyClient partyClient;
    private final AccountClient accountClient;
    private final LedgerClient ledgerClient;
    private final OutboxAppender outbox;

    public TransferService(TransferRepository transfers, PartyClient partyClient, AccountClient accountClient,
                           LedgerClient ledgerClient, OutboxAppender outbox) {
        this.transfers = transfers;
        this.partyClient = partyClient;
        this.accountClient = accountClient;
        this.ledgerClient = ledgerClient;
        this.outbox = outbox;
    }

    @Transactional
    public TransferResponse transfer(CallerContext caller, CreateTransferRequest request) {
        // 1. Idempotent replay: same key → return the original transfer, no new posting (FR-16).
        Optional<Transfer> existing = transfers.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            return TransferResponse.from(existing.get());
        }

        if (request.fromAccountId().equals(request.toAccountId())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Source and destination accounts must differ");
        }

        // 2. Resolve the source account and authorize the caller against its owning party (ADR-0006).
        AccountClient.AccountView from = accountClient.get(request.fromAccountId());
        PartyClient.Access access = partyClient.access(from.partyId(), caller.subject());
        boolean canInitiate = access.allowed() && access.roles().stream().anyMatch(CAN_INITIATE::contains);
        if (!canInitiate) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "subject " + caller.subject() + " may not initiate transfers for account " + request.fromAccountId());
        }

        // 3. Resolve the destination account.
        AccountClient.AccountView to = accountClient.get(request.toAccountId());

        // 4. Both accounts must be active, and single-currency for now (cross-currency is Phase 6).
        String currency = request.currency().toUpperCase();
        if (!ACTIVE.equalsIgnoreCase(from.status())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Source account is not ACTIVE");
        }
        if (!ACTIVE.equalsIgnoreCase(to.status())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Destination account is not ACTIVE");
        }
        if (!from.currency().equalsIgnoreCase(currency) || !to.currency().equalsIgnoreCase(currency)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cross-currency transfers are not supported yet (Phase 6); both accounts must be " + currency);
        }

        // 5. Funds check against the source's available balance.
        // NOTE: check-then-post has a TOCTOU race under concurrency (Phase 4 hardening: ledger-level
        // guard / optimistic retry before the 5k-TPS target).
        long available = ledgerClient.availableBalance(from.ledgerAccountId());
        if (available < request.amountMinor()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Insufficient funds: available " + available + " < " + request.amountMinor());
        }

        // 6. Post the balanced entry in ledger-core (idempotent on the namespaced key).
        java.util.UUID ledgerEntryId = ledgerClient.postTransfer(
                "transfer:" + request.idempotencyKey(), LocalDate.now(),
                from.ledgerAccountId(), to.ledgerAccountId(), request.amountMinor(), currency,
                request.idempotencyKey());

        // 7. Record the transfer.
        Transfer saved = transfers.save(Transfer.builder()
                .fromAccountId(from.id())
                .toAccountId(to.id())
                .fromLedgerAccountId(from.ledgerAccountId())
                .toLedgerAccountId(to.ledgerAccountId())
                .amountMinor(request.amountMinor())
                .currency(currency)
                .status(TransferStatus.COMPLETED)
                .idempotencyKey(request.idempotencyKey())
                .ledgerEntryId(ledgerEntryId)
                .reference(request.reference())
                .build());

        // 8. Emit transfer.completed in the same transaction (outbox, FR-25).
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("transferId", saved.getId().toString());
        payload.put("fromAccountId", saved.getFromAccountId().toString());
        payload.put("toAccountId", saved.getToAccountId().toString());
        payload.put("amountMinor", saved.getAmountMinor());
        payload.put("currency", currency);
        payload.put("ledgerEntryId", ledgerEntryId.toString());
        outbox.append(TenantContext.require(), TOPIC, saved.getId().toString(), TOPIC, payload);

        return TransferResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public TransferResponse getTransfer(java.util.UUID id) {
        return transfers.findById(id)
                .map(TransferResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown transfer " + id));
    }
}
