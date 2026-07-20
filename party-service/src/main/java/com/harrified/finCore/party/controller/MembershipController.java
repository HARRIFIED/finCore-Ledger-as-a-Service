/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.party.controller;

import com.harrified.finCore.party.dto.AccessResponse;
import com.harrified.finCore.party.dto.CreateMembershipRequest;
import com.harrified.finCore.party.dto.MembershipResponse;
import com.harrified.finCore.party.service.MembershipService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    /** Grant a subject a role on a party. */
    @PostMapping("/parties/{partyId}/memberships")
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipResponse grant(@PathVariable UUID partyId, @Valid @RequestBody CreateMembershipRequest request) {
        return MembershipResponse.from(membershipService.grant(partyId, request.subject(), request.role()));
    }

    /** Who can act for this party. */
    @GetMapping("/parties/{partyId}/memberships")
    public List<MembershipResponse> listForParty(@PathVariable UUID partyId) {
        return membershipService.listForParty(partyId).stream().map(MembershipResponse::from).toList();
    }

    /** "May this subject act for this party?" — allowed + the roles they hold (ADR-0006). */
    @GetMapping("/parties/{partyId}/access")
    public AccessResponse access(@PathVariable UUID partyId, @RequestParam String subject) {
        return membershipService.access(partyId, subject);
    }

    /** What parties this subject can act for. */
    @GetMapping("/memberships")
    public List<MembershipResponse> listForSubject(@RequestParam String subject) {
        return membershipService.listForSubject(subject).stream().map(MembershipResponse::from).toList();
    }
}
