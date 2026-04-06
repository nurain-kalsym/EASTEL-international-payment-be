package com.kalsym.ekedai.utility;

import java.util.Date;

import jakarta.servlet.http.HttpServletRequest;

import com.kalsym.ekedai.model.dao.CentralAuthTokenDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.kalsym.ekedai.model.MySQLUserDetails;
import com.kalsym.ekedai.model.User;

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

    if (bearerHeaderValue != null) {
      return bearerHeaderValue;
    } else {
      return null;
    }

  }

  public String getJwtToken(MySQLUserDetails userPrincipal) {

    String jwt = generateTokenFromUsername(userPrincipal.getUsername(), userPrincipal.getId());
    return jwt;

  }

  public String getJwtToken(User user) {

    String jwt = generateTokenFromUsername(user.getEmail(), user.getId());
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

  public String generateTokenFromUsername(String username, String jti) {
    return Jwts.builder()
        .setSubject(username)
        .setId(jti)
        .setIssuedAt(new Date())
        .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
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
