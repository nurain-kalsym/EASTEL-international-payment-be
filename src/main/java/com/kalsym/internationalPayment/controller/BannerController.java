package com.kalsym.internationalPayment.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kalsym.internationalPayment.model.Banner;
import com.kalsym.internationalPayment.repositories.BannerRepository;
import com.kalsym.internationalPayment.services.BannerService;
import com.kalsym.internationalPayment.utility.HttpResponse;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("banner")
public class BannerController {

    @Autowired
    BannerService bannerService;

    @Autowired
    BannerRepository bannerRepository;

    @GetMapping("")
    public ResponseEntity<HttpResponse> getAllBanners(HttpServletRequest request) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        try {
            List<Banner> banner = bannerRepository.findAll();
            response.setStatus(HttpStatus.OK);
            response.setData(banner);
        } catch (Exception e) {
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setMessage("Error creating banner: " + e.getMessage());
        }
        return ResponseEntity.status(response.getStatus()).body(response);

    }

    @GetMapping("/get-by-section/{section}")
    public ResponseEntity<HttpResponse> getBannersBySection(HttpServletRequest request, @PathVariable String section)  {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        try {
            List<Banner> banner = bannerRepository.findBySection(section.toLowerCase());

            if (banner.isEmpty()) {
                response.setStatus(HttpStatus.OK);
                response.setData(Collections.emptyList());
            } else {
                response.setStatus(HttpStatus.OK);
                response.setData(banner);
            }
        } catch (Exception e) {
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setMessage("Error creating banner: " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);

    }

}
