/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.party.service;

import com.harrified.finCore.party.domain.entity.Party;
import com.harrified.finCore.party.domain.enums.PartyStatus;
import com.harrified.finCore.party.domain.enums.PartyType;
import com.harrified.finCore.party.dto.CreatePartyRequest;
import com.harrified.finCore.party.repository.PartyRepository;
import com.harrified.finCore.platform.outbox.OutboxAppender;
import com.harrified.finCore.platform.tenancy.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Application service for the Customer/Party master (CIF). */
@Service
public class PartyService {

    private static final String TOPIC = "party.created";

    private final PartyRepository parties;
    private final OutboxAppender outbox;

    public PartyService(PartyRepository parties, OutboxAppender outbox) {
        this.parties = parties;
        this.outbox = outbox;
    }

    @Transactional
    public Party create(CreatePartyRequest request) {
        validateProfileMatchesType(request);

        Party.PartyBuilder builder = Party.builder()
                .type(request.type())
                .status(PartyStatus.PROSPECT)
                .externalRef(request.externalRef())
                .displayName(request.displayName());

        if (request.type() == PartyType.INDIVIDUAL) {
            CreatePartyRequest.Individual i = request.individual();
            builder.firstName(i.firstName()).lastName(i.lastName())
                    .dateOfBirth(i.dateOfBirth()).nationality(i.nationality());
        } else {
            CreatePartyRequest.Organization o = request.organization();
            builder.legalName(o.legalName()).registrationNumber(o.registrationNumber()).taxId(o.taxId());
        }

        Party saved = parties.save(builder.build());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("partyId", saved.getId().toString());
        payload.put("type", saved.getType().name());
        payload.put("displayName", saved.getDisplayName());
        outbox.append(TenantContext.require(), TOPIC, saved.getId().toString(), TOPIC, payload);

        return saved;
    }

    @Transactional(readOnly = true)
    public Party get(UUID id) {
        return parties.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown party " + id));
    }

    private void validateProfileMatchesType(CreatePartyRequest request) {
        if (request.type() == PartyType.INDIVIDUAL) {
            if (request.individual() == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "individual details are required for an INDIVIDUAL party");
            }
            if (request.organization() != null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "organization details are not allowed for an INDIVIDUAL party");
            }
        } else {
            if (request.organization() == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "organization details are required for an ORGANIZATION party");
            }
            if (request.individual() != null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "individual details are not allowed for an ORGANIZATION party");
            }
        }
    }
}
