/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.ledger.dto;

import java.util.UUID;

/**
 * A ledger account's balances, in the account's currency minor units (FR-3).
 * {@code available = posted - held}. {@code held} and {@code pending} are 0 until holds
 * (Phase 4) and external settlement (Phase 7) land.
 */
public record BalanceResponse(
        UUID accountId,
        String currency,
        long posted,
        long available,
        long held,
        long pending
) {
}
