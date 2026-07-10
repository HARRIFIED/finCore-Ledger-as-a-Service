/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.ledger.dto;

import com.harrified.finCore.ledger.domain.enums.CoaType;
import com.harrified.finCore.ledger.domain.enums.LedgerAccountStatus;

import java.util.UUID;

public record LedgerAccountResponse(
        UUID id,
        String reference,
        CoaType coaType,
        String currency,
        LedgerAccountStatus status
) {
}
