package com.setec.hms.security;

import com.setec.hms.entity.User;
import com.setec.hms.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public PasswordEncoder encoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public UserDetailsService uds(UserRepository repository) {
    return usernameOrEmail -> {
      User user =
          repository
              .findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
              .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

      return org.springframework.security.core.userdetails.User.withUsername(user.username)
          .password(user.passwordHash)
          .authorities(new SimpleGrantedAuthority("ROLE_" + user.role.name()))
          .disabled(!user.enabled)
          .build();
    };
  }

  @Bean
  public SecurityFilterChain chain(HttpSecurity http, JwtFilter jwtFilter) throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // Missing or expired tokens get 401 so the frontend knows to sign the user out.
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(
                    (request, response, exception) -> {
                      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                      response.setContentType("application/json");
                      response.getWriter()
                          .write(
                              "{\"status\":401,\"error\":\"Unauthorized\","
                                  + "\"message\":\"Your session has ended. Please sign in again.\"}");
                    }))
        // Signed-in users who lack the role get 403, not 401: a 401 would make the frontend sign them out.
        .exceptionHandling(
            ex ->
                ex.accessDeniedHandler(
                    (request, response, exception) -> {
                      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                      response.setContentType("application/json");
                      response.getWriter()
                          .write(
                              "{\"status\":403,\"error\":\"Forbidden\","
                                  + "\"message\":\"You do not have permission to perform this action.\"}");
                    }))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/initial-admin")
                    .permitAll()
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                    .permitAll()
                    // Any signed-in user may remove their own profile photo.
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/auth/me/avatar")
                    .authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/v1/dashboard/**")
                    .hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST")
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  /** JwtFilter is a @Component and is already placed in the security chain; stop Boot running it twice. */
  @Bean
  public FilterRegistrationBean<JwtFilter> jwtFilterRegistration(JwtFilter filter) {
    FilterRegistrationBean<JwtFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  public AuthenticationManager manager(AuthenticationConfiguration configuration) throws Exception {
    return configuration.getAuthenticationManager();
  }
}
