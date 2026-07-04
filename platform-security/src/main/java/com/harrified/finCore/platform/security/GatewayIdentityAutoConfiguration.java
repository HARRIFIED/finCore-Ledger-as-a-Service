/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.platform.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Registers {@link CallerContextArgumentResolver} for any servlet web service that
 * adds the {@code platform-security} module — so controllers can inject a
 * {@link CallerContext} with no per-service wiring.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class GatewayIdentityAutoConfiguration implements WebMvcConfigurer {

    private final CallerContextArgumentResolver callerContextArgumentResolver =
            new CallerContextArgumentResolver();

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(callerContextArgumentResolver);
    }
}
