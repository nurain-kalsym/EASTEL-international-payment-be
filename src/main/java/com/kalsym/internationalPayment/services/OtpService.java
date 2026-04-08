package com.kalsym.internationalPayment.services;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kalsym.internationalPayment.InternationalPaymentApplication;
import com.kalsym.internationalPayment.model.VerificationCode;
import com.kalsym.internationalPayment.repositories.VerificationCodeRepository;
import com.kalsym.internationalPayment.utility.Logger;
import com.kalsym.internationalPayment.utility.MsisdnUtil;

@Service
public class OtpService {
    
    @Autowired
    VerificationCodeRepository verificationCodeRepository;

    public String createOtp(String email){

        try {
            Random rNo = new Random();
            final Integer code = rNo.nextInt((999999 - 100000) + 1) + 100000;// generate six digit of code
    
            VerificationCode vcBody = new VerificationCode();
            vcBody.setEmail(email);
            vcBody.setCode(code.toString());
            vcBody.setExpiry(new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1)));// add 1 minute
            verificationCodeRepository.save(vcBody);

            return code.toString();

        } catch (Exception e) {
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, "createOtp",
                                "Exception " + e.getMessage());

            return null;
        }

    }

    public List<VerificationCode> getOtpForPhone(String phoneNumber) {

        phoneNumber = MsisdnUtil.formatMsisdn(phoneNumber);

        List<VerificationCode> getData = verificationCodeRepository.findByPhoneNumberAndExpiryGreaterThanEqual(
                phoneNumber, new Date(System.currentTimeMillis()));

        // sort latest data
        return getData.stream()
                .sorted(Comparator.comparing(VerificationCode::getExpiry).reversed())
                .collect(Collectors.toList());
    }

    public List<VerificationCode> getOtpForEmail(String email) {

        List<VerificationCode> getData = verificationCodeRepository.findByEmailAndExpiryGreaterThanEqual(email,
                new Date(System.currentTimeMillis()));

        // sort latest data
        List<VerificationCode> sortLatest = getData.stream()
                .sorted(Comparator.comparing(VerificationCode::getExpiry).reversed())
                .collect(Collectors.toList());

        return sortLatest;
    }
}
