package com.kalsym.internationalPayment.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.kalsym.internationalPayment.utility.HttpResponse;
import com.kalsym.internationalPayment.utility.JwtUtils;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AdminService {

    @Autowired
    JwtUtils jwtUtils;
    
    public ResponseEntity<HttpResponse> filterRole(HttpServletRequest request, HttpResponse response){
        String token = jwtUtils.getHeaderBearer(request);
        Boolean isAdmin = jwtUtils.isAdminRole(token);

        if (Boolean.FALSE.equals(isAdmin)) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setMessage("Resources blocked. User does not have ADMIN role.");
            return ResponseEntity.status(response.getStatus()).body(response);
        }
        
        return null;
    }
}
