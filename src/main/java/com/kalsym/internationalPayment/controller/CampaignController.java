package com.kalsym.internationalPayment.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.kalsym.internationalPayment.InternationalPaymentApplication;
import com.kalsym.internationalPayment.model.Campaign;
import com.kalsym.internationalPayment.model.CampaignCriteria;
import com.kalsym.internationalPayment.model.Transaction;
import com.kalsym.internationalPayment.model.User;
import com.kalsym.internationalPayment.model.dao.CampaignDTO;
import com.kalsym.internationalPayment.model.dao.CriterionTypeDto;
import com.kalsym.internationalPayment.model.enums.Operator;
import com.kalsym.internationalPayment.model.enums.TransactionEnum;
import com.kalsym.internationalPayment.model.enums.VariantType;
import com.kalsym.internationalPayment.repositories.CountryRepository;
import com.kalsym.internationalPayment.repositories.TransactionRepository;
import com.kalsym.internationalPayment.services.CampaignService;
import com.kalsym.internationalPayment.services.UserService;
import com.kalsym.internationalPayment.utility.HttpResponse;
import com.kalsym.internationalPayment.utility.Logger;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/campaign")
public class CampaignController {

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CountryRepository countryRepository;

    @PostMapping
    public ResponseEntity<HttpResponse> createCampaign(HttpServletRequest request, @RequestBody CampaignDTO campaign) {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logPrefix = "createCampaign";

        try {
            Campaign createdCampaign = campaignService.createCampaign(campaign);

            response.setStatus(HttpStatus.CREATED);
            response.setData(createdCampaign);

        } catch (Exception e) {
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                    "Error while creating campaign: " + e.getMessage());

            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            response.setData("Could not create the campaign: " + e.getMessage());
        }
        return ResponseEntity.status(response.getStatus()).body(response);

    }

    @GetMapping
    public ResponseEntity<HttpResponse> getAllCampaigns(HttpServletRequest request,
                                                        @RequestParam(defaultValue = "createdAt", required = false) String sortBy,
                                                        @RequestParam(defaultValue = "ASC", required = false) String sortingOrder,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int pageSize,
                                                        @RequestParam(required = false) Boolean isActive,
                                                        @RequestParam(required = false) String search,
                                                        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start, // campaign starts
                                                        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end, // campaign ends
                                                        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
                                                        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        // String logPrefix = "getAllCampaigns";

        Page<Campaign> campaigns = campaignService.getAllCampaigns(sortBy, sortingOrder, page, pageSize, isActive, search,
                from, to, start, end);

        response.setStatus(HttpStatus.OK);
        response.setData(campaigns);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<HttpResponse> updateCampaignStatus(
            HttpServletRequest request,
            @PathVariable("id") String id,
            @RequestParam("isActive") boolean isActive) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logPrefix = "updateCampaignStatus";

        try {
            Campaign updatedCampaign = campaignService.updateCampaignStatus(id, isActive);
            response.setData(updatedCampaign);
            response.setStatus(HttpStatus.OK);
        } catch (Exception e) {
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                    "Error while updating campaign status: " + e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setMessage(e.getMessage());
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HttpResponse> updateCampaign(
            HttpServletRequest request,
            @PathVariable("id") String id,
            @RequestBody CampaignDTO campaign) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logPrefix = "updateCampaign";

        try {
            Campaign updatedCampaign = campaignService.updateCampaign(id, campaign);

            response.setStatus(HttpStatus.OK);
            response.setData(updatedCampaign);

        } catch (Exception e) {
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                    "Error while updating campaign: " + e.getMessage());

            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setData("Could not update the campaign. " + e.getMessage());
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HttpResponse> getCampaignById(
            HttpServletRequest request,
            @PathVariable String id) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        // String logPrefix = "getCampaignById";

        Campaign campaign = campaignService.getCampaignById(id);
        if (campaign != null) {
            response.setStatus(HttpStatus.OK);
            response.setData(campaign);
        } else {
            response.setStatus(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> deleteCampaign(@PathVariable String id) {
        boolean isDeleted = true;
        try {
            campaignService.deleteCampaign(id);
        } catch (Exception e) {
            isDeleted = false;
        }

        return new ResponseEntity<>(isDeleted, isDeleted ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping(path = { "/validateTransaction/{transactionId}" })
    public ResponseEntity<HttpResponse> validateTransaction(HttpServletRequest request,
                                                          @PathVariable("transactionId") String transactionId) {
        HttpResponse response = new HttpResponse(request.getRequestURI());

        Optional<Transaction> transactionOptional = transactionRepository.findByTransactionId(transactionId);

        if (!transactionOptional.isPresent()) {
            response.setStatus(HttpStatus.NOT_FOUND);
            return ResponseEntity.status(response.getStatus()).body(response);
        }
        // Get user
        Optional<User> userOpt = userService.optionalUserById(transactionOptional.get().getUserId());

        // Check for campaign
        Optional<Campaign> activeCampaign = campaignService.findAndValidateTransactionCriteria(transactionOptional.get(), userOpt.orElse(null), "validateTransaction");

        response.setStatus(HttpStatus.OK);
        response.setData(activeCampaign.orElse(null));

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping(path = { "/criteria-types"})
    public List<CriterionTypeDto> getCriterionTypes() {

        List<String> countries = countryRepository.findAllCountryCodes();

        return Arrays.asList(
                new CriterionTypeDto(
                        CampaignCriteria.CampaignCriterionType.TRANSACTION_TYPE,
                        "Transaction Type",
                        CriterionTypeDto.CriterionInput.SELECTION,
                        "text",
                        Stream.of(TransactionEnum.values()).map(Enum::name).collect(Collectors.toList()),
                        Stream.of(Operator.OR, Operator.EQUAL, Operator.NOT_EQUAL)
                                .collect(Collectors.toList())
                ),
                new CriterionTypeDto(
                        CampaignCriteria.CampaignCriterionType.TRANSACTION_AMOUNT,
                        "Transaction Amount",
                        CriterionTypeDto.CriterionInput.INPUT,
                        "number",
                        Collections.emptyList(),
                        Stream.of(Operator.GREATER, Operator.GREATER_EQUAL, Operator.LESS, Operator.LESS_EQUAL, Operator.EQUAL, Operator.NOT_EQUAL)
                                .collect(Collectors.toList())
                ),
                new CriterionTypeDto(
                        CampaignCriteria.CampaignCriterionType.PRODUCT_TYPE,
                        "Product Type",
                        CriterionTypeDto.CriterionInput.SELECTION,
                        "text",
                        Stream.of(VariantType.values()).map(Enum::name).collect(Collectors.toList()),
                        Stream.of(Operator.OR, Operator.EQUAL, Operator.NOT_EQUAL)
                                .collect(Collectors.toList())
                ),
                new CriterionTypeDto(
                        CampaignCriteria.CampaignCriterionType.PRODUCT_COUNTRY_CODE,
                        "Product Country Code",
                        CriterionTypeDto.CriterionInput.SELECTION,
                        "text",
                        countries,
                        Stream.of(Operator.OR, Operator.EQUAL, Operator.NOT_EQUAL)
                                .collect(Collectors.toList())
                ),
                new CriterionTypeDto(
                        CampaignCriteria.CampaignCriterionType.USER_SEGMENT,
                        "User Segment",
                        CriterionTypeDto.CriterionInput.SELECTION,
                        "text",
                        Arrays.asList("NEW", "LOCAL", "INTERNATIONAL"),
                        Stream.of(Operator.AND, Operator.OR, Operator.EQUAL, Operator.NOT_EQUAL)
                                .collect(Collectors.toList())
                ),
                new CriterionTypeDto(
                        CampaignCriteria.CampaignCriterionType.TRANSACTION_FREQUENCY,
                        "Transaction Frequency",
                        CriterionTypeDto.CriterionInput.INPUT,
                        "number",
                        Collections.emptyList(),
                        Stream.of(Operator.EQUAL)
                                .collect(Collectors.toList())
                )
        );
    }

    // @PostMapping("/{campaignId}/criteria")
    // public ResponseEntity<CampaignCriteria> addCampaignCriteria(@PathVariable
    // String campaignId, @RequestBody CampaignCriteria criteria) {
    // criteria.setCampaignId(campaignId);
    // CampaignCriteria createdCriteria =
    // campaignService.addCampaignCriteria(criteria);
    // return new ResponseEntity<>(createdCriteria, HttpStatus.CREATED);
    // }

    // @GetMapping("/{campaignId}/criteria")
    // public ResponseEntity<List<CampaignCriteria>>
    // getCriteriaByCampaignId(@PathVariable String campaignId) {
    // List<CampaignCriteria> criteriaList =
    // campaignService.getCriteriaByCampaignId(campaignId);
    // return new ResponseEntity<>(criteriaList, HttpStatus.OK);
    // }
}
