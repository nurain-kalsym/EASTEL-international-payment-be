package com.kalsym.internationalPayment.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MySQLUserDetails implements UserDetails {

  private String id;

  private String username;

  private String phoneNumber;

  private String email;

  private Boolean isEnable;

  private String role;

  @JsonIgnore
  private String password;

  private Collection<? extends GrantedAuthority> authorities;

  public MySQLUserDetails(String id, String email, String password, Boolean isEnable, String role,
      Collection<? extends GrantedAuthority> authorities) {
    this.id = id;
    this.username = email;
    this.email = email;
    this.password = password;
    this.isEnable = isEnable;
    this.role = role;
    this.authorities = authorities;
  }

  public static MySQLUserDetails build(User user) {

    List<GrantedAuthority> grantedAuthorities = new ArrayList<GrantedAuthority>();
    grantedAuthorities.add(new SimpleGrantedAuthority(user.getRole()));

    return new MySQLUserDetails(
        user.getId(),
        user.getEmail(),
        user.getPassword(),
        user.getIsEnable(),
        user.getRole(),
        grantedAuthorities);
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  public String getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return username;
  }

  public String getRole() {
    return role;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return isEnable;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    MySQLUserDetails user = (MySQLUserDetails) o;
    return Objects.equals(id, user.id);
  }
}