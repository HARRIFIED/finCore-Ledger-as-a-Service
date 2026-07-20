/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.party.repository;

import com.harrified.finCore.party.domain.entity.PartyMembership;
import com.harrified.finCore.party.domain.enums.MembershipRole;
import com.harrified.finCore.party.domain.enums.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// All queries are tenant-scoped via Hibernate @TenantId (platform-tenancy).
public interface PartyMembershipRepository extends JpaRepository<PartyMembership, UUID> {

    List<PartyMembership> findByPartyIdAndStatus(UUID partyId, MembershipStatus status);

    List<PartyMembership> findBySubjectAndStatus(String subject, MembershipStatus status);

    List<PartyMembership> findByPartyIdAndSubjectAndStatus(UUID partyId, String subject, MembershipStatus status);

    Optional<PartyMembership> findByPartyIdAndSubjectAndRole(UUID partyId, String subject, MembershipRole role);
}
