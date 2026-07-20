/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.party.domain.entity;

import com.harrified.finCore.party.domain.enums.MembershipRole;
import com.harrified.finCore.party.domain.enums.MembershipStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;

/**
 * Links a login {@code subject} (the gateway's opaque {@code X-Subject}, from the federated
 * IdP) to a {@link Party} with a {@link MembershipRole} — finCore's authorization mapping
 * (ADR-0006). One row per (party, subject, role); a subject may hold several roles on a party.
 */
@Entity
@Table(name = "party_membership")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartyMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", length = 100, nullable = false)
    private String tenantId;

    @Column(name = "party_id", nullable = false, updatable = false)
    private UUID partyId;

    /** Opaque login id from the federated IdP (the gateway's X-Subject). */
    @Column(name = "subject", length = 255, nullable = false, updatable = false)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false, updatable = false)
    private MembershipRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private MembershipStatus status = MembershipStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
