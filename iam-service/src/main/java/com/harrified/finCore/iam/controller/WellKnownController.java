/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.iam.controller;

import com.harrified.finCore.iam.security.JwtService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/.well-known")
public class WellKnownController {

    private final JwtService jwtService;

    public WellKnownController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    // Returns the JWKS (JSON Web Key Set) containing the RSA public key.
    // The API Gateway calls this endpoint to verify JWT signatures without
    // making a per-request call to iam-service.
    @GetMapping(value = "/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> jwks() {
        return jwtService.buildJwks();
    }
}
