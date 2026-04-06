package com.kalsym.ekedai.services;

import com.kalsym.ekedai.model.User;
import com.kalsym.ekedai.model.UserDocument;
import com.kalsym.ekedai.model.dao.UserDocumentRequest;
import com.kalsym.ekedai.repositories.UserDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class UserDocumentService {

    @Autowired
    private UserDocumentRepository userDocumentRepository;

    public UserDocument createUserDocument(UserDocumentRequest userDocumentRequest) {

        // Create a new UserDocument entity using the provided UserDocumentRequest
        UserDocument userDocument = new UserDocument(
                userDocumentRequest.getUserId(),
                userDocumentRequest.getDocumentType(),
                userDocumentRequest.getDocumentNumber(),
                userDocumentRequest.getIssueDate(),
                userDocumentRequest.getExpiryDate(),
                userDocumentRequest.getStatus(),
                userDocumentRequest.getImageId());

        // Save the UserDocument entity to the repository and return it
        return userDocumentRepository.save(userDocument);
    }

    public Page<UserDocument> getAllUserDocuments(String sortBy, String sortingOrder, int page, int pageSize,
            UserDocument.UserDocumentStatus status, String globalSearch,
            Date from, Date to, List<String> documentTypes) {
        Sort sort = Sort.by(sortingOrder.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, pageSize, sort);

        Specification<UserDocument> spec = Specification.where(filterByDateRange(from, to))
                .and(filterByStatus(status))
                .and(filterByGlobalSearch(globalSearch))
                .and((filterByDocumentType(documentTypes)));

        return userDocumentRepository.findAll(spec, pageable);
    }

    public Optional<UserDocument> getUserDocumentById(String id) {
        return userDocumentRepository.findById(id);
    }

    public List<UserDocument> getUserDocumentsByUserId(String userId) {
        return userDocumentRepository.findByUserId(userId);
    }

    public List<UserDocument> getUserDocumentsByUserIdAndDocumentTypes(String userId, List<String> documentTypes) {
        Specification<UserDocument> spec = Specification.where(filterByUserId(userId))
                .and((filterByDocumentType(documentTypes)));

        return userDocumentRepository.findAll(spec);
    }

    public List<UserDocument> getUserDocumentsByUserIdAndStatus(String userId, UserDocument.UserDocumentStatus status) {
        return userDocumentRepository.findByUserIdAndStatus(userId, status);
    }

    public UserDocument updateUserDocument(UserDocument userDocument) {
        return userDocumentRepository.save(userDocument);
    }

    public void deleteUserDocument(String id) {
        userDocumentRepository.deleteById(id);
    }

    public static Specification<UserDocument> filterByDateRange(Date from, Date to) {
        return (root, query, criteriaBuilder) -> {
            if (from != null && to != null) {
                return criteriaBuilder.between(root.get("createdDate"), from, to);
            } else if (from != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("createdDate"), from);
            } else if (to != null) {
                return criteriaBuilder.lessThanOrEqualTo(root.get("createdDate"), to);
            }
            // Returns a conjunction (which is essentially a no-op predicate)
            return criteriaBuilder.conjunction();
        };
    }

    public static Specification<UserDocument> filterByStatus(UserDocument.UserDocumentStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status != null) {
                return criteriaBuilder.equal(root.get("status"), status);
            }
            // Returns a conjunction (which is essentially a no-op predicate)
            return criteriaBuilder.conjunction();
        };
    }

    public static Specification<UserDocument> filterByUserId(String userId) {
        return (root, query, criteriaBuilder) -> {
            if (userId != null) {
                return criteriaBuilder.equal(root.get("userId"), userId);
            }
            // Returns a conjunction (which is essentially a no-op predicate)
            return criteriaBuilder.conjunction();
        };
    }

    public static Specification<UserDocument> filterByDocumentType(List<String> documentTypes) {
        return (root, query, criteriaBuilder) -> {
            if (documentTypes != null && !documentTypes.isEmpty()) {
                CriteriaBuilder.In<String> inClause = criteriaBuilder.in(root.get("documentType"));
                for (String documentType : documentTypes) {
                    inClause.value(documentType);
                }
                return inClause;
            }
            // Returns a conjunction (which is essentially a no-op predicate)
            return criteriaBuilder.conjunction();
        };
    }

    public static Specification<UserDocument> filterByGlobalSearch(String globalSearch) {
        return (root, query, criteriaBuilder) -> {
            if (StringUtils.hasText(globalSearch)) {
                String likePattern = "%" + globalSearch.toLowerCase() + "%";
                Join<UserDocument, User> user = root.join("user");
                return criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(user.get("fullName")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(user.get("email")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(user.get("phoneNumber")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(user.get("nationality")), likePattern));
            }
            // Returns a conjunction (which is essentially a no-op predicate)
            return criteriaBuilder.conjunction();
        };
    }
}
