/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.platform.tenancy;

import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the tenancy seam for any service that adds {@code platform-tenancy}:
 * the {@link TenantContextFilter} (servlet web apps) and the
 * {@link FinCoreTenantIdentifierResolver} (when Hibernate is present). No per-service wiring.
 */
@AutoConfiguration
public class TenantAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    static class WebTenancyConfiguration {

        @Bean
        @ConditionalOnMissingBean
        TenantContextFilter tenantContextFilter() {
            return new TenantContextFilter();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(CurrentTenantIdentifierResolver.class)
    static class HibernateTenancyConfiguration {

        @Bean
        @ConditionalOnMissingBean(CurrentTenantIdentifierResolver.class)
        CurrentTenantIdentifierResolver<String> finCoreTenantIdentifierResolver() {
            return new FinCoreTenantIdentifierResolver();
        }
    }

    /** gRPC services get the tenant propagated over metadata (both server + client sides). */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor")
    static class GrpcTenancyConfiguration {

        @Bean
        @GrpcGlobalServerInterceptor
        GrpcTenantServerInterceptor grpcTenantServerInterceptor() {
            return new GrpcTenantServerInterceptor();
        }

        @Bean
        @GrpcGlobalClientInterceptor
        GrpcTenantClientInterceptor grpcTenantClientInterceptor() {
            return new GrpcTenantClientInterceptor();
        }
    }
}
