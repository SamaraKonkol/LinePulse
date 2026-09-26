package com.linepulse.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByRegistrationIgnoreCase(String registration);
    boolean existsByRegistrationIgnoreCase(String registration);
}
