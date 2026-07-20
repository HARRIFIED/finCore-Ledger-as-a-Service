/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.party.dto;

import com.harrified.finCore.party.domain.enums.PartyType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Create a party. Provide exactly the profile matching {@code type}: {@code individual} for
 * INDIVIDUAL, {@code organization} for ORGANIZATION (enforced in the service).
 */
public record CreatePartyRequest(
        @NotNull PartyType type,
        @Size(max = 200) String externalRef,
        @NotBlank @Size(max = 255) String displayName,
        @Valid Individual individual,
        @Valid Organization organization
) {

    public record Individual(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            LocalDate dateOfBirth,
            @Size(max = 3) String nationality
    ) {
    }

    public record Organization(
            @NotBlank @Size(max = 255) String legalName,
            @Size(max = 100) String registrationNumber,
            @Size(max = 100) String taxId
    ) {
    }
}
