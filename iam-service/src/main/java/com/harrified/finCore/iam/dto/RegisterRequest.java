/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.iam.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        // Tenant — registration creates a new tenant + its first admin user
        @NotBlank @Size(max = 255) String tenantName,

        // Lowercase alphanumeric + hyphens only e.g. "acme-fintech"
        @NotBlank @Pattern(regexp = "^[a-z0-9-]{3,100}$", message = "Slug must be 3-100 lowercase alphanumeric characters or hyphens")
        String tenantSlug,

        // First admin user for the tenant
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        String phoneNumber
) {}
