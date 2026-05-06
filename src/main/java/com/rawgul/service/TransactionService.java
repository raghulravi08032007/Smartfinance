package com.rawgul.service;

import com.rawgul.dto.TransactionRequest;
import com.rawgul.dto.TransactionResponse;
import com.rawgul.entity.Transaction;
import com.rawgul.entity.TransactionType;
import com.rawgul.model.User;
import com.rawgul.exceptions.ResourceNotFoundException;
import com.rawgul.repository.TransactionRepository;
import com.rawgul.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public TransactionResponse createTransaction(String username, TransactionRequest request) {
        log.info("Creating transaction for user: {}", username);
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setDescription(request.getDescription());
        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setDate(request.getDate());
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Transaction created successfully with ID: {}", savedTransaction.getId());
        
        return mapToResponse(savedTransaction);
    }
    
    @Transactional(readOnly = true)
    public List<TransactionResponse> getUserTransactions(String username) {
        log.info("Fetching all transactions for user: {}", username);
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        List<Transaction> transactions = transactionRepository.findByUserIdOrderByDateDesc(user.getId());
        log.info("Found {} transactions for user: {}", transactions.size(), username);
        
        return transactions.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(String username, Long transactionId) {
        log.info("Fetching transaction with ID: {} for user: {}", transactionId, username);
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId));
        
        // Verify that the transaction belongs to the user
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Transaction not found with ID: " + transactionId);
        }
        
        return mapToResponse(transaction);
    }
    
    @Transactional
    public TransactionResponse updateTransaction(String username, Long transactionId, TransactionRequest request) {
        log.info("Updating transaction with ID: {} for user: {}", transactionId, username);
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId));
        
        // Verify that the transaction belongs to the user
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Transaction not found with ID: " + transactionId);
        }
        
        transaction.setDescription(request.getDescription());
        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setDate(request.getDate());
        
        Transaction updatedTransaction = transactionRepository.save(transaction);
        log.info("Transaction updated successfully with ID: {}", updatedTransaction.getId());
        
        return mapToResponse(updatedTransaction);
    }
    
    @Transactional
    public void deleteTransaction(String username, Long transactionId) {
        log.info("Deleting transaction with ID: {} for user: {}", transactionId, username);
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId));
        
        // Verify that the transaction belongs to the user
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Transaction not found with ID: " + transactionId);
        }
        
        transactionRepository.delete(transaction);
        log.info("Transaction deleted successfully with ID: {}", transactionId);
    }
    
    @Transactional(readOnly = true)
    public Map<String, Object> getUserStatistics(String username) {
        log.info("Calculating statistics for user: {}", username);
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        
        Double totalIncome = transactionRepository.sumAmountByUserIdAndType(user.getId(), TransactionType.INCOME);
        Double totalExpense = transactionRepository.sumAmountByUserIdAndType(user.getId(), TransactionType.EXPENSE);
        
        totalIncome = totalIncome != null ? totalIncome : 0.0;
        totalExpense = totalExpense != null ? totalExpense : 0.0;
        Double balance = totalIncome - totalExpense;
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalIncome", totalIncome);
        statistics.put("totalExpense", totalExpense);
        statistics.put("balance", balance);
        
        log.info("Statistics calculated for user {}: Income={}, Expense={}, Balance={}", 
            username, totalIncome, totalExpense, balance);
        
        return statistics;
    }
    
    private TransactionResponse mapToResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setDescription(transaction.getDescription());
        response.setAmount(transaction.getAmount());
        response.setType(transaction.getType());
        response.setDate(transaction.getDate());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setUpdatedAt(transaction.getUpdatedAt());
        return response;
    }
}
