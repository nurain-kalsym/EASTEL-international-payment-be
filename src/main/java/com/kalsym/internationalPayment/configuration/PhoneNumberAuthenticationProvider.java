package com.kalsym.internationalPayment.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.kalsym.internationalPayment.model.User;
import com.kalsym.internationalPayment.repositories.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class PhoneNumberAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private UserRepository userRepository;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String phoneNumber = authentication.getName();
        Optional<User> userOptional = userRepository.findByPhoneNumber(phoneNumber);

        if (!userOptional.isPresent()) {
            throw new BadCredentialsException("Incorrect credentials");
        }

        User user = userOptional.get();

        List<GrantedAuthority> grantedAuthorities = new ArrayList<GrantedAuthority>();
        grantedAuthorities.add(new SimpleGrantedAuthority(user.getRole()));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(phoneNumber, null,
                grantedAuthorities);
        return auth;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}