package com.setec.hms.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.setec.hms.entity.Role;
import com.setec.hms.entity.User;
import com.setec.hms.exception.ConflictException;
import com.setec.hms.repository.UserRepository;
import com.setec.hms.security.JwtService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthControllerTest {

  @Test
  void returnsSafeProfileForAuthenticatedUser() {
    UserRepository users = mock(UserRepository.class);
    User user = new User();
    user.id = 17L;
    user.username = "doctor1";
    user.email = "doctor1@example.com";
    user.passwordHash = "must-not-be-in-response";
    user.role = Role.DOCTOR;
    when(users.findByUsername("doctor1")).thenReturn(Optional.of(user));

    AuthController controller =
        new AuthController(null, mock(JwtService.class), users, mock(PasswordEncoder.class));
    UserDetails principal =
        org.springframework.security.core.userdetails.User.withUsername("doctor1")
            .password("ignored")
            .authorities("ROLE_DOCTOR")
            .build();

    AuthController.Profile profile = controller.profile(principal);

    assertEquals(17L, profile.id());
    assertEquals("doctor1", profile.username());
    assertEquals("doctor1@example.com", profile.email());
    assertEquals(Role.DOCTOR, profile.role());
    assertEquals(false, profile.hasAvatar());
  }

  @Test
  void updatingUsernameReturnsReplacementToken() {
    UserRepository users = mock(UserRepository.class);
    JwtService jwtService = mock(JwtService.class);
    User user = user(17L, "doctor1", "doctor1@example.com");
    when(users.findByUsername("doctor1")).thenReturn(Optional.of(user));
    when(users.findByUsername("newdoctor")).thenReturn(Optional.empty());
    when(users.findByEmail("newdoctor@example.com")).thenReturn(Optional.empty());
    when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(jwtService.token(any(UserDetails.class))).thenReturn("replacement-token");

    AuthController controller =
        new AuthController(null, jwtService, users, mock(PasswordEncoder.class));
    AuthController.ProfileUpdateResponse response =
        controller.updateProfile(
            principal("doctor1"),
            new AuthController.ProfileUpdate("newdoctor", "newdoctor@example.com"));

    assertEquals("newdoctor", response.profile().username());
    assertEquals("newdoctor@example.com", response.profile().email());
    assertEquals("replacement-token", response.accessToken());
  }

  @Test
  void rejectsUsernameAlreadyOwnedByAnotherUser() {
    UserRepository users = mock(UserRepository.class);
    User current = user(17L, "doctor1", "doctor1@example.com");
    User taken = user(22L, "taken", "taken@example.com");
    when(users.findByUsername("doctor1")).thenReturn(Optional.of(current));
    when(users.findByUsername("taken")).thenReturn(Optional.of(taken));

    AuthController controller =
        new AuthController(null, mock(JwtService.class), users, mock(PasswordEncoder.class));

    assertThrows(
        ConflictException.class,
        () ->
            controller.updateProfile(
                principal("doctor1"),
                new AuthController.ProfileUpdate("taken", "new@example.com")));
  }

  @Test
  void uploadsSupportedAvatarAndReturnsSafeProfile() throws IOException {
    UserRepository users = mock(UserRepository.class);
    User user = user(17L, "doctor1", "doctor1@example.com");
    when(users.findByUsername("doctor1")).thenReturn(Optional.of(user));
    when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    ByteArrayOutputStream imageBytes = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", imageBytes);
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "avatar.png", MediaType.IMAGE_PNG_VALUE, imageBytes.toByteArray());

    AuthController controller =
        new AuthController(null, mock(JwtService.class), users, mock(PasswordEncoder.class));
    AuthController.Profile result = controller.updateAvatar(principal("doctor1"), file);

    assertTrue(result.hasAvatar());
    assertEquals(MediaType.IMAGE_PNG_VALUE, user.avatarContentType);
    assertEquals(imageBytes.size(), user.avatarImage.length);
  }

  private User user(Long id, String username, String email) {
    User user = new User();
    user.id = id;
    user.username = username;
    user.email = email;
    user.passwordHash = "encoded-password";
    user.role = Role.DOCTOR;
    return user;
  }

  private UserDetails principal(String username) {
    return org.springframework.security.core.userdetails.User.withUsername(username)
        .password("ignored")
        .authorities("ROLE_DOCTOR")
        .build();
  }
}
