/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.party.dto;

import com.harrified.finCore.party.domain.entity.Party;
import com.harrified.finCore.party.domain.enums.PartyStatus;
import com.harrified.finCore.party.domain.enums.PartyType;

import java.time.LocalDate;
import java.util.UUID;

public record PartyResponse(
        UUID id,
        PartyType type,
        PartyStatus status,
        String externalRef,
        String displayName,
        Individual individual,
        Organization organization
) {

    public record Individual(String firstName, String lastName, LocalDate dateOfBirth, String nationality) {
    }

    public record Organization(String legalName, String registrationNumber, String taxId) {
    }

    public static PartyResponse from(Party p) {
        Individual individual = p.getType() == PartyType.INDIVIDUAL
                ? new Individual(p.getFirstName(), p.getLastName(), p.getDateOfBirth(), p.getNationality())
                : null;
        Organization organization = p.getType() == PartyType.ORGANIZATION
                ? new Organization(p.getLegalName(), p.getRegistrationNumber(), p.getTaxId())
                : null;
        return new PartyResponse(p.getId(), p.getType(), p.getStatus(), p.getExternalRef(),
                p.getDisplayName(), individual, organization);
    }
}
