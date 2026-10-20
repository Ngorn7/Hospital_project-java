package com.setec.hms.repository;

import com.setec.hms.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByUsernameOrEmail(String username, String email);

  Optional<User> findByUsername(String username);

  Optional<User> findByEmail(String email);
}
