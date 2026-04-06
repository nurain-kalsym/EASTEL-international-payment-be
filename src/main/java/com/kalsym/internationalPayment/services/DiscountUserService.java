package com.kalsym.internationalPayment.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.kalsym.internationalPayment.InternationalPaymentApplication;
import com.kalsym.internationalPayment.model.Discount;
import com.kalsym.internationalPayment.model.DiscountProductVariant;
import com.kalsym.internationalPayment.model.DiscountUser;
import com.kalsym.internationalPayment.model.DiscountUserId;
import com.kalsym.internationalPayment.model.dao.*;
import com.kalsym.internationalPayment.model.enums.DiscountUserStatus;
import com.kalsym.internationalPayment.repositories.DiscountRepository;
import com.kalsym.internationalPayment.repositories.DiscountUserRepository;
import com.kalsym.internationalPayment.utility.HttpResponse;
import com.kalsym.internationalPayment.utility.Logger;
import com.kalsym.internationalPayment.utility.MsisdnUtil;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DiscountUserService {

    @Autowired
    DiscountUserRepository discountUserRepository;

    @Autowired
    DiscountRepository discountRepository;

    @Autowired
    DiscountProductVariantService discountProductVariantService;

    public Optional<DiscountUser> getDiscountUserById(DiscountUserId id) {
        return discountUserRepository.findById(id);
    }

    public List<DiscountUser> getByDiscountId(String discountId) {
        return discountUserRepository.getByDiscountId(discountId);
    }

    public List<DiscountUser> getByDiscountUserByUserPhoneNumber(String userPhoneNumber) {
        return discountUserRepository.getDiscountUserByUserPhoneNumber(userPhoneNumber);
    }

    public List<DiscountUser> getDiscountUserByStatus(DiscountUserStatus status) {
        return discountUserRepository.getDiscountUserByStatus(status);
    }

    // to get all DiscountUser or specific DiscountUser(s) if search criteria exist
    public Page<RelatedDiscountUser> getAllDiscountUser(
            int page, int pageSize, String sortBy, Sort.Direction sortingOrder,
            String discountId, String userPhoneNumber, DiscountUserStatus status) {

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(sortingOrder, sortBy));

        Specification<DiscountUser> discountUserSpecs = searchCriteriaDiscountUser(
                discountId, userPhoneNumber, status);

        Page<DiscountUser> discountUserPage = discountUserRepository.findAll(discountUserSpecs, pageable);

        Page<RelatedDiscountUser> relatedDiscountUserPage = discountUserPage.map(this::getRelatedDiscountUser);

        return relatedDiscountUserPage;
    }

    // for use with getAllDiscountUser, to search specific DiscountUser(s) based on
    // criteria receive
    public static Specification<DiscountUser> searchCriteriaDiscountUser(
            String discountId, String userPhoneNumber, DiscountUserStatus status) {

        return (Specification<DiscountUser>) (root, query, builder) -> {
            final List<Predicate> predicates = new ArrayList<>();

            if (discountId != null) {
                predicates.add(builder.equal(root.get("discountId"), discountId));
            }

            if (userPhoneNumber != null) {
                predicates.add(builder.equal(root.get("userPhoneNumber"), userPhoneNumber));
            }

            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }

            query.distinct(true);

            return builder.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    // to get a list ListedUserDiscounts based on user phone number
    public List<ListedUserDiscount> getListedUserDiscount(String userPhoneNumber) {

        List<DiscountUser> listDiscountUsers = discountUserRepository.getDiscountUserByUserPhoneNumber(userPhoneNumber);

        // Transforming DiscountUser objects to ListedUserDiscount objects

        return listDiscountUsers.stream()
                .map(discountUser -> {
                    ListedUserDiscount listedUserDiscount = new ListedUserDiscount();
                    listedUserDiscount.setRelatedDiscountUser(this.getRelatedDiscountUser(discountUser));
                    listedUserDiscount.setRelatedDiscount(this.getRelatedDiscount(discountUser.getDiscount()));
                    return listedUserDiscount;
                })
                .collect(Collectors.toList());
    }

    // for use with getListedUserDiscount, set the receive data to a new
    // RelatedDiscountUser
    public RelatedDiscountUser getRelatedDiscountUser(DiscountUser data) {

        RelatedDiscountUser relatedDiscountUser = new RelatedDiscountUser();

        relatedDiscountUser.setDiscountId(data.getDiscountId());
        relatedDiscountUser.setUserPhoneNumber(data.getUserPhoneNumber());
        relatedDiscountUser.setStatus(data.getStatus());

        return relatedDiscountUser;
    }

    // for use with getListedUserDiscount, set the receive data to a new
    // RelatedDiscount
    public RelatedDiscount getRelatedDiscount(Discount data) {

        RelatedDiscount relatedDiscount = new RelatedDiscount();

        relatedDiscount.setName(data.getName());
        relatedDiscount.setDiscountCode(data.getDiscountCode());
        relatedDiscount.setDeleteReason(data.getDeleteReason());
        relatedDiscount.setDiscountValue(data.getDiscountValue());
        relatedDiscount.setMaxDiscountAmount(data.getMaxDiscountAmount());
        relatedDiscount.setMinimumSpend(data.getMinimumSpend());
        relatedDiscount.setTotalQuantity(data.getTotalQuantity());
        relatedDiscount.setStatus(data.getStatus());
        relatedDiscount.setCalculationType(data.getCalculationType());
        relatedDiscount.setStartDate(data.getStartDate());
        relatedDiscount.setEndDate(data.getEndDate());
        relatedDiscount.setCreatedDate(data.getCreatedDate());
        relatedDiscount.setUpdatedDate(data.getUpdatedDate());

        return relatedDiscount;
    }

    // to create new Discount
    public void createDiscountUser(DiscountUserRequest data) {

        DiscountUser discountUser = new DiscountUser();

        discountUser.setDiscountId(discountRepository.findByDiscountCode(data.getDiscountCode()).get().getId());

        discountUser.setUserPhoneNumber(data.getUserPhoneNumber());

        discountUser.setStatus(DiscountUserStatus.NEW);

        discountUserRepository.save(discountUser);
    }

    public HttpResponse claimDiscount(HttpServletRequest request, DiscountUserRequest discountUserRequest,
            String logPrefix, Integer productVariantId) {
        HttpResponse response = new HttpResponse(request.getRequestURI());

        if (discountUserRequest.getDiscountCode() == null || discountUserRequest.getUserPhoneNumber() == null) {
            response.setStatus(HttpStatus.EXPECTATION_FAILED, "Invalid payload");
            return response;
        }

        String msisdn = MsisdnUtil.formatMsisdn(discountUserRequest.getUserPhoneNumber());

        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix, "Discount User Request: ",
                discountUserRequest);

        Optional<Discount> discountOpt = discountRepository.findByDiscountCode(discountUserRequest.getDiscountCode());

        if (!discountOpt.isPresent()) {
            Logger.application.error(
                    Logger.pattern,
                    InternationalPaymentApplication.VERSION,
                    logPrefix,
                    "Discount Code does not exist");
            response.setStatus(HttpStatus.NOT_FOUND, "The discount code does not exist");
            return response;
        }

        // If a product variant ID is specified, validate its relation to the discount
        if (productVariantId != null) {
            Optional<DiscountProductVariant> discountProductVariantOptional = discountProductVariantService
                    .getDiscountProductByDiscountIdAndProductVariantId(
                            discountOpt.get().getId(),
                            productVariantId);

            // If not present, the product variant isn't part of the discount
            if (!discountProductVariantOptional.isPresent()) {
                Logger.application.error(
                        Logger.pattern,
                        InternationalPaymentApplication.VERSION,
                        logPrefix,
                        "No entry for Product Variant ID: " + productVariantId);
                response.setStatus(HttpStatus.NOT_FOUND, "The discount code is not applicable to this product");
                return response;
            }
        }

        try {
            DiscountUserId discountUserId = new DiscountUserId();
            discountUserId.setDiscountId(discountOpt.get().getId());
            discountUserId.setUserPhoneNumber(msisdn);

            Optional<DiscountUser> discountUserOpt = discountUserRepository.findById(discountUserId);

            if (discountUserOpt.isPresent()) {
                Logger.application.error(
                        Logger.pattern,
                        InternationalPaymentApplication.VERSION,
                        logPrefix,
                        "User has already claimed this discount");
                response.setStatus(HttpStatus.CONFLICT, "You have already claimed this discount");
                return response;
            }

            // Get the count of DiscountUser by discountId
            long discountUserCount = discountUserRepository.countByDiscountId(discountUserId.getDiscountId());
            // Check if the count exceeds the total quantity allowed
            if (discountUserCount >= discountOpt.get().getTotalQuantity()) {
                Logger.application.error(
                        Logger.pattern,
                        InternationalPaymentApplication.VERSION,
                        logPrefix,
                        "Discount has reached max quantity");
                response.setStatus(HttpStatus.CONFLICT, "The discount code has been fully claimed");
                return response;
            }

            // Create discount user
            createDiscountUser(discountUserRequest);

            response.setData(discountOpt.get());
            response.setStatus(HttpStatus.OK);
        } catch (Exception e) {
            Logger.application.error(
                    Logger.pattern,
                    InternationalPaymentApplication.VERSION,
                    logPrefix,
                    "Exception: " + e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST, "An error occurred while claiming the discount");
        }
        return response;
    }

    // to update DiscountUser status
    public DiscountUser updateStatus(UserUpdateRequest data) {

        DiscountUserId discountUserId = new DiscountUserId();
        discountUserId.setDiscountId(discountRepository.findByDiscountCode(data.getDiscountCode()).get().getId());
        discountUserId.setUserPhoneNumber(data.getUserPhoneNumber());

        Optional<DiscountUser> existingDiscountUser = getDiscountUserById(discountUserId);

        DiscountUser updateStatus = existingDiscountUser.get();

        updateStatus.setStatus(data.getStatus());

        DiscountUser body = discountUserRepository.save(updateStatus);
        return body;
    }
}
