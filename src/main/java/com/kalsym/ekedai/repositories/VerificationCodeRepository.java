package com.kalsym.ekedai.repositories;
import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kalsym.ekedai.model.VerificationCode;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Integer> {

    List<VerificationCode> findByPhoneNumberAndExpiryGreaterThanEqual(@Param("phoneNumber") String phoneNumber, @Param("expiry") Date expiry);
    List<VerificationCode> findByEmailAndExpiryGreaterThanEqual(@Param("email") String email, @Param("expiry") Date expiry);

}
