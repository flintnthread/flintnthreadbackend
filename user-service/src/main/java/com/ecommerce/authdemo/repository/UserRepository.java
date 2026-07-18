package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByContactNumber(String contactNumber);

    /** All accounts that share the same registered mobile (Switch Account). */
    List<User> findAllByContactNumber(String contactNumber);

    List<User> findAllByContactNumberIn(Collection<String> contactNumbers);

    Optional<User> findByUsername(String username);

    Optional<User> findByReferralCode(String referralCode);

    /**
     * Match slug-form codes {@code FNT????######} by their trailing 6 digits
     * (SQL {@code _} = any single character).
     */
    @Query("SELECT u FROM User u WHERE UPPER(u.referralCode) LIKE CONCAT('FNT____', UPPER(:sixDigits))")
    List<User> findByReferralCodeSlugEndingWith(@Param("sixDigits") String sixDigits);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

}
