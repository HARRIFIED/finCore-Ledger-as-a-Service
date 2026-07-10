/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.ledger.dto;

import com.harrified.finCore.ledger.domain.enums.Direction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PostEntryRequest(
        @NotBlank @Size(max = 200) String idempotencyKey,
        @NotNull LocalDate valueDate,
        @Size(max = 100) String correlationId,
        @Size(max = 100) String source,
        @NotEmpty @Valid List<PostingDto> postings
) {

    public record PostingDto(
            @NotNull UUID ledgerAccountId,
            @NotNull Direction direction,
            @Positive long amountMinor,
            @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code") String currency
    ) {
    }
}
