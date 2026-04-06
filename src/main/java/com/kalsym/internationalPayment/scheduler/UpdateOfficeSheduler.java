package com.kalsym.internationalPayment.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.kalsym.internationalPayment.InternationalPaymentApplication;
import com.kalsym.internationalPayment.model.OfficeList;
import com.kalsym.internationalPayment.model.dao.ValidateBill;
import com.kalsym.internationalPayment.repositories.OfficeListRepository;
import com.kalsym.internationalPayment.services.WSPRequestService;
import com.kalsym.internationalPayment.utility.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UpdateOfficeSheduler {

    @Autowired
    OfficeListRepository officeListRepository;

    @Autowired
    WSPRequestService wspRequestService;

    @Value("${productIdOfficeSchedule:613}")
    Integer productId;

    @Value("${wspProductCodeOfficeSchedule:NPC0000_NEA_ELBIL_00}")
    String wspProductCode;

    @Scheduled(cron = "${updateOfficelist:0 0 6 * * ?}")
    public void updateOffice() {
        String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
        try {
            Logger.application.info("updateOffice : Method {}", methodName);

            List<OfficeList> processingOfficeLists = officeListRepository.findAllByProductId(productId);

            ValidateBill bill = wspRequestService.requestValidateBill(wspProductCode, "", "queryOffice");

            if (bill.getPlanList() != null) {
                if (!processingOfficeLists.isEmpty()) {
                    for (OfficeList officeList : processingOfficeLists) {
                        officeListRepository.delete(officeList);
                    }
                }
                for (JsonNode offices : bill.getPlanList()) {
                    OfficeList newOfficeList = new OfficeList();
                    // Check if the office has "planId" and get its text value as String for
                    // officeCode
                    if (offices.has("planId")) {
                        String officeCode = offices.get("planId").asText(); // Convert JsonNode to String
                        newOfficeList.setOfficeCode(officeCode);
                    }
                    // Check if the office has "planName" and get its text value as String for
                    // officeName
                    if (offices.has("planName")) {
                        String officeName = offices.get("planName").asText(); // Convert JsonNode to String
                        newOfficeList.setOfficeName(officeName);
                    }
                    newOfficeList.setProductId(productId);
                    officeListRepository.save(newOfficeList);
                }
            }

        } catch (Exception e) {
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION,
                    "updateOffice() Exception " + e.getMessage());
        }

    }
}
