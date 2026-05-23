package com.pricehawk.user.repository;

import com.pricehawk.data.repository.BaseRepository;
import com.pricehawk.user.domain.entity.AuthProvider;
import com.pricehawk.user.domain.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends BaseRepository<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByProviderIdAndAuthProvider(String providerId, AuthProvider authProvider);

    /**
     * Load user + roles trong một JOIN FETCH query theo id.
     * Dùng ở refresh token, update profile, và các nơi có sẵn userId.
     */
    @EntityGraph(attributePaths = "roles")
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithRoles(@Param("id") UUID id);

    /**
     * Load user + roles trong một JOIN FETCH query theo email.
     * Dùng ở login để tránh N+1 (email → id → roles = 2 queries).
     */
    @EntityGraph(attributePaths = "roles")
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    /**
     * Pessimistic write lock — dùng trong recordSearch để serialize concurrent
     * counter updates cho cùng 1 user. Ngăn race condition khi 2 request đồng thời
     * cùng pass rate limit check rồi cùng increment.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") UUID id);
}
