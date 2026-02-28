package com.interviewme.repository;

import com.interviewme.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByTenantId(Long tenantId);

    @Query(value = "SELECT * FROM users WHERE email LIKE :pattern", nativeQuery = true)
    List<User> findByEmailLike(@Param("pattern") String pattern);
}
