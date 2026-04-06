package com.kalsym.ekedai.scheduler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kalsym.ekedai.EkedaiApplication;
import com.kalsym.ekedai.utility.Logger;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Profile("!dev")
public class WspTokenScheduler {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY = 30 * 1000; // 30 seconds

    @Value("${wsp.getTokenUrl}")
    private String wspGetTokenUrl;

    @Value("${wsp.username}")
    private String wspUsername;

    @Value("${wsp.password}")
    private String wspPassword;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {
        fetchTokenWithRetry();
    }
    
    @Scheduled(fixedDelay = 60 * 1000)
    public void checkAndRefreshToken() {

        if (EkedaiApplication.WSPTOKENTIMEOUT == null) {
            return;
        }

        long bufferInSeconds = 5 * 60;
        long tokenTimeoutInSeconds =
                Long.parseLong(EkedaiApplication.WSPTOKENTIMEOUT) - bufferInSeconds;

        if (tokenTimeoutInSeconds <= 0) {
            fetchTokenWithRetry();
        }
    }

    private void fetchTokenWithRetry() {

        int retries = 0;

        while (retries < MAX_RETRIES) {
            try {
                fetchToken();
                return;
            } catch (Exception e) {

                retries++;

                Logger.application.error(
                        Logger.pattern,
                        EkedaiApplication.VERSION,
                        "getTokenWithRetry",
                        "Error fetching token. Attempt " + retries,
                        e
                );

                if (retries < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY);
                    } catch (InterruptedException ignored) {}
                }
            }
        }

        Logger.application.error(
                Logger.pattern,
                EkedaiApplication.VERSION,
                "getTokenWithRetry",
                "Failed to fetch token after max attempts"
        );
    }

    private void fetchToken() {

        JsonObject auth = new JsonObject();
        auth.addProperty("username", wspUsername);
        auth.addProperty("password", wspPassword);

        Logger.application.info(
                Logger.pattern,
                EkedaiApplication.VERSION,
                "GetToken",
                "Request :: ",
                auth
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity =
                new HttpEntity<>(auth.toString(), headers);

        ResponseEntity<String> response =
                restTemplate.exchange(
                        wspGetTokenUrl,
                        HttpMethod.POST,
                        requestEntity,
                        String.class
                );

        Logger.application.info(
                Logger.pattern,
                EkedaiApplication.VERSION,
                "GetToken",
                "Response :: ",
                response.getBody()
        );

        if (response.getStatusCodeValue() == 200) {

            JsonObject body =
                    new Gson().fromJson(response.getBody(), JsonObject.class);

            String token = body.get("token").getAsString();
            String tokenTimeOut =
                    body.get("tokenValidityInSecond").getAsString();

            EkedaiApplication.WSPTOKEN = token;
            EkedaiApplication.WSPTOKENTIMEOUT = tokenTimeOut;
        }
    }
}
