package com.rawgul.controller;

import com.rawgul.dto.TransactionRequest;
import com.rawgul.dto.TransactionResponse;
import com.rawgul.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class TransactionController {
    
    private final TransactionService transactionService;
    
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TransactionRequest request) {
        log.info("POST /api/transactions - Creating transaction for user: {}", userDetails.getUsername());
        TransactionResponse response = transactionService.createTransaction(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAllTransactions(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("GET /api/transactions - Fetching all transactions for user: {}", userDetails.getUsername());
        List<TransactionResponse> transactions = transactionService.getUserTransactions(userDetails.getUsername());
        return ResponseEntity.ok(transactions);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransactionById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        log.info("GET /api/transactions/{} - Fetching transaction for user: {}", id, userDetails.getUsername());
        TransactionResponse response = transactionService.getTransactionById(userDetails.getUsername(), id);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request) {
        log.info("PUT /api/transactions/{} - Updating transaction for user: {}", id, userDetails.getUsername());
        TransactionResponse response = transactionService.updateTransaction(userDetails.getUsername(), id, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        log.info("DELETE /api/transactions/{} - Deleting transaction for user: {}", id, userDetails.getUsername());
        transactionService.deleteTransaction(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getUserStatistics(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("GET /api/transactions/statistics - Fetching statistics for user: {}", userDetails.getUsername());
        Map<String, Object> statistics = transactionService.getUserStatistics(userDetails.getUsername());
        return ResponseEntity.ok(statistics);
    }
}
