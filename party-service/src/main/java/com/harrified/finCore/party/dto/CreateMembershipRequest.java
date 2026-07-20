/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.party.dto;

import com.harrified.finCore.party.domain.enums.MembershipRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Grant a login {@code subject} a {@code role} on a party. */
public record CreateMembershipRequest(
        @NotBlank @Size(max = 255) String subject,
        @NotNull MembershipRole role
) {
}
