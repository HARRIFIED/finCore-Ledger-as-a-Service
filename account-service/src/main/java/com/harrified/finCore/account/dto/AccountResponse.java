package com.harrified.finCore.account.dto;

import java.util.*;
import java.time.Instant;

import com.harrified.finCore.account.domain.enums.AccountStatus;
import com.harrified.finCore.account.domain.enums.AccountType;

public record AccountResponse(
    UUID id,
    String accountNumber,
    String accountName,
    String description,
    AccountType type,
    String currency,
    AccountStatus status,
    UUID ownerId,
    UUID parentAccountId,
    Map<String, Object> metadata,
    List<String> tags,
    Long version,
    Instant createdAt,
    Instant updatedAt
) {}
