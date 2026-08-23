/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.transfer.controller;

import com.harrified.finCore.platform.security.CallerContext;
import com.harrified.finCore.transfer.dto.CreateTransferRequest;
import com.harrified.finCore.transfer.dto.TransferResponse;
import com.harrified.finCore.transfer.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    /**
     * Move funds between two customer accounts. The caller (from the gateway-injected identity)
     * must hold an OWNER/ADMIN/INITIATOR membership on the source account's party.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse transfer(CallerContext caller, @Valid @RequestBody CreateTransferRequest request) {
        return transferService.transfer(caller, request);
    }

    @GetMapping("/{id}")
    public TransferResponse get(@PathVariable UUID id) {
        return transferService.getTransfer(id);
    }
}
