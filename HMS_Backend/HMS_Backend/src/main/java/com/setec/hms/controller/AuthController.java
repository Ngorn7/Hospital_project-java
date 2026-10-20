package com.setec.hms.controller;

import com.setec.hms.entity.Role;
import com.setec.hms.entity.User;
import com.setec.hms.exception.ConflictException;
import com.setec.hms.repository.UserRepository;
import com.setec.hms.security.JwtService;
import com.setec.hms.exception.NotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.util.Iterator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestPart;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;

  @Value("${app.initial-admin.username:}")
  private String initialName;

  @Value("${app.initial-admin.email:}")
  private String initialEmail;

  @Value("${app.initial-admin.password:}")
  private String initialPass;

  public AuthController(
      AuthenticationManager authenticationManager,
      JwtService jwtService,
      UserRepository users,
      PasswordEncoder passwordEncoder) {
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.users = users;
    this.passwordEncoder = passwordEncoder;
  }

  public record Login(@NotBlank String usernameOrEmail, @NotBlank String password) {}

  public record Register(
      @NotBlank String username,
      @NotBlank @Email String email,
      @NotBlank @Size(min = 8) String password,
      Role role) {}

  public record Token(String accessToken) {}

  public record Profile(Long id, String username, String email, Role role, boolean hasAvatar) {}

  public record ProfileUpdate(
      @NotBlank @Size(max = 80) String username,
      @NotBlank @Email @Size(max = 255) String email) {}

  public record ProfileUpdateResponse(Profile profile, String accessToken) {}

  // Login
  @PostMapping("/login")
  public Token login(@Valid @RequestBody Login request) {

    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.usernameOrEmail(), request.password()));

    User user =
        users
            .findByUsernameOrEmail(request.usernameOrEmail(), request.usernameOrEmail())
            .orElseThrow();

    UserDetails userDetails =
        org.springframework.security.core.userdetails.User.withUsername(user.username)
            .password(user.passwordHash)
            .authorities("ROLE_" + user.role.name())
            .build();

    return new Token(jwtService.token(userDetails));
  }

  @GetMapping("/me")
  @PreAuthorize("isAuthenticated()")
  public Profile profile(@AuthenticationPrincipal UserDetails principal) {
    return toProfile(currentUser(principal));
  }

  @PutMapping("/me")
  @PreAuthorize("isAuthenticated()")
  @Transactional
  public ProfileUpdateResponse updateProfile(
      @AuthenticationPrincipal UserDetails principal, @Valid @RequestBody ProfileUpdate request) {
    User user = currentUser(principal);
    String username = request.username().trim();
    String email = request.email().trim();
    users
        .findByUsername(username)
        .filter(other -> !other.id.equals(user.id))
        .ifPresent(other -> {
          throw new ConflictException("Username is already in use");
        });
    users
        .findByEmail(email)
        .filter(other -> !other.id.equals(user.id))
        .ifPresent(other -> {
          throw new ConflictException("Email is already in use");
        });

    user.username = username;
    user.email = email;
    User updated = users.save(user);
    UserDetails userDetails =
        org.springframework.security.core.userdetails.User.withUsername(updated.username)
            .password(updated.passwordHash)
            .authorities("ROLE_" + updated.role.name())
            .disabled(!updated.enabled)
            .build();
    return new ProfileUpdateResponse(toProfile(updated), jwtService.token(userDetails));
  }

  @PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("isAuthenticated()")
  @Transactional
  public Profile updateAvatar(
      @AuthenticationPrincipal UserDetails principal,
      @RequestPart("file") MultipartFile file) {
    if (file.isEmpty()) throw new IllegalArgumentException("Choose an image to upload");
    if (file.getSize() > 2 * 1024 * 1024) {
      throw new IllegalArgumentException("Profile images must be 2 MB or smaller");
    }

    String contentType = file.getContentType();
    if (!MediaType.IMAGE_JPEG_VALUE.equals(contentType)
        && !MediaType.IMAGE_PNG_VALUE.equals(contentType)) {
      throw new IllegalArgumentException("Use a JPEG or PNG image");
    }

    try {
      byte[] image = file.getBytes();
      try (ImageInputStream imageStream =
          ImageIO.createImageInputStream(new ByteArrayInputStream(image))) {
        Iterator<ImageReader> readers = ImageIO.getImageReaders(imageStream);
        if (!readers.hasNext()) {
          throw new IllegalArgumentException("The file is not a supported JPEG or PNG image");
        }
        ImageReader reader = readers.next();
        try {
          reader.setInput(imageStream, true, true);
          String format = reader.getFormatName();
          boolean jpeg = format.equalsIgnoreCase("jpeg") || format.equalsIgnoreCase("jpg");
          boolean png = format.equalsIgnoreCase("png");
          if ((!jpeg && !png)
              || (jpeg && !MediaType.IMAGE_JPEG_VALUE.equals(contentType))
              || (png && !MediaType.IMAGE_PNG_VALUE.equals(contentType))) {
            throw new IllegalArgumentException("The image content does not match its file type");
          }
          int width = reader.getWidth(0);
          int height = reader.getHeight(0);
          if (width < 1 || height < 1 || width > 3000 || height > 3000) {
            throw new IllegalArgumentException("Profile images must be no larger than 3000x3000 pixels");
          }
          BufferedImage decoded = reader.read(0);
          if (decoded == null) {
            throw new IllegalArgumentException("The uploaded image could not be decoded");
          }
        } finally {
          reader.dispose();
        }
      }
      User user = currentUser(principal);
      user.avatarImage = image;
      user.avatarContentType = contentType;
      return toProfile(users.save(user));
    } catch (IOException exception) {
      throw new IllegalArgumentException("Could not read the uploaded image", exception);
    }
  }

  @GetMapping("/me/avatar")
  @PreAuthorize("isAuthenticated()")
  @Transactional(readOnly = true)
  public ResponseEntity<byte[]> avatar(@AuthenticationPrincipal UserDetails principal) {
    User user = currentUser(principal);
    if (user.avatarImage == null || user.avatarContentType == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .header(HttpHeaders.CONTENT_TYPE, user.avatarContentType)
        .body(user.avatarImage);
  }

  @DeleteMapping("/me/avatar")
  @PreAuthorize("isAuthenticated()")
  @Transactional
  public Profile deleteAvatar(@AuthenticationPrincipal UserDetails principal) {
    User user = currentUser(principal);
    user.avatarImage = null;
    user.avatarContentType = null;
    return toProfile(users.save(user));
  }

  private User currentUser(UserDetails principal) {
    return users
        .findByUsername(principal.getUsername())
        .orElseThrow(() -> new NotFoundException("User not found"));
  }

  private Profile toProfile(User user) {
    return new Profile(user.id, user.username, user.email, user.role, user.avatarImage != null);
  }

  // Register a user (requires ADMIN role)
  @PostMapping("/register")
  @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> register(@Valid @RequestBody Register request) {

    if (request.role() == null) {
      throw new IllegalArgumentException("Role is required");
    }

    if (request.role() == Role.ADMIN) {
      throw new IllegalArgumentException("ADMIN accounts require initial setup");
    }
    if (users.findByUsername(request.username()).isPresent()) {
      throw new ConflictException("Username is already in use");
    }
    if (users.findByEmail(request.email()).isPresent()) {
      throw new ConflictException("Email is already in use");
    }

    User user = new User();
    user.username = request.username();
    user.email = request.email();
    user.passwordHash = passwordEncoder.encode(request.password());
    user.role = request.role();

    users.save(user);

    return ResponseEntity.status(201).build();
  }

  // Create the first administrator
  @PostMapping("/initial-admin")
  public ResponseEntity<Void> initialAdmin() {

    if (users.count() > 0
        || initialName == null
        || initialName.isBlank()
        || initialPass == null
        || initialPass.isBlank()) {

      return ResponseEntity.status(409).build();
    }

    User user = new User();
    user.username = initialName;
    user.email = initialEmail;
    user.passwordHash = passwordEncoder.encode(initialPass);
    user.role = Role.ADMIN;

    users.save(user);

    return ResponseEntity.status(201).build();
  }
}
