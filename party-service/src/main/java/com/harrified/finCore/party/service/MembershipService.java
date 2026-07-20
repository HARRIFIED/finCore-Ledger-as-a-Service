/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.party.service;

import com.harrified.finCore.party.domain.entity.PartyMembership;
import com.harrified.finCore.party.domain.enums.MembershipRole;
import com.harrified.finCore.party.domain.enums.MembershipStatus;
import com.harrified.finCore.party.dto.AccessResponse;
import com.harrified.finCore.party.repository.PartyMembershipRepository;
import com.harrified.finCore.party.repository.PartyRepository;
import com.harrified.finCore.platform.outbox.OutboxAppender;
import com.harrified.finCore.platform.tenancy.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the {@code subject → party} authorization mapping (ADR-0006). Provides the facts a
 * calling service uses to enforce "may this subject act for this party?"; the enforcement
 * decision (which role an action needs) stays with the caller.
 */
@Service
public class MembershipService {

    private static final String TOPIC = "party.membership.granted";

    private final PartyMembershipRepository memberships;
    private final PartyRepository parties;
    private final OutboxAppender outbox;

    public MembershipService(PartyMembershipRepository memberships, PartyRepository parties, OutboxAppender outbox) {
        this.memberships = memberships;
        this.parties = parties;
        this.outbox = outbox;
    }

    /** Grant a subject a role on a party. Idempotent per (party, subject, role). */
    @Transactional
    public PartyMembership grant(UUID partyId, String subject, MembershipRole role) {
        if (parties.findById(partyId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown party " + partyId);
        }

        return memberships.findByPartyIdAndSubjectAndRole(partyId, subject, role)
                .orElseGet(() -> {
                    PartyMembership saved = memberships.save(PartyMembership.builder()
                            .partyId(partyId)
                            .subject(subject)
                            .role(role)
                            .status(MembershipStatus.ACTIVE)
                            .build());

                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("partyId", partyId.toString());
                    payload.put("subject", subject);
                    payload.put("role", role.name());
                    outbox.append(TenantContext.require(), TOPIC, partyId.toString(), TOPIC, payload);

                    return saved;
                });
    }

    @Transactional(readOnly = true)
    public List<PartyMembership> listForParty(UUID partyId) {
        return memberships.findByPartyIdAndStatus(partyId, MembershipStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<PartyMembership> listForSubject(String subject) {
        return memberships.findBySubjectAndStatus(subject, MembershipStatus.ACTIVE);
    }

    /** The facts for "may {@code subject} act for {@code partyId}?" — allowed + granted roles. */
    @Transactional(readOnly = true)
    public AccessResponse access(UUID partyId, String subject) {
        List<MembershipRole> roles = memberships
                .findByPartyIdAndSubjectAndStatus(partyId, subject, MembershipStatus.ACTIVE)
                .stream()
                .map(PartyMembership::getRole)
                .toList();
        return new AccessResponse(partyId, subject, !roles.isEmpty(), roles);
    }
}
