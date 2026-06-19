/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.iam.dto;

import com.harrified.finCore.iam.domain.entity.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        UUID tenantId,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        AddressDto address,
        String role,
        String kycStatus,
        String status,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getTenantId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                AddressDto.from(user.getAddress()),
                user.getRole().name(),
                user.getKycStatus().name(),
                user.getStatus().name(),
                user.getCreatedAt()
        );
    }
}
