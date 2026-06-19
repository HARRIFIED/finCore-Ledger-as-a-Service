/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.account.domain.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.harrified.finCore.account.domain.enums.AccountCurrency;
import com.harrified.finCore.account.domain.enums.AccountStatus;
import com.harrified.finCore.account.domain.enums.AccountType;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import java.util.List;

@Entity
@Table(name = "accounts")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name="id", nullable=false, updatable = false)
    private UUID id;

    @Column(name = "accountNumber", nullable = false, unique = true, length = 50)
    private String accountNumber;

    @Column(name = "accountName", nullable = false, length = 255)
    private String accountName;

    @Column(name = "description", nullable = true, columnDefinition = "TEXT")
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

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name="tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "parent_account_id") // for future puposes
    private UUID parentAccountId;

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

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
