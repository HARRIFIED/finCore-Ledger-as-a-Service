/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */
package com.harrified.finCore.iam.dto;

import jakarta.validation.constraints.Size;

// All fields are optional — only non-null values are applied (PATCH semantics).
// Email, role, and password are updated via separate dedicated endpoints.
public record UpdateUserRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 30)  String phoneNumber,
        AddressDto address
) {}
