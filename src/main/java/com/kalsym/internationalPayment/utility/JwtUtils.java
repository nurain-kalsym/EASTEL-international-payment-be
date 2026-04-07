package com.kalsym.internationalPayment.utility;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.kalsym.internationalPayment.model.MySQLUserDetails;
import com.kalsym.internationalPayment.model.User;
import com.kalsym.internationalPayment.model.dao.CentralAuthTokenDetails;

import io.jsonwebtoken.*;

@Component
public class JwtUtils {
  private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

  @Value("${app.jwtSecret}")
  private String jwtSecret;

  @Value("${app.jwtExpirationMs}")
  private int jwtExpirationMs;

  @Value("${app.jwtCookieName}")
  private String jwtCookie;

  @Value("${server.servlet.context-path}")
  private String contextPath;

  @Value(("${central.auth.service.secretString}"))
  private String centralAuthServiceSecret;

  public String getHeaderBearer(HttpServletRequest request) {
    String bearerHeaderValue = request.getHeader("Authorization");

    if (bearerHeaderValue != null && bearerHeaderValue.startsWith("Bearer ")) {
        // remove "Bearer " prefix
        return bearerHeaderValue.substring(7);
    }

    return null; // no token found

  }

  public String getJwtToken(MySQLUserDetails userPrincipal) {

    String jwt = generateTokenFromUsername(userPrincipal.getUsername(), userPrincipal.getId(), userPrincipal.getRole());
    return jwt;

  }

  public String getJwtToken(User user) {

    String jwt = generateTokenFromUsername(user.getEmail(), user.getId(), new Date());
    return jwt;

  }

  public String getJwtTokenFromPhoneNumber(String phoneNumber, String id) {

    return Jwts.builder()
            .setSubject(phoneNumber)
            .setId(id)
            .setIssuedAt(new Date())
            .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();

  }

  public String getRoleFromToken(String token) {
      Claims claims = Jwts.parser()
              .setSigningKey(jwtSecret)
              .parseClaimsJws(token)
              .getBody();

      return claims.get("role", String.class);
  }

  public Boolean isAdminRole(String token) {
    if("ADMIN".equalsIgnoreCase(getRoleFromToken(token))) {
      return true;
    } else {
      return false;
    }
  }

  public String refreshToken(String authToken) {
      return generateTokenFromUsername(getUserNameFromJwtToken(authToken), getUserId(authToken),
        getIssueDate(authToken));
  }

  public String getUserNameFromJwtToken(String token) {
    return Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody().getSubject();
  }

  public CentralAuthTokenDetails parseAppToken(String token) {
    byte[] signingKeyBytes = centralAuthServiceSecret.getBytes();

    try {
      Claims claims = Jwts.parser()
              .setSigningKey(signingKeyBytes)
              .parseClaimsJws(token)
              .getBody();

      String appId = claims.get("appId", String.class);
      String phoneNumber = claims.get("phoneNumber", String.class);

      return new CentralAuthTokenDetails(appId, phoneNumber);
    } catch (SignatureException | MalformedJwtException | ExpiredJwtException | UnsupportedJwtException | IllegalArgumentException e) {
      logger.error("JWT parsing error: {}", e.getMessage(), e);
    }
      return new CentralAuthTokenDetails();
  }



  public String getUserId(String token) {
    return Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody().getId();
  }

  public Date getIssueDate(String token) {
    return Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody().getIssuedAt();

  }

  public Date getExpiryDate(String token) {
    return Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody().getExpiration();

  }

  public boolean validateJwtToken(String authToken) {
    try {
      Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
      return true;
    } catch (SignatureException e) {
      logger.error("Invalid JWT signature: {}", e.getMessage());
    } catch (MalformedJwtException e) {
      logger.error("Invalid JWT token: {}", e.getMessage());
    } catch (ExpiredJwtException e) {
      logger.error("JWT token is expired: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
      logger.error("JWT token is unsupported: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      logger.error("JWT claims string is empty: {}", e.getMessage());
    }

    return false;
  }

  public String generateTokenFromUsername(String username, String id, String role) {
      Map<String, Object> claims = new HashMap<>();
      claims.put("id", id);          // optional, already storing userId
      claims.put("role", role);      // add user role here

      return Jwts.builder()
              .setClaims(claims)
              .setSubject(username)
              .setIssuedAt(new Date())
              .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
              .signWith(SignatureAlgorithm.HS512, jwtSecret)
              .compact();
  }

  public String generateTokenFromUsername(String username, String jti, Date issueAt) {
    return Jwts.builder()
        .setSubject(username)
        .setId(jti)
        .setIssuedAt(issueAt)
        .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
        .signWith(SignatureAlgorithm.HS512, jwtSecret)
        .compact();
  }

}
