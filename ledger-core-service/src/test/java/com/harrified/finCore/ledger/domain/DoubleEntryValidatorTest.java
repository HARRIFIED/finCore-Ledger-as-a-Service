/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.ledger.domain;

import com.harrified.finCore.ledger.domain.enums.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DoubleEntryValidatorTest {

    private static PostingLine debit(long amount, String currency) {
        return new PostingLine(UUID.randomUUID(), Direction.DEBIT, amount, currency);
    }

    private static PostingLine credit(long amount, String currency) {
        return new PostingLine(UUID.randomUUID(), Direction.CREDIT, amount, currency);
    }

    @Test
    void balancedSingleCurrencyPasses() {
        assertDoesNotThrow(() -> DoubleEntryValidator.validate(List.of(
                debit(500, "USD"), credit(500, "USD"))));
    }

    @Test
    void multiCurrencyBalancedPerCurrencyPasses() {
        // USD legs net to 0 AND NGN legs net to 0 — a valid FX-style entry.
        assertDoesNotThrow(() -> DoubleEntryValidator.validate(List.of(
                debit(100_00, "USD"), credit(100_00, "USD"),
                debit(160_000_00, "NGN"), credit(160_000_00, "NGN"))));
    }

    @Test
    void unbalancedSingleCurrencyFails() {
        assertThrows(UnbalancedEntryException.class, () -> DoubleEntryValidator.validate(List.of(
                debit(500, "USD"), credit(400, "USD"))));
    }

    @Test
    void imbalanceInOneCurrencyOfAMultiCurrencyEntryFails() {
        assertThrows(UnbalancedEntryException.class, () -> DoubleEntryValidator.validate(List.of(
                debit(100_00, "USD"), credit(100_00, "USD"),
                debit(160_000_00, "NGN"), credit(159_000_00, "NGN"))));
    }

    @Test
    void fewerThanTwoPostingsFails() {
        assertThrows(UnbalancedEntryException.class, () -> DoubleEntryValidator.validate(List.of(
                debit(500, "USD"))));
        assertThrows(UnbalancedEntryException.class, () -> DoubleEntryValidator.validate(List.of()));
    }

    @Test
    void nonPositiveAmountFails() {
        assertThrows(UnbalancedEntryException.class, () -> DoubleEntryValidator.validate(List.of(
                debit(0, "USD"), credit(0, "USD"))));
    }
}
