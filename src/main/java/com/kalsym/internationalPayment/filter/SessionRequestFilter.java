package com.kalsym.internationalPayment.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.kalsym.internationalPayment.services.MySQLUserDetailsService;
import com.kalsym.internationalPayment.utility.JwtUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class SessionRequestFilter extends OncePerRequestFilter {

    public static final String HEADER_STRING = "Authorization";

    public static final String TOKEN_PREFIX = "Bearer ";
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private MySQLUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {

            final String authHeader = request.getHeader(HEADER_STRING);

            String accessToken = null;

            if (authHeader != null) {
                // Token is in the form "Bearer token". Remove Bearer word and get only the
                // Token
                if (authHeader.startsWith("Bearer ")) {
                    accessToken = authHeader.replace("Bearer ", "");
                }
            }

            if (accessToken != null && jwtUtils.validateJwtToken(accessToken)) {

                String username = jwtUtils.getUserNameFromJwtToken(accessToken);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
        }

        filterChain.doFilter(request, response);

    }

    // private String parseJwt(HttpServletRequest request) {
    // // String jwt = jwtUtils.getJwtFromCookies(request);//for cookies
    // implementation
    // String jwt = jwtUtils.getHeaderBearer(request);
    // return jwt;
    // }
}
