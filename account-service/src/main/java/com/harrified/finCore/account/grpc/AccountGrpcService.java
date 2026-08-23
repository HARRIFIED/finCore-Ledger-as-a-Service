/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.account.grpc;

import com.harrified.finCore.account.dto.AccountResponse;
import com.harrified.finCore.account.service.AccountService;
import com.harrified.finCore.grpc.account.Account;
import com.harrified.finCore.grpc.account.AccountServiceGrpc;
import com.harrified.finCore.grpc.account.GetAccountRequest;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

/**
 * gRPC front door for account lookups (ADR-0010) — transfer/hold services resolve a customer
 * account to its backing ledger account here rather than over REST. Thin adapter over
 * {@link AccountService}; the tenant is already in
 * {@link com.harrified.finCore.platform.tenancy.TenantContext}, read from {@code x-program-id}
 * metadata by the global {@code GrpcTenantServerInterceptor}.
 */
@GrpcService
public class AccountGrpcService extends AccountServiceGrpc.AccountServiceImplBase {

    private final AccountService accountService;

    public AccountGrpcService(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public void getAccount(GetAccountRequest request, StreamObserver<Account> responseObserver) {
        AccountResponse a = accountService.getAccount(UUID.fromString(request.getAccountId()));

        responseObserver.onNext(Account.newBuilder()
                .setId(a.id().toString())
                .setAccountNumber(a.accountNumber())
                .setPartyId(a.partyId().toString())
                .setLedgerAccountId(a.ledgerAccountId().toString())
                .setCurrency(a.currency())
                .setStatus(a.status().name())
                .setType(a.type().name())
                .build());
        responseObserver.onCompleted();
    }
}
