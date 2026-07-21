/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.party.grpc;

import com.harrified.finCore.grpc.party.AccessResponse;
import com.harrified.finCore.grpc.party.CheckAccessRequest;
import com.harrified.finCore.grpc.party.PartyServiceGrpc;
import com.harrified.finCore.party.domain.enums.MembershipRole;
import com.harrified.finCore.party.service.MembershipService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

/**
 * gRPC front door for party authorization (ADR-0010) — other services ask "may this subject act
 * for this party?" here rather than over REST. Thin adapter over {@link MembershipService}; the
 * tenant is already in {@link com.harrified.finCore.platform.tenancy.TenantContext}, read from
 * {@code x-program-id} metadata by the global {@code GrpcTenantServerInterceptor}.
 */
@GrpcService
public class PartyGrpcService extends PartyServiceGrpc.PartyServiceImplBase {

    private final MembershipService membershipService;

    public PartyGrpcService(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @Override
    public void checkAccess(CheckAccessRequest request, StreamObserver<AccessResponse> responseObserver) {
        var access = membershipService.access(
                UUID.fromString(request.getPartyId()),
                request.getSubject());

        AccessResponse.Builder response = AccessResponse.newBuilder()
                .setPartyId(access.partyId().toString())
                .setSubject(access.subject())
                .setAllowed(access.allowed());
        for (MembershipRole role : access.roles()) {
            response.addRoles(role.name());
        }

        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }
}
