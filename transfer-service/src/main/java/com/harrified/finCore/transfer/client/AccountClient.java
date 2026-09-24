/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.transfer.client;

import com.harrified.finCore.grpc.account.Account;
import com.harrified.finCore.grpc.account.AccountServiceGrpc;
import com.harrified.finCore.grpc.account.GetAccountRequest;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Calls account-service over gRPC to resolve a customer account to its backing ledger account
 * and owning party (ADR-0010). Tenant propagated via gRPC metadata automatically.
 */
@Component
public class AccountClient {

    @GrpcClient("account")
    private AccountServiceGrpc.AccountServiceBlockingStub accountStub;

    public AccountView get(UUID accountId) {
        Account a = accountStub.getAccount(GetAccountRequest.newBuilder()
                .setAccountId(accountId.toString())
                .build());
        return new AccountView(
                UUID.fromString(a.getId()),
                UUID.fromString(a.getPartyId()),
                UUID.fromString(a.getLedgerAccountId()),
                a.getCurrency(),
                a.getStatus());
    }

    public record AccountView(UUID id, UUID partyId, UUID ledgerAccountId, String currency, String status) {
    }
}
