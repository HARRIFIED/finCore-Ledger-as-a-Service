/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.account.domain.entity;

import com.harrified.finCore.account.domain.enums.AccountCurrency;
import com.harrified.finCore.account.domain.enums.AccountStatus;
import com.harrified.finCore.account.domain.enums.AccountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A customer account (product/wallet). Owned by a {@code partyId} (the CIF party, ADR-0006)
 * and backed by a ledger account in ledger-core ({@code ledgerAccountId}). {@code tenantId}
 * is Hibernate's {@link TenantId} discriminator (row-level tenancy, ADR-0009).
 */
@Entity
@Table(name = "accounts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", length = 100, nullable = false)
    private String tenantId;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "account_name", nullable = false, length = 255)
    private String accountName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    @Builder.Default
    private AccountType type = AccountType.SAVINGS;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private AccountCurrency currency = AccountCurrency.NGN;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private AccountStatus status = AccountStatus.INACTIVE;

    /** The owning party (CIF), not the login user (ADR-0006). */
    @Column(name = "party_id", nullable = false)
    private UUID partyId;

    /** The backing ledger account in ledger-core. */
    @Column(name = "ledger_account_id", nullable = false)
    private UUID ledgerAccountId;

    @Column(name = "parent_account_id")
    private UUID parentAccountId;

    // @JdbcTypeCode(JSON) so Hibernate binds the String as jsonb, not varchar.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @ElementCollection
    @CollectionTable(name = "account_tags", joinColumns = @JoinColumn(name = "account_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
