package com.kalsym.internationalPayment.repositories;

import java.util.Date;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kalsym.internationalPayment.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(@Param("email") String email);

    Optional<User> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    Optional<User> findByRole(@Param("role") String role);

    Page<User> findAll(Specification<User> userSpecification, Pageable pageable);

    long countByCreatedBetween(Date startDate, Date endDate);
}
