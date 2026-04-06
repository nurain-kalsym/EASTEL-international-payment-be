package com.kalsym.ekedai.repositories;

import com.kalsym.ekedai.controller.DashboardController;
import com.kalsym.ekedai.model.Transaction;
import com.kalsym.ekedai.model.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    Optional<Transaction> findByTransactionId(String transactionId);

    Page<Transaction> findAll(Specification<Transaction> transactionHistoryByUser, Pageable pageable);

    // @Query(value = "SELECT t.* FROM ekedai_production.transaction t WHERE
    // t.paymentStatus = :paymentStatus AND t.status = :status ", nativeQuery =
    // true)
    List<Transaction> findByPaymentStatusAndStatus(@Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("status") String status);

    // @Query(value = "SELECT t.* FROM ekedai_production.transaction t WHERE
    // t.paymentMethod =:paymentMethod and t.paymentStatus = :paymentStatus AND
    // t.status = :status ", nativeQuery = true)
    List<Transaction> findByPaymentMethodAndPaymentStatusAndStatus(@Param("paymentMethod") String paymentMethod,
            @Param("paymentStatus") PaymentStatus paymentStatus, @Param("status") String status);

    List<Transaction> findByPaidDateBetweenAndPaymentChannelStartingWithAndPaymentStatus(Date startDate, Date endDate,
            String paymentChannelPattern, PaymentStatus paymentStatus);

    long countByStatusAndCreatedDateBetween(String status, Date startDate, Date endDate);

    long countByPaymentStatusAndCreatedDateBetween(PaymentStatus paymentStatus, Date startDate, Date endDate);

    long countByStatusAndPaymentStatusAndCreatedDateBetween(String status, PaymentStatus paymentStatus, Date startDate,
            Date endDate);

    @Query("SELECT t FROM Transaction t WHERE t.createdDate < :cutoff and t.status = 'PENDING' and t.paymentStatus = 'PENDING'")
    List<Transaction> findStaleTransactions(@Param("cutoff") Date cutoff);

    List<Transaction> findAll(Specification<Transaction> transactionHistoryByUser);

    List<Transaction> findByUserId(String userId);

//    List<Transaction> findByUserIdAndProductIdIsNotNullAndProductVariantIdIsNotNull(String userId);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.userId = :userId AND t.status = :status AND t.createdDate BETWEEN :startDate AND :endDate AND t.id <> :currentId")
    long countByUserIdAndStatusAndCreatedDateBetweenExcludingTransaction(
            @Param("userId") String userId,
            @Param("status") String status,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate,
            @Param("currentId") String currentId
    );

    @Query(value = "SELECT t.status AS status, COUNT(*) AS count " +
            "FROM transaction t " +
            "WHERE t.userId = :userId " +
            "  AND t.productId IS NOT NULL " +
            "  AND t.productVariantId IS NOT NULL " +
            "GROUP BY t.status",
            nativeQuery = true)
    List<Object[]> getTransactionCountsByUser(@Param("userId") String userId);



}
