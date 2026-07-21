/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.account.client;

import com.harrified.finCore.grpc.party.AccessResponse;
import com.harrified.finCore.grpc.party.CheckAccessRequest;
import com.harrified.finCore.grpc.party.PartyServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Calls party-service over gRPC to authorize the caller against a party (ADR-0006/0010).
 * The tenant ({@code x-program-id}) rides along automatically — the global gRPC client
 * interceptor from platform-tenancy reads it from the current request's TenantContext.
 */
@Component
public class PartyClient {

    @GrpcClient("party")
    private PartyServiceGrpc.PartyServiceBlockingStub partyStub;

    /** "May {@code subject} act for {@code partyId}?" — returns whether allowed and the roles held. */
    public Access access(UUID partyId, String subject) {
        AccessResponse response = partyStub.checkAccess(CheckAccessRequest.newBuilder()
                .setPartyId(partyId.toString())
                .setSubject(subject)
                .build());
        return new Access(
                UUID.fromString(response.getPartyId()),
                response.getSubject(),
                response.getAllowed(),
                List.copyOf(response.getRolesList()));
    }

    public record Access(UUID partyId, String subject, boolean allowed, List<String> roles) {
    }
}
