package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.transaction.CreateTransactionService;
import com.devhouse.financial_plan.application.transaction.DeleteTransactionService;
import com.devhouse.financial_plan.application.transaction.UpdateTransactionService;
import com.devhouse.financial_plan.application.transaction.dto.CreateTransactionRequest;
import com.devhouse.financial_plan.application.transaction.dto.TransactionResponse;
import com.devhouse.financial_plan.application.transaction.dto.UpdateTransactionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final CreateTransactionService createTransactionService;
    private final UpdateTransactionService updateTransactionService;
    private final DeleteTransactionService deleteTransactionService;

    public TransactionController(CreateTransactionService createTransactionService, UpdateTransactionService updateTransactionService, DeleteTransactionService deleteTransactionService) {
        this.createTransactionService = createTransactionService;
        this.updateTransactionService = updateTransactionService;
        this.deleteTransactionService = deleteTransactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@RequestBody CreateTransactionRequest request) {
        return createTransactionService.execute(request);
    }

    @PutMapping("/{id}")
    public TransactionResponse update(@PathVariable Long id, @RequestBody UpdateTransactionRequest request) {
        return updateTransactionService.execute(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteTransactionService.execute(id);
    }
}
