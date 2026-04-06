package com.kalsym.internationalPayment.model.dao;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDate;

import com.kalsym.internationalPayment.model.UserDocument;

@Getter
@Setter
public class UserDocumentRequest {
    private String userId;

    private String documentType;

    private String documentNumber;

    private LocalDate issueDate;

    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    private UserDocument.UserDocumentStatus status;

    private String imageId;

}
