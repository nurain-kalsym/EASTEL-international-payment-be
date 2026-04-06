package com.kalsym.ekedai.controller;

import com.kalsym.ekedai.EkedaiApplication;
import com.kalsym.ekedai.model.*;
import com.kalsym.ekedai.model.dao.*;
import com.kalsym.ekedai.model.enums.CalculationType;
import com.kalsym.ekedai.model.enums.DiscountStatus;
import com.kalsym.ekedai.model.enums.DiscountUserStatus;
import com.kalsym.ekedai.repositories.DiscountRepository;
import com.kalsym.ekedai.repositories.DiscountUserRepository;
import com.kalsym.ekedai.repositories.DiscountWithDetailsRepository;
import com.kalsym.ekedai.services.CampaignService;
import com.kalsym.ekedai.services.DiscountProductVariantService;
import com.kalsym.ekedai.services.DiscountService;
import com.kalsym.ekedai.services.DiscountUserService;
import com.kalsym.ekedai.utility.HttpResponse;
import com.kalsym.ekedai.utility.JwtUtils;
import com.kalsym.ekedai.utility.Logger;
import com.kalsym.ekedai.utility.MsisdnUtil;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/discount")
public class DiscountController {

    @Autowired
    DiscountService discountService;

    @Autowired
    DiscountUserService discountUserService;

    @Autowired
    DiscountProductVariantService discountProductVariantService;

    @Autowired
    DiscountRepository discountRepository;

    @Autowired
    DiscountUserRepository discountUserRepository;

    @Autowired
    DiscountWithDetailsRepository discountWithDetailsRepository;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    CampaignService campaignService;

    @Operation(summary = "Get all discounts.", description = "To retrieve all information, or based on criterias, related to discounts.")
    @GetMapping("/get-discount")
    public ResponseEntity<HttpResponse> getDiscount(HttpServletRequest request,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction sortingOrder,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String globalSearch,
            @RequestParam(required = false) DiscountStatus status,
            @RequestParam(required = false) CalculationType calculationType) {

        String logprefix = "getDiscount";

        HttpResponse response = new HttpResponse(request.getRequestURI());

        try {

            Page<Discount> discountPage = discountService
                    .getAllDiscount(page, pageSize, sortBy, sortingOrder, globalSearch, status, calculationType);

            response.setData(discountPage);
            response.setStatus(HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERRROR:::" + e.getMessage());

            response.setStatus(HttpStatus.BAD_REQUEST);
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Operation(summary = "Get discount by id.", description = "To retrieve all information related to the discount.")
    @GetMapping("/get-discount-by-id/{id}")
    public ResponseEntity<HttpResponse> getDiscountById(HttpServletRequest request,
            @PathVariable() String id) {

        String logprefix = "getDiscountById";

        HttpResponse response = new HttpResponse(request.getRequestURI());

        Optional<DiscountWithDetails> discountOptional = discountWithDetailsRepository.findById(id);
        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix, "Discount id: ", id);

        response.setStatus(HttpStatus.NOT_FOUND);
        if (discountOptional.isPresent()) {

            response.setStatus(HttpStatus.OK);
            response.setData(discountOptional.get());
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Operation(summary = "Get all discount users.", description = "To retrieve all information, or based on criterias, related to discount users.")
    @GetMapping("/get-discount-user")
    public ResponseEntity<HttpResponse> getDiscountUser(HttpServletRequest request,
            @RequestParam(defaultValue = "discountId") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction sortingOrder,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String discountId,
            @RequestParam(required = false) String userPhoneNumber,
            @RequestParam(required = false) DiscountUserStatus status) {

        String logprefix = "getDiscountUser";

        HttpResponse response = new HttpResponse(request.getRequestURI());

        try {

            userPhoneNumber = MsisdnUtil.formatMsisdn(userPhoneNumber);

            Page<RelatedDiscountUser> relatedDiscountUserPage = discountUserService
                    .getAllDiscountUser(page, pageSize, sortBy, sortingOrder, discountId, userPhoneNumber, status);

            response.setData(relatedDiscountUserPage);
            response.setStatus(HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERRROR:::" + e.getMessage());

            response.setStatus(HttpStatus.BAD_REQUEST);
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Operation(summary = "Create new discount.", description = "To create a new discount. All datetime variables must have the same format as shown.")
    @PostMapping("/create-discount")
    public ResponseEntity<HttpResponse> createDiscount(HttpServletRequest request,
            @RequestBody DiscountRequest data) {

        String logprefix = "createDiscount";
        HttpResponse response = new HttpResponse(request.getRequestURI());

        if (data.getDiscountCode() != null && data.getName() != null && data.getDiscountProductPage() != null) {

            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix, "Discount Code: ",
                    data.getDiscountCode());

            try {

                Optional<Discount> discountOpt = discountRepository.findByDiscountCode(data.getDiscountCode());

                if (discountOpt.isPresent()) {
                    response.setStatus(HttpStatus.EXPECTATION_FAILED, "Discount code already taken");
                    Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                            "Discount Code: " + Integer.toString(HttpStatus.EXPECTATION_FAILED.value())
                                    + " already taken");
                    return ResponseEntity.status(response.getStatus()).body(response);
                }

                // create both Discount and DiscountProductVariant
                Discount discountResult = discountService.createOrUpdateDiscount(data);
                DiscountProductVariant[] discountProductVariants = discountProductVariantService
                        .createOrUpdate(discountResult.getId(), data.getDiscountProductPage());

                // map results and body as responseData
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("discountResult", discountResult);
                responseData.put("discountProductVariants", discountProductVariants);

                response.setData(responseData);
                response.setStatus(HttpStatus.OK);

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("ERRROR:::" + e.getMessage());

                response.setStatus(HttpStatus.BAD_REQUEST);
                Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                        "Exception " + e.getMessage());
            }

        } else {
            response.setStatus(HttpStatus.EXPECTATION_FAILED, "Invalid payload");
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Operation(summary = "Update a discount.", description = "To update a discount. The needed data can be retrieve from `/get-discount` api.")
    @PutMapping("/update-discount")
    public ResponseEntity<HttpResponse> updateDiscount(HttpServletRequest request,
            @RequestBody DiscountRequest data) {

        String logprefix = "updateDiscount";
        HttpResponse response = new HttpResponse(request.getRequestURI());

        if (data.getDiscountCode() != null && data.getName() != null) {

            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix, "Discount Code: ",
                    data.getDiscountCode());

            try {

                Optional<Discount> discountOpt = discountRepository.findByDiscountCode(data.getDiscountCode());

                if (discountOpt.isPresent()) {

                    Discount discountResult = discountService.createOrUpdateDiscount(data);
                    response.setData(discountResult);

                    // if discountProductPage is present, update it first then map both results and
                    // body data as responseData
                    if (data.getDiscountProductPage() != null) {

                        DiscountProductVariant[] discountProductVariants = discountProductVariantService
                                .createOrUpdate(discountOpt.get().getId(), data.getDiscountProductPage());

                        Map<String, Object> responseData = new HashMap<>();
                        responseData.put("discountResult", discountResult);
                        responseData.put("discountProductVariants", discountProductVariants);

                        response.setData(responseData);
                    }

                    response.setStatus(HttpStatus.OK);

                } else {
                    response.setStatus(HttpStatus.NOT_FOUND, "Discount code does not exist");
                    Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                            "Discount Code: " + Integer.toString(HttpStatus.NOT_FOUND.value())
                                    + " has not been registered");
                    return ResponseEntity.status(response.getStatus()).body(response);
                }

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("ERRROR:::" + e.getMessage());

                response.setStatus(HttpStatus.BAD_REQUEST);
                Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                        "Exception " + e.getMessage());
            }
        } else {
            response.setStatus(HttpStatus.EXPECTATION_FAILED, "Invalid payload");
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Discount Code: " + Integer.toString(HttpStatus.EXPECTATION_FAILED.value())
                            + " has not been registered");
            return ResponseEntity.status(response.getStatus()).body(response);
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Operation(summary = "Update a discount status.", description = "To update a discount status. Please give reason if changing to DELETED.")
    @PutMapping("/update-discount-status")
    public ResponseEntity<HttpResponse> updateDiscountStatus(HttpServletRequest request,
            @RequestParam String discountCode,
            @RequestParam DiscountStatus status,
            @RequestParam(required = false) String deleteReason) {

        String logprefix = "updateDiscountStatus";
        HttpResponse response = new HttpResponse(request.getRequestURI());

        try {

            Optional<Discount> discountOpt = discountRepository.findByDiscountCode(discountCode);

            // check if discount status is DELETED
            if (discountOpt.get().getStatus() == DiscountStatus.DELETED) {
                response.setStatus(HttpStatus.EXPECTATION_FAILED,
                        "The discount status is already DELETED and can't be update.");
                Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                        "Discount Code: " + Integer.toString(HttpStatus.EXPECTATION_FAILED.value())
                                + " is in DELETED status");
                return ResponseEntity.status(response.getStatus()).body(response);

            } else if (discountOpt.isPresent()) {
                Discount discountResult = discountService.updateStatus(discountCode, status, deleteReason);

                response.setData(discountResult);
                response.setStatus(HttpStatus.OK);

            } else {
                response.setStatus(HttpStatus.NOT_FOUND, "Discount code does not exist");
                Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                        "Discount Code: " + Integer.toString(HttpStatus.NOT_FOUND.value())
                                + " has not been registered");
                return ResponseEntity.status(response.getStatus()).body(response);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERRROR:::" + e.getMessage());

            response.setStatus(HttpStatus.BAD_REQUEST);
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Operation(summary = "Create new discount user.", description = "To allow claiming of discount and save as discount user.")
    @PostMapping("/claim-discount")
    public ResponseEntity<HttpResponse> claimDiscount(HttpServletRequest request,
            @RequestParam(required = false) Integer productVariantId,
            @RequestBody DiscountUserRequest data) {

        String logprefix = "claimDiscount";
        HttpResponse response = new HttpResponse(request.getRequestURI());

        if (data.getDiscountCode() == null || data.getUserPhoneNumber() == null) {
            response.setStatus(HttpStatus.EXPECTATION_FAILED, "Invalid payload");
            return ResponseEntity.status(response.getStatus()).body(response);
        }

        // Check if discount code is used in any campaign
        boolean codeIsUsed = campaignService.isDiscountCodeUsed(data.getDiscountCode());
        if (codeIsUsed) {
            response.setStatus(HttpStatus.EXPECTATION_FAILED, "The discount code is a part of a campaign");
            return ResponseEntity.status(response.getStatus()).body(response);
        }

        // Claim discount process
        HttpResponse claimDiscount = discountUserService.claimDiscount(request, data, logprefix, productVariantId);

        return ResponseEntity.status(claimDiscount.getStatus()).body(claimDiscount);
    }

    @Operation(summary = "Give discount to user.", description = "To give discount coupon to user.")
    @PostMapping("/give-discount-coupon")
    public ResponseEntity<HttpResponse> giveDiscountCoupon(HttpServletRequest request,
                                                      @RequestBody DiscountClaimRequest data) {

        String logprefix = "claimDiscount";
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String token = data.getToken();

        // Get the claims from the appToken
        CentralAuthTokenDetails centralAuthTokenDetails = jwtUtils.parseAppToken(token);
        String appId = centralAuthTokenDetails.getAppId();

        if (!"loyalty-service".equals(appId)) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            return ResponseEntity.status(response.getStatus()).body(response);
        }

        if (data.getDiscountCode() == null || data.getUserPhoneNumber() == null) {
            response.setStatus(HttpStatus.EXPECTATION_FAILED, "Invalid payload");
            return ResponseEntity.status(response.getStatus()).body(response);
        }

        // Build discount user
        DiscountUserRequest discountUserRequest = new DiscountUserRequest();
        discountUserRequest.setDiscountCode(data.getDiscountCode());
        discountUserRequest.setUserPhoneNumber(data.getUserPhoneNumber());

        // Claim discount process
        HttpResponse claimDiscount = discountUserService.claimDiscount(request, discountUserRequest, logprefix, null);

        return ResponseEntity.status(claimDiscount.getStatus()).body(claimDiscount);
    }

    @Operation(summary = "Update a discount user status.", description = "To update a discount user status.")
    @PutMapping("/update-user-status")
    public ResponseEntity<HttpResponse> updateUserStatus(HttpServletRequest request,
            @RequestBody UserUpdateRequest data) {

        String logprefix = "updateUserStatus";
        HttpResponse response = new HttpResponse(request.getRequestURI());

        Optional<Discount> discountOpt = discountRepository.findByDiscountCode(data.getDiscountCode());

        if (discountOpt.isPresent()) {

            try {

                String msisdn = MsisdnUtil.formatMsisdn(data.getUserPhoneNumber());

                // set id and user phone number for DiscountUserId type
                DiscountUserId discountUserId = new DiscountUserId();
                discountUserId.setDiscountId(discountOpt.get().getId());
                discountUserId.setUserPhoneNumber(msisdn);

                Optional<DiscountUser> discountUserOpt = discountUserRepository.findById(discountUserId);

                if (discountUserOpt.isPresent()) {

                    // check if the found discount user status is NEW
                    if (discountUserOpt.get().getStatus() != DiscountUserStatus.NEW) {

                        String message = "";
                        if (discountUserOpt.get().getStatus() == DiscountUserStatus.REDEEMED) {
                            message = "Invalid payload: already REDEEMED";

                        } else {
                            message = "Invalid payload: already EXPIRED";

                        }

                        response.setStatus(HttpStatus.EXPECTATION_FAILED, message);
                        Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                                "Discount User: " + Integer.toString(HttpStatus.EXPECTATION_FAILED.value())
                                        + " need to be NEW");
                        return ResponseEntity.status(response.getStatus()).body(response);

                    } else {
                        DiscountUser body = discountUserService.updateStatus(data);

                        response.setData(body);
                        response.setStatus(HttpStatus.OK);

                    }

                } else {
                    response.setStatus(HttpStatus.NOT_FOUND, "Discount User does not exist");
                    Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                            "Discount User: " + Integer.toString(HttpStatus.NOT_FOUND.value())
                                    + " has not been registered");
                    return ResponseEntity.status(response.getStatus()).body(response);
                }

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("ERRROR:::" + e.getMessage());

                response.setStatus(HttpStatus.BAD_REQUEST);
                Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                        "Exception " + e.getMessage());
            }

        } else {
            response.setStatus(HttpStatus.NOT_FOUND, "Discount code does not exist");
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Discount Code: " + Integer.toString(HttpStatus.NOT_FOUND.value())
                            + " has not been registered");
            return ResponseEntity.status(response.getStatus()).body(response);
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Operation(summary = "Delete discount product variant.", description = "To delete in batch of discount product variant.")
    @DeleteMapping("/delete-discount-variant")
    public ResponseEntity<HttpResponse> deleteDiscountProductVariant(HttpServletRequest request,
            @RequestBody DeleteDiscountVariant[] data) {

        String logprefix = "deleteDiscountProductVariant";
        HttpResponse response = new HttpResponse(request.getRequestURI());

        if (data != null) {

            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix, "Received Data: ", data);

            try {

                String message = "";
                String[] results = discountProductVariantService.deleteDiscountProductVariant(data);

                if (results[0] != "empty") {

                    // Concatenate elements of the results array
                    StringBuilder messageBuilder = new StringBuilder(
                            "Discount Product Variant Name(s) that cannot be deleted due to having users: ");
                    for (String result : results) {
                        messageBuilder.append(result);
                        messageBuilder.append(", "); // Add space between elements
                    }
                    message = messageBuilder.toString().trim(); // Trim to remove trailing space

                    // Set data in the response
                    response.setData(results);

                } else {
                    message = "All given Discount Product Variant IDs has been deleted.";
                }

                // Set message in the response
                response.setStatus(HttpStatus.OK, message);

            } catch (Exception e) {

                e.printStackTrace();
                System.out.println("ERRROR:::" + e.getMessage());

                response.setStatus(HttpStatus.BAD_REQUEST);
                Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                        "Exception " + e.getMessage());
            }

        } else {
            response.setStatus(HttpStatus.EXPECTATION_FAILED, "Invalid payload");
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Operation(summary = "Get discounted price.", description = "To calculate discounted price based on converted price and user discount.")
    @GetMapping("/list-discounted-price")
    public ResponseEntity<HttpResponse> getUserDiscount(HttpServletRequest request,
            @RequestParam(required = true) String userPhoneNumber,
            @RequestParam(required = true) Double convertedPrice,
            @RequestParam(required = true) Integer productVariantId) {

        String logprefix = "getUserDiscount";

        HttpResponse response = new HttpResponse(request.getRequestURI());

        try {

            userPhoneNumber = MsisdnUtil.formatMsisdn(userPhoneNumber);

            // check if user phone number entries exist
            Optional<DiscountUser> discountUserOpt = discountUserRepository
                    .getDiscountUserByUserPhoneNumber(userPhoneNumber)
                    .stream().findFirst();

            if (discountUserOpt.isPresent()) {

                // get the list of the related discounts and user claimed discounts with phone
                // number
                List<ListedUserDiscount> listListedUserDiscounts = discountUserService
                        .getListedUserDiscount(userPhoneNumber);


                // get the list of the related discount product variant with product variant id
                List<DiscountProductVariant> listDiscountProductVariants = discountProductVariantService
                        .getDiscountProductByProductVariantId(productVariantId);

                // initialize null variable for getting the specific discount
                List<ListedUserDiscount> calculatedDiscounts = new ArrayList<>(); // Initialize an array list to hold
                                                                                  // calculated discounts;

                // iterate first and second lists
                for (ListedUserDiscount listListedUserDiscount : listListedUserDiscounts) {

                    // check for user discount status and discount status
                    if (listListedUserDiscount.getRelatedDiscountUser().getStatus().equals(DiscountUserStatus.NEW)) {

                        if (listListedUserDiscount.getRelatedDiscount().getStatus().equals(DiscountStatus.ACTIVE)) {

                            Date currentDate = new Date();
                            Date startDate = listListedUserDiscount.getRelatedDiscount().getStartDate();
                            Date endDate = listListedUserDiscount.getRelatedDiscount().getEndDate();

                            // Normalize the dates to midnight
                            currentDate = setTimeToMidnight(currentDate);
                            startDate = setTimeToMidnight(startDate);
                            endDate = setTimeToMidnight(endDate);

                            if ((currentDate.compareTo(startDate) >= 0) && (currentDate.compareTo(endDate) <= 0)) {

                                for (DiscountProductVariant listDiscountProductVariant : listDiscountProductVariants) {

                                    // find equal value of discount id between the lists
                                    if (listDiscountProductVariant.getDiscountId().equals(listListedUserDiscount
                                            .getRelatedDiscountUser().getDiscountId())) {

                                        calculatedDiscounts.add(listListedUserDiscount);
                                    }
                                }
                            }
                        }
                    }
                }

                // if exist, check if meet min spending, then calculate and set discounted price
                if (!calculatedDiscounts.isEmpty()) {

                    for (ListedUserDiscount calculatedDiscount : calculatedDiscounts) {

                        if (calculatedDiscount.getRelatedDiscount().getMinimumSpend() > convertedPrice) {
                            calculatedDiscount.getRelatedDiscount()
                                    .setDiscountedPrice(null);

                        } else {
                            calculatedDiscount.getRelatedDiscount()
                                    .setDiscountedPrice(discountService.getDiscountedPrice(
                                            calculatedDiscount.getRelatedDiscount(), convertedPrice));
                        }
                    }

                    response.setData(calculatedDiscounts);
                    response.setStatus(HttpStatus.OK);

                } else {
                    response.setStatus(HttpStatus.NOT_FOUND,
                            "No valid discount was found. Note that discount status must be ACTIVE and user discount status must be NEW.");
                    Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                            "Data criteria given: " + Integer.toString(HttpStatus.NOT_FOUND.value())
                                    + " no discount with stated criteria was found");
                    return ResponseEntity.status(response.getStatus()).body(response);
                }

            } else {
                response.setStatus(HttpStatus.NOT_FOUND, "User phone number does not exist");
                Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                        "User phone Number: " + Integer.toString(HttpStatus.NOT_FOUND.value())
                                + " has not been registered");
                return ResponseEntity.status(response.getStatus()).body(response);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERRROR:::" + e.getMessage());

            response.setStatus(HttpStatus.BAD_REQUEST);
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // Helper method to set time to 00:00:00
    public static Date setTimeToMidnight(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
