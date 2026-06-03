package com.harrified.finCore.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.harrified.finCore.account.domain.enums.AccountType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreateAccountRequest(
    @NotBlank @Size(max = 255) String accountName,
    String description,
    @NotNull AccountType type,
    @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code") String currency,
    @NotNull UUID ownerId,
    UUID parentAccountId,
    Map<String, Object> metadata,
    List<String> tags
) {}
