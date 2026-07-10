/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.ledger.domain.entity;

import com.harrified.finCore.ledger.domain.enums.CoaType;
import com.harrified.finCore.ledger.domain.enums.LedgerAccountStatus;
import com.harrified.finCore.ledger.domain.enums.NormalSide;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * The unit balances are tracked for — a customer wallet or an internal/GL account. Single
 * tenant, single currency, one Chart-of-Accounts type. {@code tenantId} is Hibernate's
 * {@link TenantId} discriminator (row-level multi-tenancy, ADR-0009): auto-populated on
 * insert and auto-scoped on read by platform-tenancy's resolver.
 */
@Entity
@Table(name = "ledger_account")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", length = 100, nullable = false)
    private String tenantId;

    /** Business/external reference, unique within a tenant. */
    @Column(name = "reference", length = 100, nullable = false)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(name = "coa_type", length = 20, nullable = false)
    private CoaType coaType;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private LedgerAccountStatus status = LedgerAccountStatus.ACTIVE;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** The side on which this account's balance increases (derived from its CoA type). */
    public NormalSide normalSide() {
        return coaType.normalSide();
    }
}
