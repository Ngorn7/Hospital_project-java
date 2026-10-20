package com.setec.hms.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.*;
import java.nio.charset.*;
import java.util.*;
import javax.crypto.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.*;

@Service
public class JwtService {
  @Value("${app.jwt.secret}")
  String secret;

  @Value("${app.jwt.expiration-ms}")
  long expiration;

  private SecretKey key() {
    if (secret == null || secret.length() < 32)
      throw new IllegalStateException("JWT_SECRET must contain at least 32 characters");
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  public String token(UserDetails u) {
    return Jwts.builder()
        .subject(u.getUsername())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(key())
        .compact();
  }

  public String username(String t) {
    return Jwts.parser().verifyWith(key()).build().parseSignedClaims(t).getPayload().getSubject();
  }

  public boolean valid(String t, UserDetails u) {
    try {
      return username(t).equals(u.getUsername())
          && !Jwts.parser()
              .verifyWith(key())
              .build()
              .parseSignedClaims(t)
              .getPayload()
              .getExpiration()
              .before(new Date());
    } catch (JwtException | IllegalStateException e) {
      return false;
    }
  }
}
