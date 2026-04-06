package com.kalsym.ekedai.configuration;

import com.kalsym.ekedai.filter.CustomHeaderFilter;

import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public DateTimeFormatter dateTimeFormatter() {
        return DateTimeFormatter.RFC_1123_DATE_TIME;
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonObjectMapperCustomization() {
        return jacksonObjectMapperBuilder -> jacksonObjectMapperBuilder.timeZone(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));
    }

    @Bean
    public FilterRegistrationBean<CustomHeaderFilter> customHeaderFilterRegistration(CustomHeaderFilter customHeaderFilterBean) {
        FilterRegistrationBean<CustomHeaderFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(customHeaderFilterBean);
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }

}