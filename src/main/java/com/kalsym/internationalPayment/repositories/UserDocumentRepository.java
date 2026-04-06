package com.kalsym.internationalPayment.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.kalsym.internationalPayment.model.UserDocument;

import java.util.List;

@Repository
public interface UserDocumentRepository extends JpaRepository<UserDocument, String>, JpaSpecificationExecutor<UserDocument> {
    List<UserDocument> findByUserId(String userId);
    List<UserDocument> findByUserIdAndStatus(String userId, UserDocument.UserDocumentStatus status);

}
