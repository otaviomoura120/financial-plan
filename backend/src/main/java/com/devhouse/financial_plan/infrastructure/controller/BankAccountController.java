package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.bankaccount.CreateBankAccountService;
import com.devhouse.financial_plan.application.bankaccount.DeleteBankAccountService;
import com.devhouse.financial_plan.application.bankaccount.ListBankAccountsService;
import com.devhouse.financial_plan.application.bankaccount.UpdateBankAccountService;
import com.devhouse.financial_plan.application.bankaccount.dto.BankAccountResponse;
import com.devhouse.financial_plan.application.bankaccount.dto.CreateBankAccountRequest;
import com.devhouse.financial_plan.application.bankaccount.dto.UpdateBankAccountRequest;
import org.springframework.http.HttpStatus;
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
    public List<BankAccountResponse> list(@RequestParam Long spaceId) {
        return listBankAccountsService.execute(spaceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BankAccountResponse create(@RequestBody CreateBankAccountRequest request) {
        return createBankAccountService.execute(request);
    }

    @PutMapping("/{id}")
    public BankAccountResponse update(@PathVariable Long id, @RequestBody UpdateBankAccountRequest request) {
        return updateBankAccountService.execute(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteBankAccountService.execute(id);
    }
}
