/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.ledger.domain.enums;

/**
 * Chart-of-Accounts account type. Each type fixes the account's {@link NormalSide} — the
 * side on which its balance increases — which drives balance normalization.
 */
public enum CoaType {
    ASSET(NormalSide.DEBIT),
    LIABILITY(NormalSide.CREDIT),
    EQUITY(NormalSide.CREDIT),
    INCOME(NormalSide.CREDIT),
    EXPENSE(NormalSide.DEBIT);

    private final NormalSide normalSide;

    CoaType(NormalSide normalSide) {
        this.normalSide = normalSide;
    }

    public NormalSide normalSide() {
        return normalSide;
    }
}
