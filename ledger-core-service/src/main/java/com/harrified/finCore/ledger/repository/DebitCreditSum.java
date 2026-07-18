/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.ledger.repository;

/** Projection for a debit/credit fold over an account's postings (both in minor units). */
public interface DebitCreditSum {

    Long getDebits();

    Long getCredits();
}
