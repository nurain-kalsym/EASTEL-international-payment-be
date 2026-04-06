package com.kalsym.internationalPayment.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.criteria.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.kalsym.internationalPayment.model.DiscountProductVariant;
import com.kalsym.internationalPayment.model.DiscountUser;
import com.kalsym.internationalPayment.model.dao.DeleteDiscountVariant;
import com.kalsym.internationalPayment.model.dao.DiscountProductPage;
import com.kalsym.internationalPayment.repositories.DiscountProductVariantRepository;
import com.kalsym.internationalPayment.repositories.DiscountUserRepository;

@Service
public class DiscountProductVariantService {

    @Autowired
    DiscountProductVariantRepository discountProductVariantRepository;

    @Autowired
    DiscountUserRepository discountUserRepository;

    public Optional<DiscountProductVariant> getDiscountProductVariantById(String id) {
        return discountProductVariantRepository.findById(id);
    }

    public List<DiscountProductVariant> getDiscountProductByDiscountId(String discountId) {
        return discountProductVariantRepository.getDiscountProductByDiscountId(discountId);
    }

    public List<DiscountProductVariant> getDiscountProductByProductVariantId(Integer productVariantId) {
        return discountProductVariantRepository.getDiscountProductByProductVariantId(productVariantId);
    }

    public Optional<DiscountProductVariant> getDiscountProductByDiscountIdAndProductVariantId(String id, Integer productVariantId) {
        return discountProductVariantRepository.findByDiscountIdAndProductVariantId(id, productVariantId);
    }

    //to get all DiscountProductVariant or specific DiscountProductVariant(s) if search criteria exist
    public Page<DiscountProductVariant> getAllDiscountProductVariant(
            int page, int pageSize, String sortBy, Sort.Direction sortingOrder,
            String discountId, Integer productVariantId) {
        
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(sortingOrder, sortBy));

        Specification<DiscountProductVariant> discountProductVariantSpecs = searchCriteriaDiscountProductVariant(
            discountId, productVariantId);
        
        Page<DiscountProductVariant> discountProductVariantPage = discountProductVariantRepository.findAll(discountProductVariantSpecs, pageable);

        return discountProductVariantPage;
    }

    //for use with getAllDiscountProductVariant, to search specific DiscountProductVariant(s) based on criteria receive
    public static Specification<DiscountProductVariant> searchCriteriaDiscountProductVariant(
        String discountId, Integer productVariantId){

        return (Specification<DiscountProductVariant>) (root, query, builder) -> {
            final List<Predicate> predicates = new ArrayList<>();

            if (discountId != null) {
                predicates.add(builder.equal(root.get("discountId"), discountId));
            }

            if (productVariantId != null) {
                predicates.add(builder.equal(root.get("productVariantId"), productVariantId));
            }

            query.distinct(true);

            return builder.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    //to create new DiscountProductVariant, or update existing one
    public DiscountProductVariant[] createOrUpdate(String discountId, DiscountProductPage[] productVariants) {

        List<DiscountProductVariant> existingDiscountProductVariants = getDiscountProductByDiscountId(discountId);

        //check if there is any existing data base on discountId, create new if there is not
        if (existingDiscountProductVariants.isEmpty()) {

            for (DiscountProductPage idPage : productVariants) {

                DiscountProductVariant createDiscountProductVariant = new DiscountProductVariant();

                createDiscountProductVariant.setDiscountId(discountId);

                createDiscountProductVariant.setProductVariantId(idPage.getProductVariantId());

                createDiscountProductVariant.setProductPage(idPage.getProductPage());

                discountProductVariantRepository.save(createDiscountProductVariant);

            }
            
        } else {

            // Iterate through each DiscountProductPage
            for (DiscountProductPage idPage : productVariants) {

                // Check if the DiscountProductVariant already exists
                Optional<DiscountProductVariant> existingVariantOptional = existingDiscountProductVariants.stream()
                        .filter(variant -> variant.getProductVariantId().equals(idPage.getProductVariantId()))
                        .findFirst();

                // If the DiscountProductVariant exists, update it; otherwise, create a new one
                if (existingVariantOptional.isPresent()) {
                    DiscountProductVariant existingVariant = existingVariantOptional.get();
                    // Update the existing DiscountProductVariant
                    existingVariant.setProductPage(idPage.getProductPage());
                    discountProductVariantRepository.save(existingVariant);
                } else {
                    // Create a new DiscountProductVariant
                    DiscountProductVariant newDiscountProductVariant = new DiscountProductVariant();
                    newDiscountProductVariant.setDiscountId(discountId);
                    newDiscountProductVariant.setProductVariantId(idPage.getProductVariantId());
                    newDiscountProductVariant.setProductPage(idPage.getProductPage());
                    discountProductVariantRepository.save(newDiscountProductVariant);
                }
            }

        }

        //get the size and then the list of DiscountProductVariant based on the discountId
        List<DiscountProductVariant> listDiscountProductVariants = getDiscountProductByDiscountId(discountId);
        Integer amount = listDiscountProductVariants.size();

        DiscountProductVariant[] body = new DiscountProductVariant[amount];
        for (int i = 0; i < amount; i++) {
            body[i] = listDiscountProductVariants.get(i);
        }

        return body;
    }

    //to delete DiscountProductVariant base on the receive data array
    public String[] deleteDiscountProductVariant(DeleteDiscountVariant[] strings) {

        List<String> failedProductVariantNames = new ArrayList<>();

        // Iterate through data
        for (DeleteDiscountVariant string : strings) {
            // Check if related to any DiscountUser
            Optional<DiscountUser> existingUserOpt = discountUserRepository.getByDiscountId(string.getDiscountId())
                    .stream().findFirst();

            // If the DiscountUser exists, add to fails array
            if (existingUserOpt.isPresent()) {
                // Add the product variant ID to the list of failed IDs
                failedProductVariantNames.add(string.getProductVariantName()); 

            } else {
                //Delete DiscountProductVariant
                discountProductVariantRepository.deleteById(string.getDiscountProductVariantId());
            }
        }

        // Convert the list of failed IDs back to an array
        String[] fails = failedProductVariantNames.toArray(new String[0]);

        // Check if fails array is empty
        if (fails.length == 0) {
            // If fails is empty, return a new array with "empty"
            return new String[] {"empty"};
        } else {
            // If fails is not empty, return the array of failed product variant IDs
            return fails;
        }
    }
}
