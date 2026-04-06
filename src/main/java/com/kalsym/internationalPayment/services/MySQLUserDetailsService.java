package com.kalsym.internationalPayment.services;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kalsym.internationalPayment.model.MySQLUserDetails;
import com.kalsym.internationalPayment.model.User;
import com.kalsym.internationalPayment.repositories.UserRepository;
 
// https://www.codejava.net/frameworks/spring-boot/user-registration-and-login-tutorial
// https://www.bezkoder.com/spring-boot-login-example-mysql/
@Service
public class MySQLUserDetailsService implements UserDetailsService {
 
    //example tutorial 
    @Autowired
    private UserRepository userRepository;
     
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        System.out.println("username1111"+username);
        
        User user = userRepository.findByEmail(username).orElseGet(() -> {
            // This block will be executed if the user is not found by email
            // You can add additional checks here
            // For example, checking by another attribute like username or phone number
            User userByPhoneNumber = userRepository.findByPhoneNumber(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));
            // If the user is found by username, return that user
            return userByPhoneNumber;
        });

                System.out.println("user1111"+user);


        return MySQLUserDetails.build(user);
    }

    //kalsym sample

    public Integer generateRandomCode() {
        Random rNo = new Random();
        final Integer code = rNo.nextInt((999999 - 100000) + 1) + 100000;// generate six digit of code
        return code;
    }
 
}