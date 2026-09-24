/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.transfer.client;

import com.harrified.finCore.grpc.ledger.Balance;
import com.harrified.finCore.grpc.ledger.GetBalanceRequest;
import com.harrified.finCore.grpc.ledger.JournalEntry;
import com.harrified.finCore.grpc.ledger.LedgerServiceGrpc;
import com.harrified.finCore.grpc.ledger.PostJournalEntryRequest;
import com.harrified.finCore.grpc.ledger.PostingLine;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Calls ledger-core over gRPC (ADR-0010): read balances and post the balanced double-entry that
 * actually moves the money. Tenant propagated via gRPC metadata automatically.
 */
@Component
public class LedgerClient {

    @GrpcClient("ledger")
    private LedgerServiceGrpc.LedgerServiceBlockingStub ledgerStub;

    /** Available balance (posted − held) for a ledger account, in minor units. */
    public long availableBalance(UUID ledgerAccountId) {
        Balance b = ledgerStub.getBalance(GetBalanceRequest.newBuilder()
                .setLedgerAccountId(ledgerAccountId.toString())
                .build());
        return b.getAvailable();
    }

    /**
     * Post the transfer as one balanced entry: debit the source wallet (reduces the customer's
     * liability balance), credit the destination. Idempotent on {@code idempotencyKey}.
     * Returns the journal entry id.
     */
    public UUID postTransfer(String idempotencyKey, LocalDate valueDate, UUID fromLedgerAccountId,
                             UUID toLedgerAccountId, long amountMinor, String currency,
                             String correlationId) {
        JournalEntry entry = ledgerStub.postJournalEntry(PostJournalEntryRequest.newBuilder()
                .setIdempotencyKey(idempotencyKey)
                .setValueDate(valueDate.toString())
                .setCorrelationId(correlationId)
                .setSource("transfer")
                .addPostings(PostingLine.newBuilder()
                        .setLedgerAccountId(fromLedgerAccountId.toString())
                        .setDirection("DEBIT")
                        .setAmountMinor(amountMinor)
                        .setCurrency(currency)
                        .build())
                .addPostings(PostingLine.newBuilder()
                        .setLedgerAccountId(toLedgerAccountId.toString())
                        .setDirection("CREDIT")
                        .setAmountMinor(amountMinor)
                        .setCurrency(currency)
                        .build())
                .build());
        return UUID.fromString(entry.getId());
    }
}
