package com.kalsym.ekedai.services;

import com.kalsym.ekedai.EkedaiApplication;
import com.kalsym.ekedai.model.*;
import com.kalsym.ekedai.model.dao.CampaignCriteriaDTO;
import com.kalsym.ekedai.model.dao.CampaignDTO;
import com.kalsym.ekedai.model.enums.Operator;
import com.kalsym.ekedai.repositories.*;
import com.kalsym.ekedai.utility.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.kalsym.ekedai.utility.ConditionEvaluator.applyOperatorForNumber;
import static com.kalsym.ekedai.utility.ConditionEvaluator.applyOperatorForString;

@Service
public class CampaignService {

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignCriteriaRepository campaignCriteriaRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    public Campaign createCampaign(CampaignDTO campaignDTO) {
        // Map the basic fields of the campaign
        Campaign campaign = getCampaign(campaignDTO);

        // Save campaign and get the saved instance with ID
        campaign = campaignRepository.save(campaign);

        // Save criteria associated with the campaign
        if (campaignDTO.getCriteria() != null) {
            for (CampaignCriteriaDTO criteriaDTO : campaignDTO.getCriteria()) {
                CampaignCriteria criteria = new CampaignCriteria();
                criteria.setCampaignId(campaign.getId());  // Link to the saved campaign
                criteria.setCriterionType(criteriaDTO.getCriterionType());
                criteria.setCriterionValue(criteriaDTO.getCriterionValue());
                criteria.setOperator(criteriaDTO.getOperator());
                criteria.setGroupId(criteriaDTO.getGroupId());

                campaignCriteriaRepository.save(criteria);
            }

            // Fetch and set the criteria for this campaign to keep it up-to-date
            List<CampaignCriteria> criteriaList = campaignCriteriaRepository.findByCampaignId(campaign.getId());
            campaign.setCriteria(criteriaList);
        }

        return campaign;
    }

    @NotNull
    private static Campaign getCampaign(CampaignDTO campaignDTO) {
        Campaign campaign = new Campaign();
        campaign.setCampaignName(campaignDTO.getCampaignName());
        campaign.setStartDate(campaignDTO.getStartDate());
        campaign.setEndDate(campaignDTO.getEndDate());
        campaign.setDescription(campaignDTO.getDescription());
        campaign.setRewardType(campaignDTO.getRewardType());
        campaign.setRewardValue(campaignDTO.getRewardValue());
        campaign.setIsActive(false);
        return campaign;
    }

    @Transactional
    public Campaign updateCampaign(String campaignId, CampaignDTO campaignDTO) throws Exception {
        Optional<Campaign> optionalCampaign = campaignRepository.findById(campaignId);
        if (optionalCampaign.isPresent()) {
            Campaign campaign = optionalCampaign.get();

            campaign.setCampaignName(campaignDTO.getCampaignName());
            campaign.setStartDate(campaignDTO.getStartDate());
            campaign.setEndDate(campaignDTO.getEndDate());
            campaign.setDescription(campaignDTO.getDescription());
            campaign.setRewardType(campaignDTO.getRewardType());
            campaign.setRewardValue(campaignDTO.getRewardValue());
            campaign.setIsActive(false);

            // Save campaign and its criteria
            campaign = campaignRepository.save(campaign);

            // Save criteria associated with the campaign
            if (campaignDTO.getCriteria() != null) {
                // Delete all criteria for this campaign first
                campaignCriteriaRepository.deleteByCampaignId(campaign.getId());

                // Then create new ones
                for (CampaignCriteriaDTO criteriaDTO : campaignDTO.getCriteria()) {
                    CampaignCriteria criteria = new CampaignCriteria();
                    criteria.setCampaignId(campaign.getId()); // Set campaign ID
                    criteria.setCriterionType(criteriaDTO.getCriterionType());
                    criteria.setCriterionValue(criteriaDTO.getCriterionValue());
                    criteria.setOperator(criteriaDTO.getOperator());
                    criteria.setGroupId(criteriaDTO.getGroupId());

                    campaignCriteriaRepository.save(criteria);
                }
                // Fetch criteria for this campaign
                List<CampaignCriteria> criteriaList = campaignCriteriaRepository.findByCampaignId(campaign.getId());
                // Set the new criteria
                campaign.setCriteria(criteriaList);
            }

            return campaign;
        }
        // Throw exception if not found
        throw new Exception("Campaign not found with id: " + campaignId);
    }

    public Page<Campaign> getAllCampaigns(String sortBy, String sortingOrder, int page, int pageSize,
                                          Boolean isActive, String search,
                                          LocalDate from, LocalDate to, LocalDate campaignStart, LocalDate campaignEnd) {
        Sort sort = Sort.by(sortingOrder.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, pageSize, sort);

        Specification<Campaign> spec = Specification.where(filterByDateRange(from, to))
                .and(filterByStatus(isActive))
                .and(filterBySearch(search))
                .and(filterByCampaignActiveRange(campaignStart, campaignEnd));

        return campaignRepository.findAll(spec, pageable);
    }

    public static Specification<Campaign> filterByCampaignActiveRange(LocalDate start, LocalDate end) {
        return (root, query, criteriaBuilder) -> {
            if (start != null && end != null) {
                // Campaigns that overlap the range [start, end]
                return criteriaBuilder.and(
                        criteriaBuilder.lessThanOrEqualTo(root.get("startDate"), end), // Campaign starts before or on 'end'
                        criteriaBuilder.greaterThanOrEqualTo(root.get("endDate"), start)  // Campaign ends after or on 'start'
                );
            } else if (start != null) {
                // Campaigns that end after or on the 'start' date
                return criteriaBuilder.greaterThanOrEqualTo(root.get("endDate"), start);
            } else if (end != null) {
                // Campaigns that start before or on the 'end' date
                return criteriaBuilder.lessThanOrEqualTo(root.get("startDate"), end);
            }
            // Returns a conjunction (which is essentially a no-op predicate)
            return criteriaBuilder.conjunction(); // Default case: return all campaigns (no filtering)
        };
    }


    public static Specification<Campaign> filterByDateRange(LocalDate from, LocalDate to) {
        return (root, query, criteriaBuilder) -> {
            if (from != null && to != null) {
                return criteriaBuilder.between(root.get("createdAt"),
                        from.atStartOfDay(),
                        to.atTime(LocalTime.MAX));
            } else if (from != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay());
            } else if (to != null) {
                return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), to.atTime(LocalTime.MAX));
            }
            // Returns a conjunction (which is essentially a no-op predicate)
            return criteriaBuilder.conjunction();
        };
    }

    public static Specification<Campaign> filterByStatus(Boolean isActive) {
        return (root, query, criteriaBuilder) -> {
            if (isActive != null) {
                criteriaBuilder.equal(root.get("isActive"), isActive);
            }
            // Returns a conjunction (which is essentially a no-op predicate)
            return criteriaBuilder.conjunction();
        };
    }

    public static Specification<Campaign> filterBySearch(String search) {
        return (root, query, criteriaBuilder) -> {
            if (StringUtils.hasText(search)) {
                String likePattern = "%" + search.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("campaignName")), likePattern);
            }
            // Returns a conjunction (which is essentially a no-op predicate)
            return criteriaBuilder.conjunction();
        };
    }

    public Campaign getCampaignById(String id) {
        return campaignRepository.findById(id).orElse(null);
    }

    public void deleteCampaign(String id) {
        campaignRepository.deleteById(id);
    }

    public List<CampaignCriteria> getCriteriaByCampaignId(String campaignId) {
        return campaignCriteriaRepository.findByCampaignId(campaignId);
    }

    public CampaignCriteria addCampaignCriteria(CampaignCriteria criteria) {
        return campaignCriteriaRepository.save(criteria);
    }

    @Transactional
    public Campaign updateCampaignStatus(String campaignId, boolean isActive) throws Exception {
        Optional<Campaign> optionalCampaign = campaignRepository.findById(campaignId);
        if (!optionalCampaign.isPresent()) {
            throw new Exception("Campaign not found with id: " + campaignId);
        }

        Campaign campaign = optionalCampaign.get();

        // Check if setting to active and ensure no other active campaign in the same
        // date range
        if (isActive) {
            boolean isValid = validateUniqueActiveCampaign(campaign.getStartDate(), campaign.getEndDate(), campaignId);
            if (!isValid) {
                throw new Exception("Another campaign is already active with the same campaign period.");
            }
        }
        // Set to new status
        campaign.setIsActive(isActive);
        return campaignRepository.save(campaign);
    }

    private boolean validateUniqueActiveCampaign(LocalDate startDate, LocalDate endDate, String campaignIdToExclude) {
        List<Campaign> activeCampaigns = campaignRepository.findActiveCampaignsInDateRangeAndToExclude(startDate,
                endDate, campaignIdToExclude);

        // If no active campaigns, we return true, which means it is valid
        return activeCampaigns.isEmpty();

    }

    public Optional<Campaign> findAndValidateUserCriteria(User user, String logPrefix) {
        // Find one active campaign based on status and current date
        Optional<Campaign> activeCampaign = campaignRepository.findOneActiveCampaign(LocalDate.now());

        if (activeCampaign.isPresent()) {
            Campaign campaign = activeCampaign.get();

            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                    "Active campaign found: " + campaign.getCampaignName());

            // Fetch and group criteria by groupId for the active campaign
            List<CampaignCriteria> criteriaList = campaignCriteriaRepository.findByCampaignId(campaign.getId());
            Map<Integer, List<CampaignCriteria>> groupedCriteria = criteriaList.stream()
                    .collect(Collectors.groupingBy(CampaignCriteria::getGroupId));

            // Check each group to see if any one group's criteria are fully met
            boolean anyGroupMet = false;

            for (Map.Entry<Integer, List<CampaignCriteria>> groupEntry : groupedCriteria.entrySet()) {
                boolean groupCriteriaMet = true; // Assume all criteria in the group are met

                for (CampaignCriteria criteria : groupEntry.getValue()) {
                    String[] acceptedValues = criteria.getCriterionValue().toLowerCase().split("\\|");
                    Operator operator = criteria.getOperator() != null ? Operator.valueOf(criteria.getOperator().toUpperCase()) : Operator.AND;

                    boolean criteriaMet = false;

                    switch (criteria.getCriterionType()) {
                        case USER_SEGMENT:
                            criteriaMet = applyOperatorForString(operator, acceptedValues, value -> {
                                switch (value) {
                                    case "new":
                                        Instant createdInstant = user.getCreated().toInstant();
                                        return createdInstant.isAfter(Instant.now().minus(24, ChronoUnit.HOURS));
                                    case "local":
                                        return user.getNationality().equalsIgnoreCase("Malaysia");
                                    case "international":
                                        return !user.getNationality().equalsIgnoreCase("Malaysia");
                                    default:
                                        Logger.application.warn(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                                                "Unsupported user segment value: " + value);
                                        return false;
                                }
                            });
                            break;

//                        case USER_AGE:
//                            // Add age validation logic and operator handling here
//                            criteriaMet = true;
//                            break;
//
//                        case USER_GENDER:
//                            // Add gender validation logic and operator handling here
//                            criteriaMet = true;
//                            break;

                        default:
                            Logger.application.warn(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                                    "Unsupported criterion type: " + criteria.getCriterionType());
//                            criteriaMet = false;
                            break;
                    }

                    if (!criteriaMet) {
                        groupCriteriaMet = false;
                        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                                "Criterion not met in group " + groupEntry.getKey() + ": " + criteria.getCriterionType());
                        break; // Stop checking this group if any criterion fails
                    }
                    Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                            "Criterion met in group " + groupEntry.getKey() + ": " + criteria.getCriterionType());
                }

                if (groupCriteriaMet) {
                    anyGroupMet = true; // At least one group met all criteria
                    Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                            "All criteria met for group " + groupEntry.getKey() + " for user: " + user.getPhoneNumber());
                    break; // No need to check other groups if one group meets all criteria
                }
            }

            if (anyGroupMet) {
                Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                        "Campaign criteria met for user: " + user.getPhoneNumber());
                return Optional.of(campaign);
            } else {
                Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                        "Campaign criteria not met for user: " + user.getPhoneNumber());
            }
        }

        // Return empty if no active campaign criteria are met
        return Optional.empty();
    }


    /*public Optional<Campaign> findAndValidateTransactionCriteriaOld(Transaction transaction, User user, String logPrefix) {
        // Find one active campaign based on status and current date
        Optional<Campaign> activeCampaign = campaignRepository.findOneActiveCampaign(LocalDate.now());

        if (activeCampaign.isPresent()) {
            Campaign campaign = activeCampaign.get();
            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                    "Active campaign found: " + campaign.getCampaignName());

            // Fetch criteria for the active campaign
            List<CampaignCriteria> criteriaList = campaignCriteriaRepository.findByCampaignId(campaign.getId());

            // Get product variant associated with the transaction
            Optional<ProductVariant> productVariantOptional = productVariantRepository
                    .findById(transaction.getProductVariantId());

            boolean allCriteriaMet = criteriaList.stream().allMatch(criteria -> {

                // Set to lowercase to ensure case-insensitive
                String criterionValueLowerCase = criteria.getCriterionValue().toLowerCase();
                switch (criteria.getCriterionType()) {
                    case TRANSACTION_TYPE:
                        // Check if transaction type matches the criteria
                        return criterionValueLowerCase.equals(transaction.getTransactionType().name().toLowerCase());
                    case MIN_AMOUNT:
                        // Check if transaction amount meets or exceeds the minimum amount criteria
                        try {
                            return Double.parseDouble(criterionValueLowerCase) <= transaction.getTransactionAmount();
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    case PRODUCT_TYPE:
                        // Check if product type matches the criteria
                        if (productVariantOptional.isPresent()) {
                            Optional<Product> optionalProduct = productRepository
                                    .findById(productVariantOptional.get().getProductId());
                            if (optionalProduct.isPresent()) {
                                return criterionValueLowerCase
                                        .equals(optionalProduct.get().getProductType().name().toLowerCase());
                            }
                        }
                        return false; // If product variant or product not found
                    case PRODUCT_COUNTRY_CODE:
                        // Check if product country code matches the criteria or not excluded by the
                        // criteria
                        if (productVariantOptional.isPresent()) {
                            Optional<Product> optionalProduct = productRepository
                                    .findById(productVariantOptional.get().getProductId());
                            if (optionalProduct.isPresent()) {
                                String productCountryCode = optionalProduct.get().getCountryCode().toLowerCase();
                                if (criterionValueLowerCase.startsWith("not_")) {
                                    // If criteria is NOT_<countryCode>, check that product's country code is not
                                    // the excluded code
                                    String countryCodeToExclude = criterionValueLowerCase.substring(4);
                                    return !productCountryCode.equals(countryCodeToExclude);
                                } else {
                                    // Regular country code check
                                    return productCountryCode.equals(criterionValueLowerCase);
                                }
                            }
                        }
                        return false; // If product variant or product not found
                    case USER_SEGMENT:
                        if (user != null) {
                            // Check user segment criteria: 'new', 'local', or 'international'
                            switch (criterionValueLowerCase) {
                                case "new":
                                    // Convert `created` Date to Instant and check if the account was created in the
                                    // last 24 hours
                                    Instant createdInstant = user.getCreated().toInstant();
                                    return createdInstant.isAfter(Instant.now().minus(24, ChronoUnit.HOURS));
                                case "local":
                                    // Check if the user is 'local' by comparing nationality to 'Malaysian'
                                    return user.getNationality().equalsIgnoreCase("Malaysian");
                                case "international":
                                    // Check if the user is 'international' by ensuring nationality is NOT
                                    // 'Malaysian'
                                    return !user.getNationality().equalsIgnoreCase("Malaysian");
                                default:
                                    // Log if an unsupported value is found in the campaign criteria for
                                    // USER_SEGMENT
                                    Logger.application.warn(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                                            "Unsupported user segment value: " + criterionValueLowerCase);
                                    return false;
                            }
                        }
                        return false; // If user is null
                    // Add more cases
                    default:
                        return false; // Default case for unsupported criteria types
                }
            });

            if (allCriteriaMet) {
                // Log if all criteria are met for the campaign
                Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                        "Campaign criteria met for transaction: " + transaction.getTransactionId());
                return Optional.of(campaign); // Return the campaign if criteria are met
            } else {
                // Log if criteria are not met for the campaign
                Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                        "Campaign criteria not met for transaction: " + transaction.getTransactionId());
            }
        }

        return Optional.empty(); // Return empty if no active campaign criteria are met
    }*/

    public Optional<Campaign> findAndValidateTransactionCriteria(Transaction transaction, User user, String logPrefix) {
        Optional<Campaign> activeCampaign = campaignRepository.findOneActiveCampaign(LocalDate.now());

        if (activeCampaign.isPresent()) {
            Campaign campaign = activeCampaign.get();
            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                    "Active campaign found: " + campaign.getCampaignName());

            List<CampaignCriteria> criteriaList = campaignCriteriaRepository.findByCampaignId(campaign.getId());
            Optional<ProductVariant> productVariantOptional = productVariantRepository.findById(transaction.getProductVariantId());

            // Group criteria by `groupId`
            Map<Integer, List<CampaignCriteria>> groupedCriteria = criteriaList.stream()
                    .collect(Collectors.groupingBy(CampaignCriteria::getGroupId));

            boolean anyGroupMet = false;

            // Iterate over each group of criteria
            for (Map.Entry<Integer, List<CampaignCriteria>> groupEntry : groupedCriteria.entrySet()) {
                boolean groupCriteriaMet = true; // Assume all criteria in the group will be met

                for (CampaignCriteria criteria : groupEntry.getValue()) {
                    String[] acceptedValues = criteria.getCriterionValue().toLowerCase().split("\\|");
                    Operator operator = criteria.getOperator() != null ? Operator.valueOf(criteria.getOperator().toUpperCase()) : Operator.AND;

                    boolean criteriaMet = false;

                    switch (criteria.getCriterionType()) {
                        case TRANSACTION_TYPE:
                            criteriaMet = applyOperatorForString(operator, acceptedValues,
                                    value -> value.equals(transaction.getTransactionType().toString().toLowerCase()));
                            break;

                        case TRANSACTION_AMOUNT:
                            try {
                                double transactionAmount = transaction.getTransactionAmount();
                                criteriaMet = applyOperatorForNumber(operator, acceptedValues, transactionAmount);
                            } catch (NumberFormatException e) {
                                Logger.application.warn(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                                        "Invalid number format in criteria for TRANSACTION_AMOUNT: " + e.getMessage());
                            }
                            break;

                        case PRODUCT_TYPE:
                            if (productVariantOptional.isPresent()) {
                                Optional<Product> optionalProduct = productRepository.findById(productVariantOptional.get().getProductId());
                                if (optionalProduct.isPresent()) {
                                    String productType = optionalProduct.get().getProductType().toString().toLowerCase();
                                    criteriaMet = applyOperatorForString(operator, acceptedValues, value -> value.equals(productType));
                                }
                            }
                            break;

                        case PRODUCT_COUNTRY_CODE:
                            if (productVariantOptional.isPresent()) {
                                Optional<Product> optionalProduct = productRepository.findById(productVariantOptional.get().getProductId());
                                if (optionalProduct.isPresent()) {
                                    String productCountryCode = optionalProduct.get().getCountryCode().toLowerCase();
                                    criteriaMet = applyOperatorForString(operator, acceptedValues, value -> value.equals(productCountryCode));
                                }
                            }
                            break;

                        case USER_SEGMENT:
                            if (user != null) {
                                criteriaMet = applyOperatorForString(operator, acceptedValues, value -> {
                                    switch (value) {
                                        case "new":
                                            Instant createdInstant = user.getCreated().toInstant();
                                            return createdInstant.isAfter(Instant.now().minus(24, ChronoUnit.HOURS));
                                        case "local":
                                            return user.getNationality().equalsIgnoreCase("Malaysian");
                                        case "international":
                                            return !user.getNationality().equalsIgnoreCase("Malaysian");
                                        default:
                                            Logger.application.warn(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                                                    "Unsupported user segment value: " + value);
                                            return false;
                                    }
                                });
                            }
                            break;

                        case TRANSACTION_FREQUENCY:
                            try {
                                int requiredFrequency = Integer.parseInt(criteria.getCriterionValue());
                                // Ensure the current transaction status is "PAID"
                                if (!"PAID".equals(transaction.getStatus())) {
                                    Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                                            "Transaction status is not PAID. Current status: " + transaction.getStatus());
                                    break;
                                }

                                // Convert LocalDate to Date
                                Date startDate = Date.from(campaign.getStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
                                Date endDate = Date.from(campaign.getEndDate().atStartOfDay(ZoneId.systemDefault()).toInstant());

                                // Query the number of "PAID" transactions by the user within the campaign period, excluding the current transaction
                                long userTransactionCount = transactionRepository.countByUserIdAndStatusAndCreatedDateBetweenExcludingTransaction(
                                        user != null ? user.getId() : null, "PAID", startDate, endDate, transaction.getId());

                                // Include the current transaction
                                userTransactionCount++;

                                // Compare the transaction count with the required frequency
                                criteriaMet = userTransactionCount == requiredFrequency;

                                Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                                        "Transaction frequency for user: " + userTransactionCount + ", Required: " + requiredFrequency);
                            } catch (NumberFormatException e) {
                                Logger.application.warn(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                                        "Invalid number format in criteria for TRANSACTION_FREQUENCY: " + e.getMessage());
                            }
                            break;

                        default:
                            Logger.application.warn(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                                    "Unsupported criterion type: " + criteria.getCriterionType());
                            break;
                    }

                    if (!criteriaMet) {
                        groupCriteriaMet = false;
                        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                                "Criterion not met in group " + groupEntry.getKey() + ": " + criteria.getCriterionType());
                        break; // Exit loop if any criterion in the group is not met
                    }
                    Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                            "Criterion met in group " + groupEntry.getKey() + ": " + criteria.getCriterionType());
                }

                if (groupCriteriaMet) {
                    anyGroupMet = true;
                    break; // Exit loop if one group of criteria is met
                }
            }

            if (anyGroupMet) {
                Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                        "Campaign criteria met for transaction: " + transaction.getTransactionId());
                return Optional.of(campaign);
            } else {
                Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                        "Campaign criteria not met for transaction: " + transaction.getTransactionId());
            }
        }

        return Optional.empty();
    }

    public boolean isDiscountCodeUsed(String discountCode) {
        // Fetch campaigns with reward type DISCOUNT_CODE and the given discount code as
        // reward value
        List<Campaign> campaigns = campaignRepository
                .findByRewardTypeAndRewardValue(Campaign.CampaignRewardType.DISCOUNT_CODE, discountCode);
        // Return true if any campaigns are found, otherwise false
        return !campaigns.isEmpty();
    }

}
