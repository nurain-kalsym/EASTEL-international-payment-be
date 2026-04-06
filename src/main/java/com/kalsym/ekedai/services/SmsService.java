package com.kalsym.ekedai.services;

import java.io.UnsupportedEncodingException;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.kalsym.ekedai.EkedaiApplication;
import com.kalsym.ekedai.model.VerificationCode;
import com.kalsym.ekedai.repositories.VerificationCodeRepository;
import com.kalsym.ekedai.utility.Logger;

import java.net.URLEncoder;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

@Service
public class SmsService {

    @Autowired
    VerificationCodeRepository verificationCodeRepository;

    @Value("${umgUser}")
    String umgUser;

    @Value("${umgPassword}")
    String umgPassword;

    @Value("${umgSenderAddress}")
    String umgSenderAddress;

    @Value("${umgUrl:https://umgotp.hellosim.com.my/api/processMsg.php}")
    String umgUrl;

    @Value("${umgUrl.nonotp:https://umgnonotp.hellosim.com.my/api/processMsg.php}")
    String umgUrlNonOtp;

    public String sendHttpGetRequest(String destAddr, String message, Boolean otp) {
        try {
            // Set up the URL and parameters
            String url = umgUrl;
            String cmd = "submitMT";
            String systemId = umgUser;
            String password = umgPassword;
            String srcAddr = umgSenderAddress;
            String dataCoding = "0";

            String newMessage = "";

            if (otp) {
                Random rNo = new Random();
                final Integer code = rNo.nextInt((999999 - 100000) + 1) + 100000;// generate six digit of code

                VerificationCode vcBody = new VerificationCode();
                vcBody.setPhoneNumber(destAddr);
                vcBody.setCode(code.toString());
                vcBody.setExpiry(new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5)));// add 5 minutes
                verificationCodeRepository.save(vcBody);
                newMessage = message + code.toString();
            } else {
                url = umgUrlNonOtp;
                newMessage = message;
            }

            // Encode the message parameter
            String encodedMessage = URLEncoder.encode(newMessage, "UTF-8");

            // Construct the full URL with parameters
            String fullUrl = String.format(
                    "%s?Cmd=%s&SystemId=%s&Password=%s&Message=%s&SrcAddr=%s&DestAddr=%s&DataCoding=%s",
                    url, cmd, systemId, password, encodedMessage, srcAddr, destAddr, dataCoding);

            // Create headers with Content-Type set to application/json
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create an HTTP entity with headers
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Create a RestTemplate instance
            RestTemplate restTemplate = new RestTemplate();

            // Send the GET request
            ResponseEntity<String> response = restTemplate.exchange(fullUrl, HttpMethod.GET, entity, String.class);
            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, "fullUrl", fullUrl);

            // Process the response
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, "sendHttpGetRequest",
                        "Request failed. Status code: " + response.getStatusCodeValue());
                return "Request failed. Status code: " + response.getStatusCodeValue();
            }
        } catch (UnsupportedEncodingException e) {
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, "sendHttpGetRequest",
                    "UnsupportedEncodingException " + e.getMessage());
            return "Error encoding message parameter: " + e.getMessage();
        }
    }
}
