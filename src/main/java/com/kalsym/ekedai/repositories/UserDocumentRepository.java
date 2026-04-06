package com.kalsym.ekedai.repositories;

import com.kalsym.ekedai.model.UserDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserDocumentRepository extends JpaRepository<UserDocument, String>, JpaSpecificationExecutor<UserDocument> {
    List<UserDocument> findByUserId(String userId);
    List<UserDocument> findByUserIdAndStatus(String userId, UserDocument.UserDocumentStatus status);

}
