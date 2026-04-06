package com.kalsym.internationalPayment.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.kalsym.internationalPayment.filter.SessionRequestFilter;
import com.kalsym.internationalPayment.services.MySQLUserDetailsService;

import java.util.Arrays;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    // @Autowired
    // MySQLUserDetailsService userDetailsService;

    // @Bean
    // public AuthenticationProvider userDetailsAuthProvider() {
    // DaoAuthenticationProvider a = new DaoAuthenticationProvider();
    // a.setUserDetailsService(userDetailsService);
    // a.setPasswordEncoder(encoder());
    // return a;
    // }

    // @Bean
    // public AuthenticationManager authenticationManager() {
    // return new ProviderManager(userDetailsAuthProvider());
    // }

    @Value("${allowed.origins}")
    private String allowedOrigins;

    @Autowired
    private SessionAuthenticationEntryPoint unauthorizedHandler;

    @Autowired
    private SessionRequestFilter sessionRequestFilter;

    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(MySQLUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder, PhoneNumberAuthenticationProvider phoneNumberAuthenticationProvider) {
        DaoAuthenticationProvider daoAuthProvider = new DaoAuthenticationProvider();
        daoAuthProvider.setUserDetailsService(userDetailsService);
        daoAuthProvider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(Arrays.asList(daoAuthProvider, phoneNumberAuthenticationProvider));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/**",
                    "/user/**",
                    "/payment/payment-channel/**",
                    "/payment/exchange/currency/**",
                    "/payment/validateBill/**",
                    "/countries/**",
                    "/dashboard/**",
                    "/banner/**",
                    "/discount/list-discounted-price",
                    "/discount/give-discount-coupon",
                    "/assets/image/**",
                    "/product/**",
                    "/callback",
                    "/payment/callback",
                    "/payment/**/status",
                    "/error",
                    "/v2/api-docs",
                    "/payment/request/payment",
                    "/swagger-resources",
                    "/swagger-resources/**",
                    "/configuration/ui",
                    "/configuration/**",
                    "/configuration/security",
                    "/swagger-ui.html",
                    "/session",
                    "/webjars/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers("/admin/**").hasAnyAuthority("ADMIN", "SUPERADMIN")
                .anyRequest().authenticated()
            )
            .headers(headers -> headers
                .defaultsDisabled()
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; object-src 'none'; frame-ancestors 'none'; base-uri 'self';")
                )
                .frameOptions(frame -> frame.sameOrigin())
            );

        http.addFilterBefore(sessionRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CORS configuration source for http.cors()
    private UrlBasedCorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);

        String[] originsArray = allowedOrigins.split(",");
        for (String origin : originsArray) {
            config.addAllowedOrigin(origin.trim());
        }

        config.setAllowedHeaders(Arrays.asList("Authorization", "Session-Token", "Content-Type", "X-App-Token"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
