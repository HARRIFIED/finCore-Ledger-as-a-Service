/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.party.dto;

import com.harrified.finCore.party.domain.entity.PartyMembership;
import com.harrified.finCore.party.domain.enums.MembershipRole;
import com.harrified.finCore.party.domain.enums.MembershipStatus;

import java.util.UUID;

public record MembershipResponse(
        UUID id,
        UUID partyId,
        String subject,
        MembershipRole role,
        MembershipStatus status
) {

    public static MembershipResponse from(PartyMembership m) {
        return new MembershipResponse(m.getId(), m.getPartyId(), m.getSubject(), m.getRole(), m.getStatus());
    }
}
