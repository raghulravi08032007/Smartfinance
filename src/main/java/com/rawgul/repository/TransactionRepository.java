package com.rawgul.repository;

import com.rawgul.entity.Transaction;
import com.rawgul.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    List<Transaction> findByUserIdOrderByDateDesc(Long userId);
    
    List<Transaction> findByUserIdAndDateBetweenOrderByDateDesc(
        Long userId, 
        LocalDate startDate, 
        LocalDate endDate
    );
    
    List<Transaction> findByUserIdAndTypeOrderByDateDesc(
        Long userId, 
        TransactionType type
    );
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.type = :type")
    Double sumAmountByUserIdAndType(
        @Param("userId") Long userId, 
        @Param("type") TransactionType type
    );
    
    void deleteByUserId(Long userId);
}
