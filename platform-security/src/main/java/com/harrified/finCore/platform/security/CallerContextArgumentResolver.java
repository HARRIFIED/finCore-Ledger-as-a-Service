/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.platform.security;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

/**
 * Resolves a {@link CallerContext} controller argument from the gateway-injected
 * identity headers ({@link GatewayHeaders}).
 *
 * <p>These headers are gateway-authoritative: the gateway strips any client-supplied
 * copies and re-sets them from the verified token. A missing {@code X-Subject} therefore
 * means the request did not pass through the gateway → {@code 401 Unauthorized}.
 *
 * <p>Registered automatically for servlet web apps by {@link GatewayIdentityAutoConfiguration}.
 */
public class CallerContextArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return CallerContext.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        String subject = webRequest.getHeader(GatewayHeaders.SUBJECT);
        if (subject == null || subject.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Missing " + GatewayHeaders.SUBJECT + " — request did not pass through the API gateway");
        }

        String programId = trimToNull(webRequest.getHeader(GatewayHeaders.PROGRAM_ID));

        String rawScopes = webRequest.getHeader(GatewayHeaders.SCOPES);
        List<String> scopes = (rawScopes == null || rawScopes.isBlank())
                ? List.of()
                : Arrays.stream(rawScopes.trim().split("\\s+")).toList();

        return new CallerContext(subject.trim(), programId, scopes);
    }

    private static String trimToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
