/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.platform.tenancy;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/**
 * gRPC counterpart of {@link TenantContextFilter}: reads the {@code x-program-id} metadata a
 * caller sends and populates {@link TenantContext} for the duration of the (unary) call, then
 * clears it. This is how the tenant propagates over gRPC (ADR-0009/0010).
 */
public class GrpcTenantServerInterceptor implements ServerInterceptor {

    static final Metadata.Key<String> PROGRAM_ID =
            Metadata.Key.of("x-program-id", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        String tenantId = headers.get(PROGRAM_ID);

        // For unary RPCs the service method runs during onHalfClose — set the tenant around it.
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(next.startCall(call, headers)) {
            @Override
            public void onHalfClose() {
                if (tenantId != null && !tenantId.isBlank()) {
                    TenantContext.set(tenantId.trim());
                }
                try {
                    super.onHalfClose();
                } finally {
                    TenantContext.clear();
                }
            }
        };
    }
}
