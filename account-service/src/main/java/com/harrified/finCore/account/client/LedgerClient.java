/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.account.client;

import com.harrified.finCore.grpc.ledger.CreateLedgerAccountRequest;
import com.harrified.finCore.grpc.ledger.LedgerAccount;
import com.harrified.finCore.grpc.ledger.LedgerServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Calls ledger-core over gRPC to create the ledger account backing a customer account
 * (ADR-0010). The tenant ({@code x-program-id}) is injected automatically by the global gRPC
 * client interceptor from platform-tenancy.
 */
@Component
public class LedgerClient {

    @GrpcClient("ledger")
    private LedgerServiceGrpc.LedgerServiceBlockingStub ledgerStub;

    public UUID createLedgerAccount(String reference, String coaType, String currency) {
        LedgerAccount account = ledgerStub.createLedgerAccount(CreateLedgerAccountRequest.newBuilder()
                .setReference(reference)
                .setCoaType(coaType)
                .setCurrency(currency)
                .build());
        return UUID.fromString(account.getId());
    }
}
