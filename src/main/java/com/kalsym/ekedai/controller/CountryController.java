package com.kalsym.ekedai.controller;

import com.kalsym.ekedai.EkedaiApplication;
import com.kalsym.ekedai.model.Country;
import com.kalsym.ekedai.repositories.CountryRepository;
import com.kalsym.ekedai.utility.HttpResponse;
import com.kalsym.ekedai.utility.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/countries")
public class CountryController {

    @Autowired
    private CountryRepository countryRepository;

    // GET all countries and filter by countryName
    @GetMapping("")
    public ResponseEntity<HttpResponse> getAllCountries(HttpServletRequest request) {
        HttpResponse response = new HttpResponse(request.getRequestURI());

        String logprefix = "getAllCountries";
        
        try {
            List<Country> countryList = countryRepository.findAll(Sort.by("countryName"));
            response.setStatus(HttpStatus.OK);
            response.setData(countryList);
            
        } catch (Exception e) {
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setMessage("Failed to fetch countries: " + e.getMessage());
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/{countryCode}")
    public ResponseEntity<HttpResponse> getCountryByCountryCode(
            HttpServletRequest request,
            @PathVariable("countryCode") String countryCode) {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "getCountryByCountryCode";
        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                "Country code: " + countryCode);
        try {
            Country country = countryRepository.findById(countryCode).orElse(null);
            if (country != null) {
                response.setStatus(HttpStatus.OK);
                response.setData(country);
            } else {
                response.setStatus(HttpStatus.NOT_FOUND);
                response.setMessage("Country not found");
                Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                        "Country not found");
            }
        } catch (Exception e) {
            response.setStatus(HttpStatus.EXPECTATION_FAILED);
            response.setMessage("Failed to fetch country");
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

}
