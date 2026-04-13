package com.kalsym.internationalPayment.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import com.kalsym.internationalPayment.model.ResetPasswordRequest;
import com.kalsym.internationalPayment.model.User;
import com.kalsym.internationalPayment.model.VerificationCode;
import com.kalsym.internationalPayment.model.enums.UserStatus;
import com.kalsym.internationalPayment.repositories.UserRepository;
import com.kalsym.internationalPayment.services.EmailService;
import com.kalsym.internationalPayment.services.OtpService;
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
        EmailService emailService;

        @Autowired
        OtpService otpService;

        @Autowired
        private PasswordEncoder passwordEncoder;

        /**
         * ------------------------------------------------------------------------------------------------------------------------------------------------
         * Register/Login/Token related endpoints
         * --------------------------------------------------------------------------------------------------------------------------------------------------------------------------
         */

        @Operation(summary = "User registration", description = "To register user to the system (DEALER/ADMIN)")
        @PostMapping(path = "/register") 
        public ResponseEntity<HttpResponse> registerUser(
                HttpServletRequest request,
                @RequestBody User userBody) {

            HttpResponse response = new HttpResponse(request.getRequestURI());
            String logprefix = "registerUser";

                if (userBody.getEmail() != null && userBody.getPassword() != null  && userBody.getFullName() != null) {
                        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "User email: ",
                                userBody.getEmail());

                        try {
                                Optional<User> userOpt = userRepository.findByEmail(userBody.getEmail());

                                if (userOpt.isPresent()) {
                                        response.setStatus(HttpStatus.EXPECTATION_FAILED, "Email address already taken.");
                                        Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                                                "Email: " + userBody.getEmail() + " already taken.");
                                        return ResponseEntity.status(response.getStatus()).body(response);
                                }

                                User body = userService.userRegistration(userBody);

                                response.setData(body);
                                response.setStatus(HttpStatus.OK);

                        } catch (DataIntegrityViolationException e) {
                                e.printStackTrace();

                                if (e.getMessage().contains("User_un_email")) {

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
                        response.setStatus(HttpStatus.BAD_REQUEST, "Invalid payload. Missing either email, full name or password.");
                        Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "Invalid payload. Missing either email, full name or password.");
                }

                return ResponseEntity.status(response.getStatus()).body(response);

        }

        @Operation(summary = "User login", description = "To login to the system (DEALER/ADMIN)")
        @PostMapping("/sign-in")
        public ResponseEntity<?> authenticateUser(
                HttpServletRequest request,
                @RequestBody LoginRequest loginRequest) {

                HttpResponse response = new HttpResponse(request.getRequestURI());
                String logprefix = "authenticateUser";
                MySQLUserDetails userDetails = null;
                Optional<User> optUser = Optional.empty(); 

                if (loginRequest.getEmail() == null || loginRequest.getPassword() == null) {
                        response.setStatus(HttpStatus.BAD_REQUEST, "Invalid payload. Missing either email or password.");
                        Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "Invalid payload. Missing either email or password.");
                        return ResponseEntity.status(response.getStatus()).body(response);
                }
                
                optUser = userRepository.findByEmail(loginRequest.getEmail());
                if (!optUser.isPresent()) {
                        response.setStatus(HttpStatus.UNAUTHORIZED, "Incorrect credentials.");
                        Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                                "User not found : " + loginRequest.getEmail());
                        return ResponseEntity.status(response.getStatus()).body(response);
                }

                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "User ID: ",
                        optUser.get().getId());

                if (optUser.get().getStatus().equals(UserStatus.INACTIVE)) {
                        response.setStatus(HttpStatus.UNAUTHORIZED, "User unauthorized.");
                        Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "User status is INACTVE.");
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

                Map<String, Object> rspBody = new HashMap<>();
                rspBody.put("userId", optUser.get().getId());
                rspBody.put("isFirstTimeLogin", optUser.get().getIsFirstTimeLogin());
                rspBody.put("jwt", jwtToken);

                response.setStatus(HttpStatus.OK);
                response.setData(rspBody);

                return ResponseEntity.status(response.getStatus()).body(response);
        }

        @Operation(summary = "User logout", description = "To log out from the system")
        @PostMapping("/sign-out")
        public ResponseEntity<?> logoutUser() {
                return ResponseEntity.ok().body("You've been signed out!");
        }

        @Operation(summary = "Token refresh", description = "To refresh token without needing to re-login")
        @PostMapping("/refresh")
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

        
        @Operation(summary = "Forgot password", description = "To reset/forgot password")
        @PostMapping("/forgot-password/reset")
        public ResponseEntity<?> forgotPassword(HttpServletRequest request, @RequestBody ResetPasswordRequest reqBody) {
                HttpResponse response = new HttpResponse(request.getRequestURI());
                String logprefix = "forgotPassword";

                Optional<User> userOpt = userService.findUserByEmail(reqBody.getEmail());

                if (!userOpt.isPresent()) {
                           response.setStatus(HttpStatus.NOT_FOUND, "User not found",
                                Integer.toString(HttpStatus.NOT_FOUND.value()));
                        Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                                "User with email : " + reqBody.getEmail() + " Not Found");
                        return ResponseEntity.status(response.getStatus()).body(response);
                }

                User userData = userOpt.get();

                // checking password reset qualification
                if (!reqBody.getNewPassword().equals(reqBody.getConfirmNewPassword())) {
                        response.setStatus(HttpStatus.EXPECTATION_FAILED, "The password confirmation do not match.",
                                Integer.toString(HttpStatus.EXPECTATION_FAILED.value()));
                        Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                                "The password confirmation do not match.");
                        return ResponseEntity.status(response.getStatus()).body(response);
                }

                if (passwordEncoder.matches(reqBody.getNewPassword(), userData.getPassword())) {
                        response.setStatus(HttpStatus.BAD_REQUEST,
                                "New password cannot be the same as old password.");
                        return ResponseEntity.status(response.getStatus()).body(response);
                }

                try {
                        // allow user to reset
                        userData.setPassword(reqBody.getNewPassword());
                        User saveData = userService.userProfileResetPassword(userData);
                        response.setData(saveData);
                        response.setStatus(HttpStatus.OK, "Password reset is successful.");

                        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                                "User email : " + reqBody.getEmail() + " password reset");

                } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println("e.getMessage()" + e.getMessage());
                        response.setStatus(HttpStatus.EXPECTATION_FAILED, e.getMessage(),
                                Integer.toString(HttpStatus.EXPECTATION_FAILED.value()));
                        Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                                "Exception " + e.getMessage());
                        return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(response);
                }

                return ResponseEntity.status(response.getStatus()).body(response);

                
        }

        /**
         * ------------------------------------------------------------------------------------------------------------------------------------------------
         * OTP related endpoints
         * --------------------------------------------------------------------------------------------------------------------------------------------------------------------------
         */

        @Operation(summary = "Request OTP", description = "To request OTP code via email")
        @PostMapping("/request-otp") 
        public ResponseEntity<HttpResponse> sendOtpRequest(HttpServletRequest request,
                @RequestBody RequestBodyData requestBodyData) {

                HttpResponse response = new HttpResponse(request.getRequestURI());
                String logPrefix = "sendOtpRequest";

                if (requestBodyData.getEmail() == null) {
                        response.setStatus(HttpStatus.BAD_REQUEST, "Invalid payload. Missing email.");
                        Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix, "Invalid payload. Missing email.");
                }

                try {
                        Optional<User> optUser = userService.findUserByEmail(requestBodyData.getEmail());

                        if (!optUser.isPresent()) {
                                response.setStatus(HttpStatus.NOT_FOUND, "Email Not Found : "+ requestBodyData.getEmail());
                                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                                        "Email Not Found : " + requestBodyData.getEmail());

                                return ResponseEntity.status(response.getStatus()).body(response);
                        }

                        // generate OTP
                        User user = optUser.get();
                        String otpCode = otpService.createOtp(user.getEmail());
                        
                        if (otpCode == null) {
                                response.setStatus(HttpStatus.EXPECTATION_FAILED, "OTP Code failed to generate.");
                                return ResponseEntity.status(response.getStatus()).body(response);
                        }
                        
                        // send otp to email
                        emailService.sendOtpEmail(otpCode, user.getFullName(), user.getEmail());

                        response.setStatus(HttpStatus.OK);
                        response.setMessage("OTP has been requested successfully.");
                        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix, "OTP sent for email: ",
                                requestBodyData.getEmail());

                } catch (Exception e) {
                        response.setStatus(HttpStatus.EXPECTATION_FAILED, e.getMessage());
                        Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                                "Exception " + e.getMessage());
                }

                return ResponseEntity.status(response.getStatus()).body(response);

        }

        @Operation(summary = "Verify OTP", description = "To verify OTP")
        @PostMapping("/verify-otp") 
        public ResponseEntity<?> confirmVerificationCode(HttpServletRequest request,
            @RequestBody() OTPRequest OTPRequestBody) {
                String logPrefix = "confirmVerificationCode";

                HttpResponse response = new HttpResponse(request.getRequestURI());

                try {
                        List<VerificationCode> vcResult = otpService.getOtpForEmail(OTPRequestBody.getEmail());

                        if (vcResult.isEmpty()) {
                                response.setStatus(HttpStatus.EXPECTATION_FAILED, "OTP Code Expired");
                                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                                        "OTP Code For: " + OTPRequestBody.getEmail() + " Expired");

                                return ResponseEntity.status(response.getStatus()).body(response);
                        }

                        VerificationCode vc = vcResult.get(0);

                        if (vc.getCode().equals(OTPRequestBody.getTacCode())) {
                                response.setStatus(HttpStatus.OK, "OTP Validated");
                                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                                        "OTP Validated For : " + OTPRequestBody.getEmail());
                        } else {
                                response.setStatus(HttpStatus.EXPECTATION_FAILED, "Wrong OTP Code");
                                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                                        "Wrong OTP Code For: " + OTPRequestBody.getEmail());
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
