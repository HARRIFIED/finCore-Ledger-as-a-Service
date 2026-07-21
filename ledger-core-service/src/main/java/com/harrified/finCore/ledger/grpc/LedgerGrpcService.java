/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.ledger.grpc;

import com.harrified.finCore.grpc.ledger.Balance;
import com.harrified.finCore.grpc.ledger.CreateLedgerAccountRequest;
import com.harrified.finCore.grpc.ledger.GetBalanceRequest;
import com.harrified.finCore.grpc.ledger.JournalEntry;
import com.harrified.finCore.grpc.ledger.LedgerAccount;
import com.harrified.finCore.grpc.ledger.LedgerServiceGrpc;
import com.harrified.finCore.grpc.ledger.PostJournalEntryRequest;
import com.harrified.finCore.ledger.domain.enums.CoaType;
import com.harrified.finCore.ledger.domain.enums.Direction;
import com.harrified.finCore.ledger.dto.BalanceResponse;
import com.harrified.finCore.ledger.service.LedgerService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * gRPC front door for the ledger core (ADR-0010) — internal callers (account, transfer, hold,
 * scheduler) reach it here rather than over REST. This is a thin adapter: it maps proto messages
 * to/from the domain and delegates to {@link LedgerService}. The tenant is already in
 * {@link com.harrified.finCore.platform.tenancy.TenantContext} — the global
 * {@code GrpcTenantServerInterceptor} read it from {@code x-program-id} metadata before we run.
 */
@GrpcService
public class LedgerGrpcService extends LedgerServiceGrpc.LedgerServiceImplBase {

    private final LedgerService ledgerService;

    public LedgerGrpcService(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @Override
    public void createLedgerAccount(CreateLedgerAccountRequest request,
                                    StreamObserver<LedgerAccount> responseObserver) {
        var account = ledgerService.createAccount(
                request.getReference(),
                CoaType.valueOf(request.getCoaType()),
                request.getCurrency());

        responseObserver.onNext(LedgerAccount.newBuilder()
                .setId(account.getId().toString())
                .setReference(account.getReference())
                .setCoaType(account.getCoaType().name())
                .setCurrency(account.getCurrency())
                .setStatus(account.getStatus().name())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void postJournalEntry(PostJournalEntryRequest request,
                                 StreamObserver<JournalEntry> responseObserver) {
        List<com.harrified.finCore.ledger.domain.PostingLine> lines = request.getPostingsList().stream()
                .map(p -> new com.harrified.finCore.ledger.domain.PostingLine(
                        UUID.fromString(p.getLedgerAccountId()),
                        Direction.valueOf(p.getDirection()),
                        p.getAmountMinor(),
                        p.getCurrency()))
                .toList();

        var entry = ledgerService.postEntry(
                request.getIdempotencyKey(),
                LocalDate.parse(request.getValueDate()),
                lines,
                emptyToNull(request.getCorrelationId()),
                emptyToNull(request.getSource()));

        responseObserver.onNext(JournalEntry.newBuilder()
                .setId(entry.getId().toString())
                .setStatus(entry.getStatus().name())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getBalance(GetBalanceRequest request, StreamObserver<Balance> responseObserver) {
        BalanceResponse b = ledgerService.getBalance(UUID.fromString(request.getLedgerAccountId()));

        responseObserver.onNext(Balance.newBuilder()
                .setLedgerAccountId(b.accountId().toString())
                .setCurrency(b.currency())
                .setPosted(b.posted())
                .setAvailable(b.available())
                .setHeld(b.held())
                .setPending(b.pending())
                .build());
        responseObserver.onCompleted();
    }

    /** Proto strings default to "" when unset; the domain treats those optional fields as null. */
    private static String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
