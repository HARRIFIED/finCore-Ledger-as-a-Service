/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.platform.tenancy;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

/**
 * Injects the current request's tenant into outgoing gRPC calls as {@code x-program-id}
 * metadata, so a downstream service's {@link GrpcTenantServerInterceptor} can pick it up.
 * The counterpart of forwarding {@code X-Program-Id} on the REST clients.
 */
public class GrpcTenantClientInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                String tenantId = TenantContext.get();
                if (tenantId != null && !tenantId.isBlank()) {
                    headers.put(GrpcTenantServerInterceptor.PROGRAM_ID, tenantId);
                }
                super.start(responseListener, headers);
            }
        };
    }
}
