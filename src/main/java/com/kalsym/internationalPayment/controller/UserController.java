package com.kalsym.internationalPayment.controller;

import com.kalsym.internationalPayment.InternationalPaymentApplication;
import com.kalsym.internationalPayment.model.*;
import com.kalsym.internationalPayment.model.dao.*;
import com.kalsym.internationalPayment.model.enums.ImageType;
import com.kalsym.internationalPayment.model.enums.UserStatus;
import com.kalsym.internationalPayment.repositories.UserRepository;
import com.kalsym.internationalPayment.repositories.WalletRepository;
import com.kalsym.internationalPayment.services.*;
import com.kalsym.internationalPayment.utility.*;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;

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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import static com.kalsym.internationalPayment.filter.SessionRequestFilter.HEADER_STRING;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/users")
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
        private PasswordEncoder passwordEncoder;

        @Autowired
        private ImageAssetService imageAssetService;

        @Autowired
        private WalletRepository walletRepository;

    
        /**
         * ------------------------------------------------------------------------------------------------------------------------------------------------
         * User related endpoints
         * --------------------------------------------------------------------------------------------------------------------------------------------------------------------------
         */

        @Operation(summary = "Get user details", description = "To retrieve user's details. ie: fullname, email, nationatity, etc")
        @GetMapping(path = { "/details" }) 
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
                        Optional<Wallet> wallet = walletRepository.findByUserId(user.getId());
                        if (wallet.isPresent()) {
                                data.setWallet(wallet.get());
                        }

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

        @Operation(summary = "Update/Change password", description = "To let user change their password")
        @PutMapping(path = { "/{id}/change-password" }) 
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
                if (!passwordEncoder.matches(reqBody.getCurrentPassword(), userData.getPassword())) {
                        response.setStatus(HttpStatus.BAD_REQUEST,
                                "Old Password is incorrect.");
                        return ResponseEntity.status(response.getStatus()).body(response);
                }

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

        @Hidden
        @Operation(summary = "Update user profile", description = "To update user profile. ie: fullname, nationality, email, etc")
        @PutMapping(path = { "/{id}/change-profile" })
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

        @Operation(summary = "Update user status", description = "To update status to ACTIVE/INACTIVE")
        @PutMapping(path = { "/{id}/change-status" }) 
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

        @Operation(summary = "Update user language", description = "To update user's preffered language")
        @PutMapping(path = { "/{id}/change-language" })
        public ResponseEntity<HttpResponse> changeLanguageProfile(HttpServletRequest request,
                @PathVariable String id,
                @RequestBody ChangeLanguage reqBody) {
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

        @Operation(summary = "Get all users", description = "To retrieve all users with filter/pagination. ie: date range, search, etc")
        @GetMapping("/pagination")
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

        @Operation(summary = "Upload profile picture", description = "To upload user's profile picture")
        @PostMapping(path = "/upload/profile-picture/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) //✅
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


}
