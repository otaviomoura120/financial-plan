package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.transaction.CreateTransactionService;
import com.devhouse.financial_plan.application.transaction.DeleteTransactionService;
import com.devhouse.financial_plan.application.transaction.ListTransactionsService;
import com.devhouse.financial_plan.application.transaction.UpdateTransactionService;
import com.devhouse.financial_plan.application.transaction.dto.CreateTransactionRequest;
import com.devhouse.financial_plan.application.transaction.dto.TransactionResponse;
import com.devhouse.financial_plan.application.transaction.dto.UpdateTransactionRequest;
import com.devhouse.financial_plan.domain.enums.TransactionType;
import com.devhouse.financial_plan.infrastructure.security.SecurityService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final CreateTransactionService createTransactionService;
    private final UpdateTransactionService updateTransactionService;
    private final DeleteTransactionService deleteTransactionService;
    private final ListTransactionsService listTransactionsService;

    public TransactionController(CreateTransactionService createTransactionService, UpdateTransactionService updateTransactionService, DeleteTransactionService deleteTransactionService, ListTransactionsService listTransactionsService) {
        this.createTransactionService = createTransactionService;
        this.updateTransactionService = updateTransactionService;
        this.deleteTransactionService = deleteTransactionService;
        this.listTransactionsService = listTransactionsService;
    }

    @GetMapping
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public List<TransactionResponse> list(@RequestHeader(SecurityService.SPACE_HEADER) Long spaceId,
                                           @RequestParam(required = false) Long userId,
                                           @RequestParam(required = false) Long bankAccountId,
                                           @RequestParam(required = false) Long categoryId,
                                           @RequestParam(required = false) Long subCategoryId,
                                           @RequestParam(required = false) TransactionType type,
                                           @RequestParam(required = false) LocalDate from,
                                           @RequestParam(required = false) LocalDate to,
                                           Authentication authentication, HttpServletRequest request) {
        return listTransactionsService.execute(spaceId, userId, bankAccountId, categoryId, subCategoryId,
                type, from, to);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public TransactionResponse create(@RequestHeader(SecurityService.SPACE_HEADER) Long spaceId,
                                      @RequestBody CreateTransactionRequest body, Authentication authentication, HttpServletRequest request) {
        return createTransactionService.execute(spaceId, authentication.getName(), body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public TransactionResponse update(@RequestHeader(SecurityService.SPACE_HEADER) Long spaceId, @PathVariable Long id,
                                      @RequestBody UpdateTransactionRequest body, Authentication authentication, HttpServletRequest request) {
        return updateTransactionService.execute(spaceId, id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityService.userHasPermissionForURL(authentication, #request)")
    public void delete(@RequestHeader(SecurityService.SPACE_HEADER) Long spaceId, @PathVariable Long id,
                       Authentication authentication, HttpServletRequest request) {
        deleteTransactionService.execute(spaceId, id);
    }
}
