/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.transfer.dto;

import com.harrified.finCore.transfer.domain.entity.Transfer;
import com.harrified.finCore.transfer.domain.enums.TransferStatus;

import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        UUID fromAccountId,
        UUID toAccountId,
        long amountMinor,
        String currency,
        TransferStatus status,
        UUID ledgerEntryId,
        String reference,
        Instant createdAt
) {

    public static TransferResponse from(Transfer t) {
        return new TransferResponse(
                t.getId(), t.getFromAccountId(), t.getToAccountId(), t.getAmountMinor(),
                t.getCurrency(), t.getStatus(), t.getLedgerEntryId(), t.getReference(), t.getCreatedAt());
    }
}
