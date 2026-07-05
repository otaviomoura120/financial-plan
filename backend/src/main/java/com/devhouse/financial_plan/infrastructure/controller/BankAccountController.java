package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.bankaccount.CreateBankAccountService;
import com.devhouse.financial_plan.application.bankaccount.DeleteBankAccountService;
import com.devhouse.financial_plan.application.bankaccount.ListBankAccountsService;
import com.devhouse.financial_plan.application.bankaccount.UpdateBankAccountService;
import com.devhouse.financial_plan.application.bankaccount.dto.BankAccountResponse;
import com.devhouse.financial_plan.application.bankaccount.dto.CreateBankAccountRequest;
import com.devhouse.financial_plan.application.bankaccount.dto.UpdateBankAccountRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bank-accounts")
public class BankAccountController {

    private final CreateBankAccountService createBankAccountService;
    private final UpdateBankAccountService updateBankAccountService;
    private final DeleteBankAccountService deleteBankAccountService;
    private final ListBankAccountsService listBankAccountsService;

    public BankAccountController(CreateBankAccountService createBankAccountService, UpdateBankAccountService updateBankAccountService, DeleteBankAccountService deleteBankAccountService, ListBankAccountsService listBankAccountsService) {
        this.createBankAccountService = createBankAccountService;
        this.updateBankAccountService = updateBankAccountService;
        this.deleteBankAccountService = deleteBankAccountService;
        this.listBankAccountsService = listBankAccountsService;
    }

    @GetMapping
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public List<BankAccountResponse> list(@RequestParam Long spaceId, Authentication authentication, HttpServletRequest request) {
        return listBankAccountsService.execute(spaceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public BankAccountResponse create(@RequestBody CreateBankAccountRequest body, Authentication authentication, HttpServletRequest request) {
        return createBankAccountService.execute(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public BankAccountResponse update(@PathVariable Long id, @RequestBody UpdateBankAccountRequest body, Authentication authentication, HttpServletRequest request) {
        return updateBankAccountService.execute(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public void delete(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        deleteBankAccountService.execute(id);
    }
}
