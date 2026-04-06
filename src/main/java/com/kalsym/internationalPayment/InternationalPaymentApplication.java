package com.kalsym.internationalPayment;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class InternationalPaymentApplication {

    public static String PROTOCOLSUBDOMAIN;
    public static String CONTEXTPATH;
    public static String VERSION;

    public static String KALSYMADDRESS;
    public static String CITY;
    public static String COUNTRY;
    public static String ZIPCODE;
    public static String STATE;

    public static String MMGETTOKENURL;
    public static String MMPAYMENTURL;
    public static String MMVALIDATEOTP;

    public static String MMRESENDOTPURL;
    public static String PASS;
    public static String KEY;
    public static String HOST;

    public static String MMPAYMENTSTATUSURL;
    public static String MMREFUNDURL;

    public static String WSPTOKEN;
    public static String WSPTOKENTIMEOUT;

    @Value("${build.version:not-known}")
    String version;

    public static void main(String[] args) {
        SpringApplication.run(InternationalPaymentApplication.class, args);
    }

    @Value("${protocol.subdomain}")
    String protocolSubdomain;

    @Value("${server.servlet.context-path}")
    String contextPath;

    @Value("${mmGetTokenUrl}")
    String getMMGetTokenUrl;
    @Value("${mmPaymentUrl}")
    String mmPaymenturl;
    @Value("${mmValidateUrl}")
    String mmValidateUrl;
    @Value("${mmResendOtpUrl}")
    String mmResendOtpUrl;
    @Value("${pass}")
    String pass;
    @Value("${key}")
    String key;

    @Value("${host}")
    String host;

    @Value("${mmPaymentStatusUrl}")
    String mmPaymentStatusUrl;

    @Value("${mmRefundPaymentUrl}")
    String mmRefundPaymentUrl;

    @Bean
    CommandLineRunner lookup(ApplicationContext context) {
        return args -> {
            PROTOCOLSUBDOMAIN = protocolSubdomain;
            CONTEXTPATH = contextPath;
            VERSION = version;
            MMGETTOKENURL = getMMGetTokenUrl;
            MMPAYMENTURL = mmPaymenturl;
            MMVALIDATEOTP = mmValidateUrl;
            MMRESENDOTPURL = mmResendOtpUrl;
            PASS = pass;
            KEY = key;
            HOST = host;
            MMPAYMENTSTATUSURL = mmPaymentStatusUrl;
            MMREFUNDURL = mmRefundPaymentUrl;
        };
    }

}
