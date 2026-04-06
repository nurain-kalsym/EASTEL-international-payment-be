package com.kalsym.internationalPayment.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kalsym.internationalPayment.InternationalPaymentApplication;
import com.kalsym.internationalPayment.model.*;
import com.kalsym.internationalPayment.model.dao.*;
import com.kalsym.internationalPayment.model.enums.ImageType;
import com.kalsym.internationalPayment.model.enums.TransactionEnum;
import com.kalsym.internationalPayment.model.enums.UserStatus;
import com.kalsym.internationalPayment.repositories.UserRepository;
import com.kalsym.internationalPayment.services.*;
import com.kalsym.internationalPayment.utility.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import static com.kalsym.internationalPayment.filter.SessionRequestFilter.HEADER_STRING;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/user")
public class UserController {

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

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private UserDocumentService userDocumentService;

    @Autowired
    private ImageAssetService imageAssetService;

    @Autowired
    private UserMergeService userMergeService;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private DiscountUserService discountUserService;

    @Autowired
    private SymplifiedOrderService symplifiedOrderService;

    @Autowired
    private PaymentController paymentController;

    // Default channel name
    @Value("${channel.name:e-kedai}")
    private String channelName;

    @PostMapping(path = "/test-campaign")
    public ResponseEntity<HttpResponse> testUserCampaign(HttpServletRequest request,
            String phoneNumber) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logPrefix = "testUserCampaign";

        String msisdn = MsisdnUtil.formatMsisdn(phoneNumber);
        Optional<User> userOpt = userRepository.findByPhoneNumber(msisdn);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Handle active campaign and issue reward
            handleActiveCampaignAndReward(user, logPrefix, request);
            response.setStatus(HttpStatus.OK);
        } else {
            response.setStatus(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.status(response.getStatus()).body(response);

    }

    @PostMapping(path = "/register")
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

                // Handle active campaign and issue reward
                handleActiveCampaignAndReward(body, logprefix, request);

                // Create user document
                if (body.getNationality() != null) {
                    try {
                        UserDocumentRequest userDocumentRequest = new UserDocumentRequest();
                        userDocumentRequest.setUserId(body.getId());
                        userDocumentRequest
                                .setDocumentType(!body.getNationality().equals("Malaysian") ? "PASSPORT" : "MYKAD");
                        userDocumentRequest.setDocumentNumber(userBody.getDocumentNo());
                        userDocumentRequest.setStatus(UserDocument.UserDocumentStatus.PENDING_UPLOAD);

                        // Create user document
                        userDocumentService.createUserDocument(userDocumentRequest);
                        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                                "User document created");

                    } catch (Exception e) {
                        Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                                "User document creation error: " + e.getMessage());
                    }
                }

                // Register to mongodb user profile
                try {
                    ProfileServiceResponse profileServiceResponse = profileService.createUser(
                            body.getPhoneNumber(),
                            body.getFullName(), body.getEmail(), body.getReferral(), body.getNationality(), false,
                            null);
                    Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            "Customer profile service: ",
                            profileServiceResponse.getMessage());
                } catch (Exception e) {
                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            "Customer profile service error: " + e.getMessage());
                }

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

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(
            HttpServletRequest request,
            @RequestBody LoginRequest loginRequest) {

        String logprefix = "signin";
        MySQLUserDetails userDetails = null;
        Optional<User> optUser = Optional.empty(); // Initialize the Optional as empty
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

    @PostMapping("/signout")
    public ResponseEntity<?> logoutUser() {
        return ResponseEntity.ok()
                .body("You've been signed out!");
    }

    @GetMapping(path = { "/userDetails" })
    public ResponseEntity<HttpResponse> getUserById(
            HttpServletRequest request) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "userDetails";
        try {

            User user = userService.getUser(request.getHeader(HEADER_STRING));
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "User Id: ",
                    user.getId());
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION,
                    DateTimeUtil.currentTimestampToString() + " @GetMapping getUserById",
                    "path: " + user.getId());

            User data = userService.userById(user.getId());

            List<String> documentTypes = Arrays.asList("PASSPORT", "MYKAD");
            // List<String> documentTypes = Collections.singletonList("PASSPORT");
            // Get user documents for this user
            List<UserDocument> userDocuments = userDocumentService
                    .getUserDocumentsByUserIdAndDocumentTypes(user.getId(), documentTypes);

            String userDocumentStatus = "N/A";
            if (!userDocuments.isEmpty()) {
                userDocumentStatus = userDocuments.get(0).getStatus().toString();
            }
            data.setDocumentStatus(userDocumentStatus);

            response.setStatus(HttpStatus.OK);
            response.setData(data);

        } catch (Exception e) {
            response.setStatus(HttpStatus.UNAUTHORIZED, "Unauthorized user",
                    Integer.toString(HttpStatus.UNAUTHORIZED.value()));
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());

        }

        return ResponseEntity.status(response.getStatus()).body(response);

    }

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

    // //authentication
    @PostMapping(path = "/loginoauth")
    public ResponseEntity<?> loginOauth(@RequestBody ValidateOauthRequest body,
            HttpServletRequest request) throws Exception {
        String logprefix = "loginOauth";
        String userEmail = "";

        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "body : ", body);

        Optional<User> userOpt = userService.findUserByEmail(userEmail);

        // if data not exist then we create the user
        if (!userOpt.isPresent()) {

            User userData = new User();
            userData.setEmail(userEmail);
            userData.setFullName(body.getName());
            userData.setChannel(body.getLoginType());

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
    }

    @PutMapping(path = { "/{id}/changepassword" })
    public ResponseEntity<HttpResponse> changePassword(HttpServletRequest request,
            @PathVariable String id,
            @RequestBody ChangePasswordRequest reqBody) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "changePassword";

        Optional<User> userOpt = userService.optionalUserById(id);

        if (!userOpt.isPresent()) {
            response.setStatus(HttpStatus.NOT_FOUND, "User not found",
                    Integer.toString(HttpStatus.NOT_FOUND.value()));
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "User with id : " + id + " Not Found");
            return ResponseEntity.status(response.getStatus()).body(response);
        }
        User userData = userOpt.get();
        // checking first the new and confirm new password is correct
        if (!reqBody.getNewPassword().equals(reqBody.getConfirmNewPassword())) {

            response.setStatus(HttpStatus.EXPECTATION_FAILED, "Confirm new password not same",
                    Integer.toString(HttpStatus.EXPECTATION_FAILED.value()));
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Confirm password not same with password");
            return ResponseEntity.status(response.getStatus()).body(response);

        }

        try {

            // verify current password
            authenticationManager
                    .authenticate(
                            new UsernamePasswordAuthenticationToken(userData.getEmail(),
                                    reqBody.getCurrentPassword()));

        } catch (BadCredentialsException e) {
            response.setStatus(HttpStatus.FORBIDDEN, "Bad Credentiails",
                    Integer.toString(HttpStatus.FORBIDDEN.value()));
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "BadCredentialsException " + e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } catch (AuthenticationException e) {
            response.setStatus(HttpStatus.FORBIDDEN, e.getLocalizedMessage(),
                    Integer.toString(HttpStatus.FORBIDDEN.value()));
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "AuthenticationException " + e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        try {
            // allow user to reset if it is enable
            userData.setPassword(reqBody.getNewPassword());
            User saveData = userService.userProfileResetPassword(userData);
            response.setData(saveData);
            response.setStatus(HttpStatus.OK, "Success Change Password");

            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "User id : " + id + " password changed");

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

    @PutMapping(path = { "/resetpassword" })
    public ResponseEntity<HttpResponse> changePassword(HttpServletRequest request,
            @RequestBody ChangePasswordRequest changePasswordRequest) {
        String logprefix = "resetpassword";

        HttpResponse response = new HttpResponse(request.getRequestURI());

        String msisdn = MsisdnUtil.formatMsisdn(changePasswordRequest.getMsisdn());

        Optional<User> userOpt = userService.findUserByPhoneNumber(msisdn);

        if (!userOpt.isPresent()) {
            response.setStatus(HttpStatus.NOT_FOUND, "User not found",
                    Integer.toString(HttpStatus.NOT_FOUND.value()));
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "User with id : " + msisdn + " Not Found");
            return ResponseEntity.status(response.getStatus()).body(response);
        }

        User userData = userOpt.get();
        // checking first the new and confirm new password is correct
        if (!changePasswordRequest.getNewPassword().equals(changePasswordRequest.getConfirmNewPassword())) {
            response.setStatus(HttpStatus.EXPECTATION_FAILED,
                    "The password confirmation do not match");
            return ResponseEntity.status(response.getStatus()).body(response);
        }

        if (passwordEncoder.matches(changePasswordRequest.getNewPassword(), userData.getPassword())) {
            response.setStatus(HttpStatus.EXPECTATION_FAILED,
                    "New password cannot be the same as old password");
            return ResponseEntity.status(response.getStatus()).body(response);
        }

        // allow user to reset if it is enable
        userData.setPassword(changePasswordRequest.getNewPassword());
        User saveData = userService.userProfileResetPassword(userData);
        response.setData(saveData);
        response.setStatus(HttpStatus.OK, "Success Change Password");

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PutMapping(path = { "/{id}/change-user-profile" })
    public ResponseEntity<HttpResponse> changeUserProfile(HttpServletRequest request,
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String id,
            @RequestBody ChangeUserProfile reqBody) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "changeUserProfile";

        String accessToken = null;

        Optional<User> userOpt = userService.optionalUserById(id);
        if (!userOpt.isPresent()) {
            response.setStatus(HttpStatus.NOT_FOUND, "User not found",
                    Integer.toString(HttpStatus.NOT_FOUND.value()));
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "User with id not found : " + id);
            return ResponseEntity.status(response.getStatus()).body(response);
        }

        if (authHeader == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED, "User unauthorized",
                    Integer.toString(HttpStatus.UNAUTHORIZED.value()));
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "User unauthorized : " + HttpStatus.UNAUTHORIZED.value());
            return ResponseEntity.status(response.getStatus()).body(response);

        }

        try {

            if (authHeader.startsWith("Bearer ")) {
                accessToken = authHeader.replace("Bearer ", "");
            }

            if (accessToken != null) {
                User userData = userOpt.get();
                if (reqBody.getEmail() != null) {
                    userData.setEmail(reqBody.getEmail());
                }
                if (reqBody.getPhoneNumber() != null) {
                    userData.setPhoneNumber(reqBody.getPhoneNumber());
                }
                if (reqBody.getChannel() != null) {
                    userData.setChannel(reqBody.getChannel());
                }
                if (reqBody.getFullName() != null) {
                    userData.setFullName(reqBody.getFullName());
                }
                if (reqBody.getNationality() != null) {
                    userData.setNationality(reqBody.getNationality());
                }
                // Update user
                User savedData = userService.userProfileUpdate(userData);

                // set response
                response.setStatus(HttpStatus.OK);
                response.setData(savedData);

                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "User id : " + id + " updated");
            } else {

                response.setStatus(HttpStatus.UNAUTHORIZED, "User unauthorized",
                        Integer.toString(HttpStatus.UNAUTHORIZED.value()));
                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "User unauthorized : " + HttpStatus.UNAUTHORIZED.value());
            }

        } catch (DataIntegrityViolationException e) {
            e.printStackTrace();

            if (e.getMessage().contains("User_un_phone")) {

                response.setStatus(HttpStatus.CONFLICT, "Phone number already taken",
                        Integer.toString(HttpStatus.CONFLICT.value()));
                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "Phone number already taken : " + HttpStatus.CONFLICT.value());
            } else if (e.getMessage().contains("User_un_email")) {

                response.setStatus(HttpStatus.CONFLICT, "Email already taken",
                        Integer.toString(HttpStatus.CONFLICT.value()));
                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "Email already taken : " + HttpStatus.CONFLICT.value());
            } else {

                response.setStatus(HttpStatus.EXPECTATION_FAILED, e.getMessage());
                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "Exception " + e.getMessage());
            }

        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PutMapping(path = { "/{id}/change-user-status" })
    public ResponseEntity<HttpResponse> changeUserStatus(HttpServletRequest request,
            @PathVariable String id,
            @RequestParam(required = true) UserStatus status) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "changeUserStatus";

        String accessToken = null;

        Optional<User> userOpt = userService.optionalUserById(id);
        if (!userOpt.isPresent()) {
            response.setStatus(HttpStatus.NOT_FOUND, "User not found",
                    Integer.toString(HttpStatus.NOT_FOUND.value()));
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "User with id not found : " + id);
            return ResponseEntity.status(response.getStatus()).body(response);
        }

        accessToken = request.getHeader(HEADER_STRING);

        if (accessToken != null) {
            try {
                User user = userService.getUser(accessToken);
                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        user.getRole() + " user is updating user status...");

                if ("ADMIN".equalsIgnoreCase(user.getRole())
                        || "SUPERADMIN".equalsIgnoreCase(user.getRole())) {
                    User userData = userOpt.get();
                    userData.setStatus(status);

                    User savedData = userService.userProfileUpdate(userData);

                    // set response
                    response.setStatus(HttpStatus.OK);
                    response.setData(savedData);

                    Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            "User id : " + id + " status updated");
                } else {
                    // set response
                    response.setStatus(HttpStatus.UNAUTHORIZED);
                    response.setMessage("User with role of " + user.getRole()
                            + " is not authorized to access this resource.");

                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            "User unauthorized : " + HttpStatus.UNAUTHORIZED.value());
                }

            } catch (DataIntegrityViolationException e) {
                e.printStackTrace();

                if (e.getMessage().contains("User_un_phone")) {

                    response.setStatus(HttpStatus.CONFLICT, "Phone number already taken",
                            Integer.toString(HttpStatus.CONFLICT.value()));
                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            "Phone number already taken : " + HttpStatus.CONFLICT.value());
                } else if (e.getMessage().contains("User_un_email")) {

                    response.setStatus(HttpStatus.CONFLICT, "Email already taken",
                            Integer.toString(HttpStatus.CONFLICT.value()));
                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            "Email already taken : " + HttpStatus.CONFLICT.value());
                } else {

                    response.setStatus(HttpStatus.EXPECTATION_FAILED, e.getMessage());
                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            "Exception " + e.getMessage());
                }

            }
        } else {
            response.setStatus(HttpStatus.UNAUTHORIZED, "User unauthorized",
                    Integer.toString(HttpStatus.UNAUTHORIZED.value()));
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "User unauthorized : " + HttpStatus.UNAUTHORIZED.value());
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PutMapping(path = { "/{id}/change-language-profile" })
    public ResponseEntity<HttpResponse> changeLanguageProfile(HttpServletRequest request,
            @PathVariable String id,
            @RequestBody ChangeNationality reqBody) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "changeLanguageProfile()";

        Optional<User> userOpt = userService.optionalUserById(id);
        if (!userOpt.isPresent()) {
            response.setStatus(HttpStatus.NOT_FOUND, "User not found", Integer.toString(HttpStatus.NOT_FOUND.value()));
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "User with id not found : " + id);
            return ResponseEntity.status(response.getStatus()).body(response);
        }

        try {
            User userData = userOpt.get();

            boolean isUpdateRequired = false;

            if (reqBody.getLanguage() != null) {
                userData.setLanguage(reqBody.getLanguage());
                isUpdateRequired = true;
            }

            if (reqBody.getNationality() != null) {
                userData.setNationality(reqBody.getNationality());
                isUpdateRequired = true;
            }

            if (!isUpdateRequired) {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setError("Invalid Payload: No valid fields to update");
                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "Invalid Payload: " + reqBody);
                return ResponseEntity.status(response.getStatus()).body(response);
            }

            User savedData = userService.userProfileUpdate(userData);
            response.setStatus(HttpStatus.OK);
            response.setData(savedData);

            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "User id : " + id + " updated");
        } catch (Exception exception) {
            response.setStatus(HttpStatus.EXPECTATION_FAILED);
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Exception : " + exception.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping("/requestOTP")
    public ResponseEntity<HttpResponse> sendRequest(HttpServletRequest request,
            @RequestBody RequestBodyData requestBodyData) {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String channel = "e-kedai".equalsIgnoreCase(channelName) ? "eByzarr" : channelName;
        String message = channel + ": OTP: ";
        String logPrefix = "sendRequestOTP";

        try {

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

            smsService.sendHttpGetRequest(msisdn, message, true);
            response.setData("SUCCESS");
            response.setStatus(HttpStatus.OK);
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix, "OTP SENT For : ",
                    msisdn);

        } catch (Exception e) {
            response.setStatus(HttpStatus.EXPECTATION_FAILED, e.getMessage());
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);

    }

    @PostMapping("/requestRegistrationOTP")
    public ResponseEntity<HttpResponse> sendRegistrationOTPRequest(HttpServletRequest request,
            @RequestBody RequestBodyData requestBodyData) {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String channel = "e-kedai".equalsIgnoreCase(channelName) ? "eByzarr" : channelName;
        String message = channel + ": OTP: ";
        String logPrefix = "sendRegistrationOTPRequest";

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

    @PostMapping("/confirmVerificationCode")
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

    @PostMapping(path = "/getAppUserToken")
    public ResponseEntity<?> getAppUserToken(
            HttpServletRequest request,
            @RequestHeader("X-App-Token") String appToken,
            @RequestBody(required = false) String extraData) {
        String logprefix = "getAppUserToken";

        // Get the claims from the appToken
        CentralAuthTokenDetails centralAuthTokenDetails = jwtUtils.parseAppToken(appToken);

        String appId = centralAuthTokenDetails.getAppId();
        String phoneNumber = centralAuthTokenDetails.getPhoneNumber();

        // If the claims is null, disallow the request
        if (phoneNumber == null || appId == null || !appId.equalsIgnoreCase(channelName)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Unauthorized: Access is denied due to invalid appToken provided");
        }

        phoneNumber = MsisdnUtil.formatMsisdn(phoneNumber);

        Optional<User> optionalUser = userRepository.findByPhoneNumber(phoneNumber);
        User user;
        boolean isNewUser = false;

        // Extra data from Hellosim
        // const extraData = {
        // name: user.fullName,
        // email: user.email,
        // nationality: user.nationality,
        // gender: user.gender,
        // channel: "HELLOSIM"
        // };

        // Extract extra data fields if available
        ExtraData extraDataFields = extractExtraData(extraData);

        // If email is not provided, set to default email
        if (extraDataFields.getEmail() == null) {
            extraDataFields.setEmail(String.format("%s@%s.com", phoneNumber, channelName));
        }

        // If channel is not provided, set to default channel
        if (extraDataFields.getChannel() == null) {
            extraDataFields.setChannel(channelName.toUpperCase());
        }

        // If name is not provided, set to default name
        if (extraDataFields.getName() == null) {
            extraDataFields.setName(String.format("User%s%s", phoneNumber, channelName));
        }

        // Create new account
        if (!optionalUser.isPresent() && extraDataFields.getChannel() != null && extraDataFields.getEmail() != null) {
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Creating user with phone no: ",
                    phoneNumber);

            User newUser = new User();
            newUser.setPhoneNumber(phoneNumber);
            newUser.setChannel(extraDataFields.getChannel());
            newUser.setIsEnable(true);
            newUser.setRole("CUSTOMER");
            newUser.setEmail(extraDataFields.getEmail());
            newUser.setFullName(extraDataFields.getName());
            newUser.setStatus(UserStatus.ACTIVE);
            if (extraDataFields.getNationality() != null) {
                newUser.setNationality(extraDataFields.getNationality());
            }
            user = userRepository.save(newUser);

            // Handle active campaign and issue reward
            handleActiveCampaignAndReward(user, logprefix, request);

            // Set isNewUser true
            isNewUser = true;

            // Register to customer profile service
            try {
                ProfileServiceResponse profileServiceResponse = profileService.createUser(
                        user.getPhoneNumber(),
                        user.getFullName(), user.getEmail(), null, extraDataFields.getNationality(),
                        true, extraDataFields.getDob());
                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "Customer profile service - create: ",
                        profileServiceResponse.getMessage());
            } catch (Exception e) {
                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "Customer profile service Create Error: " + e.getMessage());
            }

        } else {
            // User exists
            user = optionalUser.get();
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Found user with phone no: ",
                    phoneNumber);

            // Set Inactive user to unauthorized
            if (user.getStatus().equals(UserStatus.INACTIVE)) {
                Map<String, Object> tokenData = new HashMap<>();
                tokenData.put("jwt", null);
                tokenData.put("isNewUser", isNewUser);
                tokenData.put("isAuthorized", false);

                return ResponseEntity.ok()
                        .body(tokenData);
            }

            // Check if need to update
            boolean doUpdate = false;

            if (extraDataFields.getName() != null && !user.getFullName().equals(extraDataFields.getName())) {
                user.setFullName(extraDataFields.getName());
                doUpdate = true;
                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "Update user " + phoneNumber + " with new name: ", extraDataFields.getName());
            }
            if (extraDataFields.getEmail() != null && !user.getEmail().equals(extraDataFields.getEmail())) {
                user.setEmail(extraDataFields.getEmail());
                doUpdate = true;
                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "Update user " + phoneNumber + " with new email: ", extraDataFields.getEmail());
            }
            // Update user if flag is true
            if (doUpdate) {
                try {
                    // Update db
                    user = userRepository.save(user);
                } catch (Exception e) {
                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            "Error updating user: " + e.getMessage());
                }

                // Update customer profile service
                try {
                    ProfileServiceResponse profileServiceResponse = profileService.updateUser(
                            user.getPhoneNumber(),
                            user.getFullName(), user.getEmail(), user.getNationality(), extraDataFields.getGender(),
                            extraDataFields.getDob());
                    Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            "Customer profile service - update: ",
                            profileServiceResponse.getMessage());
                } catch (Exception e) {
                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                            "Customer profile service Update Error: " + e.getMessage());
                }
            }

        }

        // Add device token to nestjs profile service
//        if (extraDataFields.getDeviceToken() != null) {
//            try {
//                ProfileServiceResponse profileServiceResponse = profileService.postUserDeviceToken(
//                        user.getPhoneNumber(),
//                        extraDataFields.getDeviceToken());
//                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
//                        "Customer profile service - add device token: ",
//                        profileServiceResponse.getMessage());
//            } catch (Exception e) {
//                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
//                        "Customer profile service add device token Error: " + e.getMessage());
//            }
//        }

        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(user.getPhoneNumber(), null));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwtToken = jwtUtils.getJwtTokenFromPhoneNumber(user.getPhoneNumber(), user.getId());

        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("jwt", jwtToken);
        tokenData.put("isNewUser", isNewUser);
        tokenData.put("isAuthorized", true);

        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + jwtToken)
                .body(tokenData);
    }

    private ExtraData extractExtraData(String extraData) {
        if (extraData == null) {
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, "getAppUserToken", "No extra data");
            return new ExtraData();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode extraDataNode = mapper.readTree(extraData);
            return new ExtraData(extraDataNode);
        } catch (Exception e) {
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, "getAppUserToken",
                    "Extra data parsing error: " + e.getMessage());
            return new ExtraData();
        }
    }

    @GetMapping("/getUsers")
    public ResponseEntity<HttpResponse> getUsers(HttpServletRequest request,
            @RequestParam(defaultValue = "created", required = false) String sortBy,
            @RequestParam(defaultValue = "ASC", required = false) String sortingOrder,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String globalSearch,
            @RequestParam(required = false) List<String> roles,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date to) {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "getUsers";
        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                "Requested");

        Sort sort = Sort.by(sortingOrder.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy);
        Pageable pageable = PageRequest.of(page, pageSize, sort);

        Page<User> users = userRepository
                .findAll(userService.getUsersSpec(from, to, status, globalSearch, roles), pageable);
        response.setStatus(HttpStatus.OK);
        response.setData(users);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping(path = { "/{userId}/profile-picture" })
    public ResponseEntity<HttpResponse> postProfilePicture(
            HttpServletRequest request,
            @PathVariable("userId") String userId,
            @RequestParam(value = "file", required = false) MultipartFile file)
            throws Exception {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logPrefix = "postProfilePicture";

        // Return error if no file provided
        if (file == null) {
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                    "No file provided");
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setMessage("No file provided");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Log the filename and userId
        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                "userId: " + userId + ", filename: " + file.getOriginalFilename());

        Optional<User> optionalUser = userRepository.findById(userId);

        // Return if no user found
        if (!optionalUser.isPresent()) {
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                    "User not found");
            response.setStatus(HttpStatus.NOT_FOUND);
            response.setMessage("User not found");

            return ResponseEntity.status(response.getStatus()).body(response);
        }

        try {
            // Save the image
            ImageAssets data = imageAssetService.saveImageAsset(file, ImageType.profile);

            User user = optionalUser.get();
            // Check if image id is not null
            if (user.getImageId() != null) {
                // Delete image
                String deleteStatus = imageAssetService.deleteImageById(user.getImageId());
                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                        "Delete image: " + deleteStatus);
            }
            // Set image id
            user.setImageId(data.getId());
            // Save the user
            user = userRepository.save(user);
            // Set image details explicitly
            user.setImageDetails(data);

            response.setStatus(HttpStatus.OK);
            response.setData(user);
        } catch (IOException e) {
            // Handle exceptions
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setMessage(e.getMessage());

            // Log the exception message
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping("/merge-account")
    public ResponseEntity<HttpResponse> mergeUserAccount(HttpServletRequest request,
            @RequestBody UserMergeRequest requestBody) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        UserMerge mergeRecord;
        try {
            // Normalize phone number
            String oldPhoneNumber = MsisdnUtil.formatMsisdn(requestBody.getOldPhoneNumber());

            mergeRecord = userMergeService.initiateMerge(requestBody.getNewUserId(), oldPhoneNumber,
                    requestBody.getMergeReason());
            response.setStatus(HttpStatus.OK);
            response.setData(mergeRecord);
        } catch (RuntimeException e) {
            response.setStatus(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    private void handleActiveCampaignAndReward(User user, String logPrefix, HttpServletRequest request) {
        // Check for campaign
        Optional<Campaign> activeCampaign = campaignService.findAndValidateUserCriteria(user, logPrefix);
        if (activeCampaign.isPresent()) {
            // Handle the user as it meets the campaign criteria
            Campaign campaign = activeCampaign.get();

            switch (campaign.getRewardType()) {
                case DISCOUNT_CODE:
                    // Give discount code if applicable
                    DiscountUserRequest discountUserRequest = new DiscountUserRequest();
                    discountUserRequest.setDiscountCode(campaign.getRewardValue());
                    discountUserRequest.setUserPhoneNumber(user.getPhoneNumber());

                    // Claim discount process
                    HttpResponse claimDiscountResponse = discountUserService.claimDiscount(request, discountUserRequest,
                            logPrefix, null);

                    if (claimDiscountResponse.getStatus() == 200) {
                        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                                "Reward " + campaign.getRewardValue() + " issued to " + user.getPhoneNumber());
                    } else {
                        Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                                "Error in issuing reward to " + user.getPhoneNumber() + ": "
                                        + claimDiscountResponse.getMessage());
                    }
                    break;

                case EXTERNAL_VOUCHER_CODE:
                    try {
                        // 1. Make placeFreeCouponGroupOrder HTTP request
                        HttpResponse httpResponse = symplifiedOrderService.claimFreeCoupon(
                                user.getPhoneNumber(),
                                campaign.getRewardValue());
                        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                                "Symplified order service - claim free coupon response: " + httpResponse.getStatus()
                                        + " - " + httpResponse.getMessage());

                        // If response data is not null
                        if (httpResponse.getData() != null) {
                            ObjectMapper objectMapper = new ObjectMapper();
                            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // Ensures
                                                                                                              // unknown
                                                                                                              // fields
                                                                                                              // are
                                                                                                              // ignored
                            SymplifiedOrderGroupResponse symplifiedOrderGroupResponse = objectMapper
                                    .convertValue(httpResponse.getData(), SymplifiedOrderGroupResponse.class);
                            PaymentController.Payment payment = new PaymentController.Payment();
                            SymplifiedOrderDTO symplifiedOrderDTO = new SymplifiedOrderDTO();
                            if (!symplifiedOrderGroupResponse.getOrderList().isEmpty()) {
                                symplifiedOrderDTO = symplifiedOrderGroupResponse.getOrderList().get(0);
                            }

                            payment.setName(user.getFullName());
                            payment.setEmail(user.getEmail());
                            payment.setPhoneNo(user.getPhoneNumber());
                            payment.setProductVariantId(1525); // Coupon with variantType FREECOUPON
                            payment.setTransactionAmount(0.0);
                            payment.setUserId(user.getId());
                            payment.setPaymentMethod("FREECOUPON");
                            payment.setSpInvoiceId(symplifiedOrderDTO.getInvoiceId());
                            payment.setSpOrderId(symplifiedOrderGroupResponse.getId());
                            payment.setPaymentEnum(TransactionEnum.COUPON);
                            payment.setExtra1("COUPON");
                            payment.setRedeemCoins(false);

                            // 2. Create transaction
                            HttpResponse httpResponseTransaction = paymentController.createTransaction(request, payment)
                                    .getBody();

                            assert httpResponseTransaction != null;
                            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                                    "Create transaction response: " + httpResponseTransaction.getStatus()
                                            + " - " + httpResponseTransaction.getMessage());

                        } else {
                            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                                    "placeFreeCouponGroupOrder HTTP request returns null");
                        }

                    } catch (HttpStatusCodeException e) {
                        // Register the custom deserializer for Date
                        Gson gson = new GsonBuilder()
                                .registerTypeAdapter(Date.class, new CustomTimestampDeserializer())
                                .create();
                        HttpResponse requestResponse = gson.fromJson(e.getResponseBodyAsString(), HttpResponse.class);

                        Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                                "Symplified order service - claim free coupon error: " + requestResponse.getMessage(),
                                e.getResponseBodyAsString());
                    } catch (Exception e) {
                        Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                                "Symplified order service - claim free coupon Exception: " + e.getMessage(), e);
                    }
                    break;

                default:
                    // Log if an unsupported value is found in the campaign rewardType
                    Logger.application.warn(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                            "Unsupported campaign rewardType: " + campaign.getRewardType());
                    break;
            }

        } else {
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix,
                    "No active campaign");
        }
    }
}
