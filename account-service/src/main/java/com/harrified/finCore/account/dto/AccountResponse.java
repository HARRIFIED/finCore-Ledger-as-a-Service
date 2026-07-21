/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.account.dto;

import com.harrified.finCore.account.domain.entity.Account;
import com.harrified.finCore.account.domain.enums.AccountStatus;
import com.harrified.finCore.account.domain.enums.AccountType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountNumber,
        String accountName,
        String description,
        AccountType type,
        String currency,
        AccountStatus status,
        UUID partyId,
        UUID ledgerAccountId,
        UUID parentAccountId,
        String metadata,
        List<String> tags,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {

    public static AccountResponse from(Account a) {
        return new AccountResponse(
                a.getId(), a.getAccountNumber(), a.getAccountName(), a.getDescription(),
                a.getType(), a.getCurrency().name(), a.getStatus(), a.getPartyId(), a.getLedgerAccountId(),
                a.getParentAccountId(), a.getMetadata(), List.copyOf(a.getTags()),
                a.getVersion(), a.getCreatedAt(), a.getUpdatedAt());
    }
}
