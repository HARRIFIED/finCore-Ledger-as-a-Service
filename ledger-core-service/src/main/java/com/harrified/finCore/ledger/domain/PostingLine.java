/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.ledger.domain;

import com.harrified.finCore.ledger.domain.enums.Direction;

import java.util.UUID;

/**
 * An intended posting leg (a command), independent of persistence: which ledger account,
 * which direction, how much (integer minor units), in what currency.
 */
public record PostingLine(UUID ledgerAccountId, Direction direction, long amountMinor, String currency) {
}
