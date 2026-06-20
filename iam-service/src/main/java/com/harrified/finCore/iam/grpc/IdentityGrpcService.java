/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.iam.grpc;

import com.harrified.finCore.grpc.common.PageMeta;
import com.harrified.finCore.grpc.identity.*;
import com.harrified.finCore.iam.domain.entity.User;
import com.harrified.finCore.iam.exception.NotFoundException;
import com.harrified.finCore.iam.repository.UserRepository;
import com.harrified.finCore.iam.security.JwtKeyProvider;
import com.nimbusds.jose.jwk.RSAKey;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.UUID;

@GrpcService
public class IdentityGrpcService extends IdentityServiceGrpc.IdentityServiceImplBase {

    private final UserRepository userRepository;
    private final JwtKeyProvider keyProvider;

    public IdentityGrpcService(UserRepository userRepository, JwtKeyProvider keyProvider) {
        this.userRepository = userRepository;
        this.keyProvider = keyProvider;
    }

    @Override
    public void getUser(GetUserRequest request, StreamObserver<UserResponse> responseObserver) {
        try {
            UUID userId = UUID.fromString(request.getUserId());
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found: " + request.getUserId()));

            responseObserver.onNext(toProto(user));
            responseObserver.onCompleted();

        } catch (NotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid user ID format")
                    .asRuntimeException());
        }
    }

    @Override
    public void getJwks(GetJwksRequest request, StreamObserver<GetJwksResponse> responseObserver) {
        try {
            GetJwksResponse.Builder response = GetJwksResponse.newBuilder();

            for (com.nimbusds.jose.jwk.JWK jwk : keyProvider.getJwkSet().getKeys()) {
                RSAKey rsaKey = (RSAKey) jwk;
                response.addKeys(JwkKey.newBuilder()
                        .setKty("RSA")
                        .setUse("sig")
                        .setAlg("RS256")
                        .setKid(rsaKey.getKeyID() != null ? rsaKey.getKeyID() : "")
                        .setN(rsaKey.getModulus().toString())
                        .setE(rsaKey.getPublicExponent().toString())
                        .build());
            }

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to build JWKS")
                    .asRuntimeException());
        }
    }

    @Override
    public void listUsers(ListUsersRequest request, StreamObserver<ListUsersResponse> responseObserver) {
        try {
            UUID tenantId = UUID.fromString(request.getTenantId());

            int page = request.hasPage() ? request.getPage().getPage() : 0;
            int size = request.hasPage() ? request.getPage().getSize() : 20;

            Page<User> userPage = userRepository.findAllByTenantId(
                    tenantId, PageRequest.of(page, Math.min(size, 100)));

            ListUsersResponse response = ListUsersResponse.newBuilder()
                    .addAllUsers(userPage.getContent().stream().map(this::toProto).toList())
                    .setMeta(PageMeta.newBuilder()
                            .setPage(userPage.getNumber())
                            .setSize(userPage.getSize())
                            .setTotalElements(userPage.getTotalElements())
                            .setTotalPages(userPage.getTotalPages())
                            .build())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid tenant ID format")
                    .asRuntimeException());
        }
    }

    private UserResponse toProto(User user) {
        UserResponse.Builder builder = UserResponse.newBuilder()
                .setId(user.getId().toString())
                .setTenantId(user.getTenantId().toString())
                .setEmail(user.getEmail())
                .setFirstName(user.getFirstName())
                .setLastName(user.getLastName())
                .addRoles(user.getRole().name())
                .setKycStatus(user.getKycStatus().name())
                .setStatus(user.getStatus().name());

        if (user.getPhoneNumber() != null) builder.setPhoneNumber(user.getPhoneNumber());

        return builder.build();
    }
}
