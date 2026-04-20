package com.kalsym.internationalPayment.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kalsym.internationalPayment.model.Wallet;
import com.kalsym.internationalPayment.model.enums.Status;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, String> {

    Optional<Wallet> findByUserIdAndStatus(String id, Status active);

    Optional<Wallet> findByUserId(String userId);

}
