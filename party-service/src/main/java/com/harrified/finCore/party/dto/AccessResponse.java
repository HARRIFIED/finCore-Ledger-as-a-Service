/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.party.dto;

import com.harrified.finCore.party.domain.enums.MembershipRole;

import java.util.List;
import java.util.UUID;

/**
 * Answer to "may this subject act for this party?" — the facts a calling service needs to
 * enforce authorization (ADR-0006). {@code allowed} is true when the subject has any active
 * membership; {@code roles} lets the caller check for a specific role (e.g. INITIATOR).
 */
public record AccessResponse(
        UUID partyId,
        String subject,
        boolean allowed,
        List<MembershipRole> roles
) {
}
