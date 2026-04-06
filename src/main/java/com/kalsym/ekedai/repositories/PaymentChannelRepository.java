package com.kalsym.ekedai.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kalsym.ekedai.model.PaymentChannel;

@Repository
public interface PaymentChannelRepository extends JpaRepository<PaymentChannel, Long> {

    List<PaymentChannel> findByStatus(Boolean status);
}
