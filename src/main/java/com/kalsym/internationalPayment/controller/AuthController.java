package com.kalsym.internationalPayment.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kalsym.internationalPayment.InternationalPaymentApplication;
import com.kalsym.internationalPayment.model.JwtBody;
import com.kalsym.internationalPayment.model.LoginRequest;
import com.kalsym.internationalPayment.model.MySQLUserDetails;
import com.kalsym.internationalPayment.model.OTPRequest;
import com.kalsym.internationalPayment.model.RequestBodyData;
import com.kalsym.internationalPayment.model.User;
import com.kalsym.internationalPayment.model.VerificationCode;
import com.kalsym.internationalPayment.model.enums.UserStatus;
import com.kalsym.internationalPayment.repositories.UserRepository;
import com.kalsym.internationalPayment.services.SmsService;
import com.kalsym.internationalPayment.services.UserService;
import com.kalsym.internationalPayment.utility.*;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/auth")
public class AuthController {

        @Autowired
        UserService userService;

        @Autowired
        UserRepository userRepository;
        
        @Autowired
        AuthenticationManager authenticationManager;

        @Autowired
        JwtUtils jwtUtils;

        @Autowired
        SmsService smsService;

        /**
         * ------------------------------------------------------------------------------------------------------------------------------------------------
         * Register/Login/Token related endpoints
         * --------------------------------------------------------------------------------------------------------------------------------------------------------------------------
         */

        @Operation(summary = "User registration", description = "To register user to the system (DEALER/ADMIN)")
        @PostMapping(path = "/register") //✅
        public ResponseEntity<HttpResponse> registerUser(
                HttpServletRequest request,
                @RequestBody User userBody) {

            HttpResponse response = new HttpResponse(request.getRequestURI());
            String logprefix = "register";

                if (userBody.getPhoneNumber() != null && userBody.getPassword() != null) {

                        String msisdn = MsisdnUtil.formatMsisdn(userBody.getPhoneNumber());

                        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "User phone: ",
                                msisdn);

                        try {

                        Optional<User> userOpt = userRepository.findByPhoneNumber(msisdn);

                        if (userOpt.isPresent()) {
                                response.setStatus(HttpStatus.EXPECTATION_FAILED, "Phone number already taken");
                                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                                        "Phone number: " + HttpStatus.CONFLICT.value()
                                                + " already taken");
                                return ResponseEntity.status(response.getStatus()).body(response);

                        }

                        User body = userService.userRegistration(userBody);

                        response.setData(body);
                        response.setStatus(HttpStatus.OK);

                        } catch (DataIntegrityViolationException e) {
                        e.printStackTrace();

                        if (e.getMessage().contains("User_un_phone")) {

                                response.setStatus(HttpStatus.EXPECTATION_FAILED, "Phone number already taken");
                                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                                        "Phone number: " + Integer.toString(HttpStatus.CONFLICT.value())
                                                + " already taken");
                        } else if (e.getMessage().contains("User_un_email")) {

                                response.setStatus(HttpStatus.EXPECTATION_FAILED, "Email already taken");
                                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                                        "Email: " + Integer.toString(HttpStatus.CONFLICT.value())
                                                + " already taken");
                        } else {

                                response.setStatus(HttpStatus.EXPECTATION_FAILED, e.getMessage());
                                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                                        "DataIntegrityViolationException : " + e.getMessage());
                        }

                        }
                } else {
                        response.setStatus(HttpStatus.EXPECTATION_FAILED, "Invalid payload");
                }

                return ResponseEntity.status(response.getStatus()).body(response);

        }

        @Operation(summary = "User login", description = "To login to the system (DEALER/ADMIN)")
        @PostMapping("/sign-in") //✅
        public ResponseEntity<?> authenticateUser(
                HttpServletRequest request,
                @RequestBody LoginRequest loginRequest) {

                String logprefix = "sign-in";
                MySQLUserDetails userDetails = null;
                Optional<User> optUser = Optional.empty(); 

                String msisdn = loginRequest.getPhoneNumber();
                if (loginRequest.getEmail() == null) {
                        msisdn = MsisdnUtil.formatMsisdn(msisdn);
                        optUser = userService.findUserByPhoneNumber(msisdn);
                } else {
                        optUser = userRepository.findByEmail(loginRequest.getEmail());
                }

                if (!optUser.isPresent()) {
                        String message = "";
                        if (loginRequest.getEmail() == null)
                        message = "User not found : " + msisdn;
                        else
                        message = "User not found : " + loginRequest.getEmail();
                        HttpResponse response = new HttpResponse(request.getRequestURI());
                        response.setStatus(HttpStatus.UNAUTHORIZED, "Incorrect credentials");
                        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                                message);
                        return ResponseEntity.status(response.getStatus()).body(response);
                }

                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "User Id: ",
                        optUser.get().getId());

                if (optUser.get().getStatus().equals(UserStatus.INACTIVE)) {
                        HttpResponse response = new HttpResponse(request.getRequestURI());
                        response.setStatus(HttpStatus.UNAUTHORIZED, "User unauthorized");
                        return ResponseEntity.status(response.getStatus()).body(response);
                }

                Authentication authentication = authenticationManager
                        .authenticate(
                                new UsernamePasswordAuthenticationToken(optUser.get().getEmail(),
                                        loginRequest.getPassword()));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                userDetails = (MySQLUserDetails) authentication.getPrincipal();
                String jwtToken = jwtUtils.getJwtToken(userDetails);

                JwtBody jwtBody = new JwtBody();
                jwtBody.setJwt(jwtToken);

                return ResponseEntity.ok()
                        .header("Authorization", "Bearer " + jwtToken)
                        .body(jwtBody);
        }

        @Operation(summary = "User logout", description = "To log out from the system")
        @PostMapping("/sign-out") //✅
        public ResponseEntity<?> logoutUser() {
                return ResponseEntity.ok()
                        .body("You've been signed out!");
        }

        @Operation(summary = "Token refresh", description = "To refresh token without needing to re-login")
        @PostMapping("/refresh") //✅
        public ResponseEntity<?> refreshToken(HttpServletRequest request,
                @RequestBody JwtBody jwt) {
                HttpResponse response = new HttpResponse(request.getRequestURI());

                try {
                        String username = jwtUtils.getUserNameFromJwtToken(jwt.getJwt());

                        Optional<User> userOptional = userRepository.findByEmail(username);
                        if (userOptional.isPresent() && userOptional.get().getStatus().equals(UserStatus.INACTIVE)) {
                        response.setStatus(HttpStatus.UNAUTHORIZED, "User unauthorized");
                        return ResponseEntity.status(response.getStatus()).body(response);
                        }

                } catch (Exception e) {
                        response.setStatus(HttpStatus.BAD_REQUEST, "Invalid token",
                                Integer.toString(HttpStatus.UNAUTHORIZED.value()));
                        Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, "refreshToken",
                                "Exception " + e.getMessage());
                        return ResponseEntity.status(response.getStatus()).body(response);
                }

                // generate new token
                String jwtToken = jwtUtils.refreshToken(jwt.getJwt());

                JwtBody jwtBody = new JwtBody();
                jwtBody.setJwt(jwtToken);

                return ResponseEntity.ok()
                        .header("Authorization", "Bearer " + jwtToken)
                        .body(jwtBody);
        }

        /* @Operation(summary = "", description = "")
        @PostMapping(path = "/login-oauth") //❓
        public ResponseEntity<?> loginOauth(@RequestBody ValidateOauthRequest body,
                HttpServletRequest request) throws Exception {
                String logprefix = "login-oauth";
                String userEmail = "";

                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "body : ", body);

                Optional<User> userOpt = userService.findUserByEmail(userEmail);

                // if data not exist then we create the user
                if (!userOpt.isPresent()) {

                        User userData = new User();
                        userData.setEmail(userEmail);
                        userData.setFullName(body.getName());

                        User saveData = userService.userSocialLoginRegistration(userData);

                        String jwtToken = jwtUtils.getJwtToken(saveData);

                        JwtBody jwtBody = new JwtBody();
                        jwtBody.setJwt(jwtToken);

                        return ResponseEntity.ok()
                                .header("Authorization", "Bearer " + jwtToken)
                                .body(jwtBody);
                }

                // else we generate the token for user that has registered under google
                String jwtToken = jwtUtils.getJwtToken(userOpt.get());

                JwtBody jwtBody = new JwtBody();
                jwtBody.setJwt(jwtToken);

                return ResponseEntity.ok()
                        .header("Authorization", "Bearer " + jwtToken)
                        .body(jwtBody);
        } */


    
        /**
         * ------------------------------------------------------------------------------------------------------------------------------------------------
         * OTP related endpoints
         * --------------------------------------------------------------------------------------------------------------------------------------------------------------------------
         */

        @Operation(summary = "Request OTP", description = "To request OTP code via phone/email")
        @PostMapping("/request-otp") //❓
        public ResponseEntity<HttpResponse> sendOtpRequest(HttpServletRequest request,
                @RequestBody RequestBodyData requestBodyData) {

                HttpResponse response = new HttpResponse(request.getRequestURI());
                String logPrefix = "sendOtpRequest";

                try {

                        if (requestBodyData.getDestAddr() != null) {
                        String msisdn = MsisdnUtil.formatMsisdn(requestBodyData.getDestAddr());
                        Optional<User> optUser = userService.findUserByPhoneNumber(msisdn);

                        if (!optUser.isPresent()) {
                                response.setStatus(HttpStatus.NOT_FOUND,
                                        "Number Not Found : "
                                                + msisdn);
                                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                                        "Number Not Found : "
                                                + msisdn);

                                return ResponseEntity.status(response.getStatus()).body(response);
                        }

                        //temp comment
                        //smsService.sendHttpGetRequest(msisdn, message, true);
                        response.setData("SUCCESS");
                        response.setStatus(HttpStatus.OK);
                        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix, "OTP SENT For : ",
                                msisdn);
                        } else {
                        // send OTP to email
                        }


                } catch (Exception e) {
                        response.setStatus(HttpStatus.EXPECTATION_FAILED, e.getMessage());
                        Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                                "Exception " + e.getMessage());
                }

                return ResponseEntity.status(response.getStatus()).body(response);

        }

        @Operation(summary = "Request registration OTP", description = "To request OTP specifically for registration.")
        @PostMapping("/request-registration-otp") //❓
        public ResponseEntity<HttpResponse> sendRegistrationOtpRequest(HttpServletRequest request,
                @RequestBody RequestBodyData requestBodyData) {

                HttpResponse response = new HttpResponse(request.getRequestURI());
                String message = "OTP: ";
                String logPrefix = "sendRegistrationOtpRequest";

                try {

                        String msisdn = MsisdnUtil.formatMsisdn(requestBodyData.getDestAddr());

                        Optional<User> optUser = userService.findUserByPhoneNumber(msisdn);

                        // Throw error if exist
                        if (optUser.isPresent()) {
                        response.setStatus(HttpStatus.CONFLICT,
                                "Phone number already registered");
                        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                                "Phone number already registered: "
                                        + msisdn);

                        return ResponseEntity.status(response.getStatus()).body(response);
                        }

                        if (requestBodyData.getEmail() != null) {
                        Optional<User> optUserEmail = userService.findUserByEmail(requestBodyData.getEmail());

                        // Throw error if exist
                        if (optUserEmail.isPresent()) {
                                response.setStatus(HttpStatus.CONFLICT,
                                        "Email already registered");
                                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                                        "Email already registered: "
                                                + requestBodyData.getEmail());

                                return ResponseEntity.status(response.getStatus()).body(response);
                        }
                        }

                        smsService.sendHttpGetRequest(msisdn, message, true);
                        response.setData("SUCCESS");
                        response.setStatus(HttpStatus.OK);
                        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix, "OTP SENT To : ",
                                msisdn);

                } catch (Exception e) {
                        response.setStatus(HttpStatus.EXPECTATION_FAILED, e.getMessage());
                        Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                                "Exception " + e.getMessage());
                }

                return ResponseEntity.status(response.getStatus()).body(response);

        }

        @Operation(summary = "Verify OTP", description = "To verify OTP")
        @PostMapping("/confirm-verification-code") //❓
        public ResponseEntity<?> confirmVerificationCode(HttpServletRequest request,
            @RequestBody() OTPRequest OTPRequestBody) {
                String logPrefix = "confirmVerificationCode";

                HttpResponse response = new HttpResponse(request.getRequestURI());

                try {

                String msisdn = MsisdnUtil.formatMsisdn(OTPRequestBody.getMsisdn());

                List<VerificationCode> vcResult = userService
                        .getPhoneRegistrationCode(msisdn);

                if (vcResult.isEmpty()) {
                        response.setStatus(HttpStatus.EXPECTATION_FAILED, "TAC Code Expired");
                        Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                                "TAC Code Number For: " + msisdn + " Expired");

                        return ResponseEntity.status(response.getStatus()).body(response);
                }

                VerificationCode vc = vcResult.get(0);

                if (vc.getCode().equals(OTPRequestBody.getTacCode())) {
                        response.setStatus(HttpStatus.OK, "TAC Validated");
                        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                                "TAC Validated For : " + msisdn);
                } else {
                        response.setStatus(HttpStatus.EXPECTATION_FAILED, "Wrong TAC Code Number");
                        Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                                "Wrong TAC Code Number For: " + msisdn);
                }
                return ResponseEntity.status(response.getStatus()).body(response);
                } catch (Exception e) {
                response.setStatus(HttpStatus.EXPECTATION_FAILED);
                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                        "Exception " + e.getMessage());
                return ResponseEntity.status(response.getStatus()).body(response);
                }

        }



    
}
