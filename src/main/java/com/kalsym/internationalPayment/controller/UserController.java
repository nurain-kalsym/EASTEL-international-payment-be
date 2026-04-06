package com.kalsym.internationalPayment.controller;

import com.kalsym.internationalPayment.InternationalPaymentApplication;
import com.kalsym.internationalPayment.model.*;
import com.kalsym.internationalPayment.model.dao.*;
import com.kalsym.internationalPayment.model.enums.ImageType;
import com.kalsym.internationalPayment.model.enums.UserStatus;
import com.kalsym.internationalPayment.repositories.UserRepository;
import com.kalsym.internationalPayment.services.*;
import com.kalsym.internationalPayment.utility.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
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
    private ImageAssetService imageAssetService;

    
    /**
     * ------------------------------------------------------------------------------------------------------------------------------------------------
     * Register/Login/Token related endpoints
     * --------------------------------------------------------------------------------------------------------------------------------------------------------------------------
     */

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

    @PostMapping("/sign-out") //✅
    public ResponseEntity<?> logoutUser() {
        return ResponseEntity.ok()
                .body("You've been signed out!");
    }

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
    }

    
    /**
     * ------------------------------------------------------------------------------------------------------------------------------------------------
     * User related endpoints
     * --------------------------------------------------------------------------------------------------------------------------------------------------------------------------
     */

    @GetMapping(path = { "/user-details" }) //✅
    public ResponseEntity<HttpResponse> getUserById(
            HttpServletRequest request) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "getUserById";
        try {

            User user = userService.getUser(request.getHeader(HEADER_STRING));
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix, "User Id: ",
                    user.getId());
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION,
                    DateTimeUtil.currentTimestampToString() + " @GetMapping getUserById",
                    "path: " + user.getId());

            User data = userService.userById(user.getId());
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

    @PutMapping(path = { "/{id}/change-password" }) //✅
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
            response.setStatus(HttpStatus.OK, "Password change is successful.");

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

    @PutMapping(path = { "/reset-password" }) // first-time login password reset //✅
    public ResponseEntity<HttpResponse> resetPassword(HttpServletRequest request,
            @RequestBody ChangePasswordRequest changePasswordRequest) {
        String logprefix = "resetPassword";

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

        // check if already first time login
        if (userData.getIsFirstTimeLogin()) {
            response.setStatus(HttpStatus.EXPECTATION_FAILED,
                    "Account has already reset their password for first time login.");
            return ResponseEntity.status(response.getStatus()).body(response);
        }
        
        // checking password reset qualification
        if (!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), userData.getPassword())) {
            response.setStatus(HttpStatus.BAD_REQUEST,
                    "Old Password is incorrect.");
            return ResponseEntity.status(response.getStatus()).body(response);
        }

        if (!changePasswordRequest.getNewPassword().equals(changePasswordRequest.getConfirmNewPassword())) {
            response.setStatus(HttpStatus.BAD_REQUEST,
                    "The password confirmation do not match.");
            return ResponseEntity.status(response.getStatus()).body(response);
        }

        if (passwordEncoder.matches(changePasswordRequest.getNewPassword(), userData.getPassword())) {
            response.setStatus(HttpStatus.BAD_REQUEST,
                    "New password cannot be the same as old password.");
            return ResponseEntity.status(response.getStatus()).body(response);
        }

        // allow user to reset if it is enable
        userData.setPassword(changePasswordRequest.getNewPassword());
        User saveData = userService.userProfileResetPassword(userData);
        response.setData(saveData);
        response.setStatus(HttpStatus.OK, "Password reset is successful.");

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PutMapping(path = { "/{id}/change-user-profile" }) //✅
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

    @PutMapping(path = { "/{id}/change-user-status" }) //✅
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

    @PutMapping(path = { "/{id}/change-language-profile" }) //✅
    public ResponseEntity<HttpResponse> changeLanguageProfile(HttpServletRequest request,
            @PathVariable String id,
            @RequestBody ChangeNationality reqBody) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "changeLanguageProfile";

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

    @GetMapping("/get-users") //✅
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

    @PostMapping(path = "/{userId}/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) //✅
     public ResponseEntity<HttpResponse> postProfilePicture(
                HttpServletRequest request,
                @PathVariable("userId") String userId,
                @RequestPart(value = "file", required = true) MultipartFile file
     ) throws Exception {

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


    /**
     * ------------------------------------------------------------------------------------------------------------------------------------------------
     * OTP related endpoints
     * --------------------------------------------------------------------------------------------------------------------------------------------------------------------------
     */

    @PostMapping("/request-otp") //❓
    public ResponseEntity<HttpResponse> sendOtpRequest(HttpServletRequest request,
            @RequestBody RequestBodyData requestBodyData) {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String message = "OTP: ";
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
