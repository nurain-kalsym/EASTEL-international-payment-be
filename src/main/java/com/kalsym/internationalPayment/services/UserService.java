package com.kalsym.internationalPayment.services;

import static com.kalsym.internationalPayment.filter.SessionRequestFilter.TOKEN_PREFIX;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.kalsym.internationalPayment.model.*;
import com.kalsym.internationalPayment.model.enums.UserStatus;
import com.kalsym.internationalPayment.repositories.CountryRepository;
import com.kalsym.internationalPayment.repositories.ImageAssetsRepository;
import com.kalsym.internationalPayment.repositories.UserRepository;
import com.kalsym.internationalPayment.repositories.VerificationCodeRepository;
import com.kalsym.internationalPayment.utility.Base64Util;
import com.kalsym.internationalPayment.utility.MsisdnUtil;

import io.jsonwebtoken.Jwts;

import jakarta.persistence.criteria.Predicate;

@Service
public class UserService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    VerificationCodeRepository verificationCodeRepository;

    @Autowired
    private PasswordEncoder bcryptEncoder;

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Autowired
    CountryRepository countryRepository;

    @Autowired
    ImageAssetsRepository imageAssetsRepository;

    public User userRegistration(User user) {

        if (user.getPhoneNumber() != null) {
            String msisdn = MsisdnUtil.formatMsisdn(user.getPhoneNumber());
            user.setPhoneNumber(msisdn);
        }
        
        user.setEmail(user.getEmail());
        user.setFullName(user.getFullName());
        user.setNationality(user.getNationality());
        user.setLanguage(user.getLanguage());
        user.setIsEnable(true);// always set to true
        user.setIsFirstTimeLogin(false); //always set to false
        user.setStatus(UserStatus.ACTIVE);
    
        String password = bcryptEncoder.encode(user.getPassword());
        user.setPassword(password);
        
        if (user.getRole() != null && user.getRole().equalsIgnoreCase("admin")) {
            user.setRole("ADMIN");
        } else {
            user.setRole("DEALER");
        }
        
        // set default profile image based on nationality
        Country country = countryRepository.findByNationality(user.getNationality());
        String profileImageId = null;
        // If country exists
        if (country != null) {
            // Set image id based on country code
            profileImageId = "profile_default_" + country.getCountryCode();

            // But need to check first if it exists
            Optional<ImageAssets> optionalImageAssets = imageAssetsRepository.findById(profileImageId);

            // If imageId does not exist, set profile image to null
            if (!optionalImageAssets.isPresent()) {
                profileImageId = null;
            }
        }

        // Set imageId
        user.setImageId(profileImageId);
        
        Date date = new Date();
        user.setCreated(date);
        user.setUpdated(date);

        return userRepository.save(user);
    }

    public User userProfileUpdate(User user) {

        String msisdn = MsisdnUtil.formatMsisdn(user.getPhoneNumber());
        user.setPhoneNumber(msisdn);

        return userRepository.save(user);
    }

    public User userProfileResetPassword(User user) {

        Date now = new Date();

        String pin = bcryptEncoder.encode(user.getPassword());
        user.setPassword(pin);
        user.setIsFirstTimeLogin(true);
        user.setUpdated(now);

        return userRepository.save(user);
    }

    public User userSocialLoginRegistration(User user) {

        user.setIsEnable(true);// set to false first , after success verification code then we can set it to true
        user.setRole("DEALER");
        user.setStatus(UserStatus.ACTIVE);

        return userRepository.save(user);
    }

    public Optional<User> findUserByPhoneNumber(String phoneNumber) {

        return userRepository.findByPhoneNumber(phoneNumber);
    }

    public Optional<User> findUserByEmail(String email) {

        return userRepository.findByEmail(email);
    }

    public Optional<User> findUserById(String id) {

        return userRepository.findById(id);
    }

    public Optional<User> optionalUserById(String id) {

        return userRepository.findById(id);

    }

    public User userById(String id) {

        Optional<User> optResult = userRepository.findById(id);

        return optResult.get();

    }

    public User userByRole(String role) {

        Optional<User> optResult = userRepository.findByRole(role);

        return optResult.get();

    }

    public Integer generateRandomCode() {
        Random rNo = new Random();
        final Integer code = rNo.nextInt((999999 - 100000) + 1) + 100000;// generate six digit of code
        return code;
    }

    public User getUser(String token) {
        try {
            String userIdentifier = Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(token.replace(TOKEN_PREFIX, ""))
                    .getBody()
                    .getSubject();

            // Attempt to find the user by phone number
            Optional<User> optionalUserPhoneNo = userRepository.findByPhoneNumber(userIdentifier);
            if (optionalUserPhoneNo.isPresent()) {
                return optionalUserPhoneNo.get();
            }

            // If not found by phone number, attempt to find by email
            Optional<User> optionalUser = userRepository.findByEmail(userIdentifier);
            return optionalUser.orElse(null);

        } catch (Exception e) {
            return null;
        }
    }

    public String encodeUuidBase64(String uuid) {

        byte[] uuidByte = Base64Util.uuidToByte(uuid);
        String uuidBased64 = Base64Util.byteToBase64(uuidByte);

        return uuidBased64;
    }

    public String decodeBase64(String uuid) {

        byte[] uuidByte = Base64Util.stringBase64ToByte(uuid);
        String uuidBased64 = Base64Util.byteOfBased64toUuid(uuidByte);

        return uuidBased64;
    }

    public Specification<User> getUsersSpec(Date from, Date to, UserStatus status, String globalSearch,
            List<String> roles) {
        return (root, query, builder) -> {
            final List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("created"), from));
            }
            if (to != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(to);
                calendar.add(Calendar.DAY_OF_MONTH, 1); // To include the end date in the search
                predicates.add(builder.lessThanOrEqualTo(root.get("created"), calendar.getTime()));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (globalSearch != null) {
                predicates.add(builder.or(
                        builder.like(root.get("fullName"), "%" + globalSearch + "%"),
                        builder.like(root.get("phoneNumber"), "%" + globalSearch + "%"),
                        builder.like(root.get("email"), "%" + globalSearch + "%")));
            }
            if (roles != null && !roles.isEmpty()) {
                List<String> normalizedRoles = roles.stream().map(String::toUpperCase).collect(Collectors.toList());

                Predicate rolePredicate = root.get("role").in(normalizedRoles);
                predicates.add(rolePredicate);
            }

            return builder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
