/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.ledger.domain;

import com.harrified.finCore.ledger.domain.enums.NormalSide;

/** Balance math for the event-sourced ledger (ADR-0005). Pure and side-effect-free. */
public final class Balances {

    private Balances() {
    }

    /**
     * Normalize a debit/credit fold into a signed balance by the account's normal side:
     * a debit-normal account (asset/expense) increases with debits; a credit-normal account
     * (liability/equity/income) increases with credits. Amounts are integer minor units.
     */
    public static long signed(NormalSide normalSide, long debits, long credits) {
        return normalSide == NormalSide.DEBIT ? debits - credits : credits - debits;
    }
}
