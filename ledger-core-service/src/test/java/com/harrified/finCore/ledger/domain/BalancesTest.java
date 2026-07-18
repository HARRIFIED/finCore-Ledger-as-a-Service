/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.ledger.domain;

import com.harrified.finCore.ledger.domain.enums.NormalSide;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BalancesTest {

    @Test
    void creditNormalIncreasesWithCredits() {
        assertEquals(50_000L, Balances.signed(NormalSide.CREDIT, 0, 50_000));
        assertEquals(-50_000L, Balances.signed(NormalSide.CREDIT, 50_000, 0));
    }

    @Test
    void debitNormalIncreasesWithDebits() {
        assertEquals(50_000L, Balances.signed(NormalSide.DEBIT, 50_000, 0));
        assertEquals(-50_000L, Balances.signed(NormalSide.DEBIT, 0, 50_000));
    }

    @Test
    void netsDebitsAndCredits() {
        assertEquals(30_000L, Balances.signed(NormalSide.CREDIT, 20_000, 50_000));
        assertEquals(-30_000L, Balances.signed(NormalSide.DEBIT, 20_000, 50_000));
    }
}
