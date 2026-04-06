package com.kalsym.internationalPayment.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;

import jakarta.persistence.*;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_documents")
@Getter
@Setter
public class UserDocument {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    private String userId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", referencedColumnName = "id", updatable = false, insertable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private User user;

    @Column(name = "documentType", nullable = false)
    private String documentType;

    @Column(name = "documentNumber", nullable = false)
    private String documentNumber;

    private LocalDate issueDate;

    private LocalDate expiryDate;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private UserDocumentStatus status;

    private String imageId;

    @OneToOne()
    @JoinColumn(name = "imageId", referencedColumnName = "id", insertable = false, updatable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private ImageAssets imageDetails;

    @Column(name = "createdAt")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updatedAt")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public UserDocument(String userId, String documentType, String documentNumber, LocalDate issueDate, LocalDate expiryDate, UserDocumentStatus status, String imageId) {
        this.userId = userId;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.status = status != null ? status : UserDocumentStatus.PENDING_UPLOAD;
        this.imageId = imageId;
    }

    // Default constructor
    public UserDocument() {
    }

    public enum UserDocumentStatus {
        PENDING_UPLOAD, VERIFIED, REJECTED, PENDING_VERIFICATION
    }

}

