package com.kalsym.ekedai.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kalsym.ekedai.model.dao.ProfileServiceResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ProfileService {

    @Value("${profile.service.url:https://uatprofile.e-kedai.my}")
    String profileServiceUrl;

    @Value("${profile.service.token:eyJhbGciOiJIUzI1NiJ9.e30.SWH-3nREFY6jPlzU8Pc0ClTVtPWKhorbaTDhVHx_j7M}")
    String profileServiceToken;

    @Value("${channel.name}")
    String channelName;

    public ProfileServiceResponse createUser(String phone, String name, String email, String referral, String nationality, boolean isDocUploaded, String dob) {

//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
//        Date currentDate = new Date();
//        String currentTimeStamp = sdf.format(currentDate);

        JsonObject jsonBuilder = new JsonObject();
        jsonBuilder.addProperty("name", name);
        jsonBuilder.addProperty("email", email);
        jsonBuilder.addProperty("phone", phone);
        jsonBuilder.addProperty("referral", referral);
        jsonBuilder.addProperty("country", nationality);
        jsonBuilder.addProperty("idDoc", isDocUploaded);

        JsonObject demographic = new JsonObject();
        demographic.addProperty("nationality", nationality);
        demographic.addProperty("dob", dob);

        jsonBuilder.add("demographic", demographic);

        JsonObject channel = new JsonObject();
        channel.addProperty("channelName", channelName);
//        channel.addProperty("registrationDate", currentTimeStamp);
        channel.addProperty("status", "active");

        jsonBuilder.add("channel", channel);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + profileServiceToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(jsonBuilder.toString(), headers);

        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<ProfileServiceResponse> responseEntity = restTemplate.exchange(
                    profileServiceUrl + "/customer-profile/sign-up", HttpMethod.POST, requestEntity,
                    ProfileServiceResponse.class);
            return responseEntity.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            Gson gson = new Gson();
            return gson.fromJson(responseBody, ProfileServiceResponse.class);
        }

    }

    public ProfileServiceResponse updateUser(String phoneNumber, String name, String email, String nationality, String gender, String dob) {


        JsonObject jsonBuilder = new JsonObject();
        jsonBuilder.addProperty("name", name);
        jsonBuilder.addProperty("email", email);

        JsonObject demographic = new JsonObject();
        demographic.addProperty("gender", gender);
        demographic.addProperty("nationality", nationality);
        demographic.addProperty("dob", dob);

        jsonBuilder.add("demographic", demographic);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + profileServiceToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(jsonBuilder.toString(), headers);

        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<ProfileServiceResponse> responseEntity = restTemplate.exchange(
                    profileServiceUrl + "/customer-profile/customer-details/update/" + phoneNumber, HttpMethod.PUT, requestEntity,
                    ProfileServiceResponse.class);
            return responseEntity.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            Gson gson = new Gson();
            return gson.fromJson(responseBody, ProfileServiceResponse.class);
        }

    }

    public ProfileServiceResponse updateDocumentUploadStatus(String phoneNumber, String uploadDocStatus) {

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + profileServiceToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        String url = UriComponentsBuilder.fromHttpUrl(profileServiceUrl + "/customer-profile/customer-details/update-doc/" + phoneNumber)
                .queryParam("idDoc", uploadDocStatus)
                .toUriString();

        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<ProfileServiceResponse> responseEntity = restTemplate.exchange(url, HttpMethod.PUT, requestEntity,
                    ProfileServiceResponse.class);
            return responseEntity.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            Gson gson = new Gson();
            return gson.fromJson(responseBody, ProfileServiceResponse.class);
        }

    }

    public ProfileServiceResponse mergeProfile(String oldPhoneNumber, String newPhoneNumber) {

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + profileServiceToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        String url = UriComponentsBuilder.fromHttpUrl(profileServiceUrl + "/customer-profile/customer-details/update-merge/" + oldPhoneNumber)
                .queryParam("newPhone", newPhoneNumber)
                .toUriString();

        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<ProfileServiceResponse> responseEntity = restTemplate.exchange(url, HttpMethod.PUT, requestEntity,
                    ProfileServiceResponse.class);
            return responseEntity.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            Gson gson = new Gson();
            return gson.fromJson(responseBody, ProfileServiceResponse.class);
        }

    }

    public ProfileServiceResponse postUserDeviceToken(String phone, String deviceToken) {

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + profileServiceToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        String url = UriComponentsBuilder.fromHttpUrl(profileServiceUrl + "/customer-profile/customer-details/device-token/" + phone)
                .queryParam("deviceToken", deviceToken)
                .toUriString();

        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<ProfileServiceResponse> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity,
                    ProfileServiceResponse.class);
            return responseEntity.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            Gson gson = new Gson();
            return gson.fromJson(responseBody, ProfileServiceResponse.class);
        }

    }
}
