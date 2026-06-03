package com.harrified.finCore.account.service;

import com.harrified.finCore.account.dto.AccountResponse;
import com.harrified.finCore.account.dto.CreateAccountRequest;
import com.harrified.finCore.account.dto.UpdateAccountRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.*;

public interface AccountService {
    AccountResponse createAccount(CreateAccountRequest request);
    AccountResponse getAccount(UUID id);
    AccountResponse getAccountByNumber(String accountNumber);
    Page<AccountResponse> listAccounts(UUID ownerId, Pageable pageable);
    AccountResponse updateAccount(UUID id, UpdateAccountRequest request);
    AccountResponse freezeAccount(UUID id, String reason);
    AccountResponse closeAccount(UUID id, String reason);
    void deleteAccount(UUID id);
}
