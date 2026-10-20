package com.setec.hms.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.*;
import org.springframework.web.filter.*;

@Component
public class JwtFilter extends OncePerRequestFilter {
  final JwtService jwt;
  final UserDetailsService users;

  public JwtFilter(JwtService j, UserDetailsService u) {
    jwt = j;
    users = u;
  }

  protected void doFilterInternal(HttpServletRequest q, HttpServletResponse p, FilterChain c)
      throws ServletException, IOException {
    String h = q.getHeader("Authorization");
    if (h != null && h.startsWith("Bearer ")) {
      String t = h.substring(7);
      try {
        UserDetails u = users.loadUserByUsername(jwt.username(t));
        if (jwt.valid(t, u))
          SecurityContextHolder.getContext()
              .setAuthentication(
                  new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));
      } catch (Exception ignored) {
      }
    }
    c.doFilter(q, p);
  }
}
