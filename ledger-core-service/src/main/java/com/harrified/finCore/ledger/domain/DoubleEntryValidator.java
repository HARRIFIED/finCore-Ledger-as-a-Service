/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.ledger.domain;

import com.harrified.finCore.ledger.domain.enums.Direction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enforces the double-entry invariant on a proposed set of postings:
 * <ul>
 *   <li>at least two postings;</li>
 *   <li>every amount is positive;</li>
 *   <li>for <b>each currency</b>, the sum of debits equals the sum of credits.</li>
 * </ul>
 * Per-currency balancing is what makes a single multi-currency entry valid (FX via position
 * accounts — see fx-and-chart-of-accounts). Pure and side-effect-free.
 */
public final class DoubleEntryValidator {

    private DoubleEntryValidator() {
    }

    public static void validate(List<PostingLine> lines) {
        if (lines == null || lines.size() < 2) {
            throw new UnbalancedEntryException("A journal entry needs at least two postings");
        }

        Map<String, Long> netByCurrency = new HashMap<>();
        for (PostingLine line : lines) {
            if (line.amountMinor() <= 0) {
                throw new UnbalancedEntryException("Posting amount must be positive, was " + line.amountMinor());
            }
            long signed = (line.direction() == Direction.DEBIT) ? line.amountMinor() : -line.amountMinor();
            netByCurrency.merge(line.currency(), signed, Math::addExact);
        }

        for (Map.Entry<String, Long> net : netByCurrency.entrySet()) {
            if (net.getValue() != 0L) {
                throw new UnbalancedEntryException(
                        "Entry not balanced for currency " + net.getKey() + " (net " + net.getValue() + " minor units)");
            }
        }
    }
}
