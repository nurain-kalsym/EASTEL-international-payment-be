package com.kalsym.ekedai.services;

import com.kalsym.ekedai.model.Discount;
import com.kalsym.ekedai.model.dao.DiscountRequest;
import com.kalsym.ekedai.model.dao.RelatedDiscount;
import com.kalsym.ekedai.model.enums.CalculationType;
import com.kalsym.ekedai.model.enums.DiscountStatus;
import com.kalsym.ekedai.repositories.DiscountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class DiscountService {
    
    @Autowired
    DiscountRepository discountRepository;

    public Optional<Discount> getDiscountById(String id) {
        return discountRepository.findById(id);
    }

    public Optional<Discount> getDiscountByDiscountCode(String discountCode) {
        return discountRepository.findByDiscountCode(discountCode);
    }

    public List<Discount> getDiscountByStatus(DiscountStatus status) {
        return discountRepository.getDiscountByStatus(status);
    }
    
    public List<Discount> getDiscountByCalculationType(CalculationType calculationType) {
        return discountRepository.getDiscountByCalculationType(calculationType);
    }

    public Specification<Discount> getDiscountByStatusAndCalculationType(DiscountStatus status, CalculationType calculationType) {
        return discountRepository.getDiscountByStatusAndCalculationType(status, calculationType);
    }

    //to get all Discount or specific Discount(s) if search criteria exist
    public Page<Discount> getAllDiscount(
            int page, int pageSize, String sortBy, Sort.Direction sortingOrder,
            String globalSearch, DiscountStatus status, CalculationType calculationType) {
        
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(sortingOrder, sortBy));

        Specification<Discount> discountSpecs = searchCriteriaDiscount(globalSearch, status, calculationType);
        
        Page<Discount> discountPage = discountRepository.findAll(discountSpecs, pageable);
        
        return discountPage;
    }

    //for use with getAllDiscount, to search specific Discount(s) based on criteria receive
    public static Specification<Discount> searchCriteriaDiscount( String globalSearch, DiscountStatus status, CalculationType calculationType) {

        return (Specification<Discount>) (root, query, builder) -> {
            final List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }

            if (calculationType != null) {
                predicates.add(builder.equal(root.get("calculationType"), calculationType));
            }

            if (globalSearch != null) {
                predicates.add(builder.or(
                        builder.like(root.get("discountCode"), "%" + globalSearch + "%"),
                        builder.like(root.get("name"), "%" + globalSearch + "%")));
            }

            query.distinct(true);

            return builder.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    //to update Discount status, with additional processes if DELETE
    public Discount updateStatus(String discountCode, DiscountStatus status, String deleteReason) {

        Optional<Discount> existingStatus = getDiscountByDiscountCode(discountCode);

        Discount updateStatus = existingStatus.get();

        //check if the update want to delete the Discount. Otherwise, just update normally.
        if (status == DiscountStatus.DELETED) {
            updateStatus.setStatus(status);
            updateStatus.setDeleteReason(deleteReason);
            
        } else {
            updateStatus.setStatus(status);

        }

        Discount body = discountRepository.save(updateStatus);
        return body;
    }

    //to create new Discount, or update existing one
    public Discount createOrUpdateDiscount(DiscountRequest data) {

        Optional<Discount> existingDiscount = getDiscountByDiscountCode(data.getDiscountCode());

        //if existingDiscount exist, it will update. Otherwise, it will create a new Discount
        if (existingDiscount.isPresent()) {
            Discount updateDiscount = existingDiscount.get();

            updateDiscount.setName(data.getName());

            updateDiscount.setDiscountCode(data.getDiscountCode());

            updateDiscount.setDiscountValue(data.getDiscountValue());

            updateDiscount.setMaxDiscountAmount(data.getMaxDiscountAmount());

            updateDiscount.setMinimumSpend(data.getMinimumSpend());

            updateDiscount.setTotalQuantity(data.getTotalQuantity());

            updateDiscount.setStatus(data.getStatus());

            updateDiscount.setCalculationType(data.getCalculationType());

            updateDiscount.setStartDate(data.getStartDate());

            updateDiscount.setEndDate(data.getEndDate());

            updateDiscount.setUpdatedDate(new Date());

            Discount body = discountRepository.save(updateDiscount);
            return body;

        } else {

            Discount createDiscount = new Discount();

            createDiscount.setName(data.getName());

            createDiscount.setDiscountCode(data.getDiscountCode());

            createDiscount.setDiscountValue(data.getDiscountValue());

            createDiscount.setMaxDiscountAmount(data.getMaxDiscountAmount());

            createDiscount.setMinimumSpend(data.getMinimumSpend());

            createDiscount.setTotalQuantity(data.getTotalQuantity());

            createDiscount.setStatus(data.getStatus());

            createDiscount.setCalculationType(data.getCalculationType());

            createDiscount.setStartDate(data.getStartDate());

            createDiscount.setEndDate(data.getEndDate());

            createDiscount.setCreatedDate(new Date());

            Discount body = discountRepository.save(createDiscount);
            return body;
        }
    }

    //to calculate the discounted price, between FIX or PERCENT
    public Double getDiscountedPrice(RelatedDiscount relatedDiscount, Double convertedPrice) {
        if (relatedDiscount == null || convertedPrice == null) {
            throw new IllegalArgumentException("relatedDiscount and convertedPrice must not be null");
        }

        Double discountedPrice;

        switch (relatedDiscount.getCalculationType()) {
            case FIX:
                discountedPrice = calculateFixedDiscount(relatedDiscount, convertedPrice);
                break;

            case PERCENT:
                discountedPrice = calculatePercentageDiscount(relatedDiscount, convertedPrice);
                break;

            default:
                throw new IllegalArgumentException("Unsupported calculation type");
        }

        return discountedPrice;
    }

    private Double calculateFixedDiscount(RelatedDiscount relatedDiscount, Double convertedPrice) {
        Double discountValue = relatedDiscount.getDiscountValue();

        // Ensure discount doesn't exceed converted price
        if (discountValue > convertedPrice) {
            return 0.0;
        }

        return convertedPrice - discountValue;
    }

    private Double calculatePercentageDiscount(RelatedDiscount relatedDiscount, Double convertedPrice) {
        Double discountPercentage = relatedDiscount.getDiscountValue() / 100.0;
        Double discountAmount = convertedPrice * discountPercentage;

        // Check if discountAmount exceeds the maximum allowed discount
        if (relatedDiscount.getMaxDiscountAmount() != null && discountAmount > relatedDiscount.getMaxDiscountAmount()) {
            discountAmount = relatedDiscount.getMaxDiscountAmount();
        }

        return convertedPrice - discountAmount;
    }

}
