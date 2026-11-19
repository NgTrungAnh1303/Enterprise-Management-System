package com.fpt.ems.be.repository;

import com.fpt.ems.be.model.AppUser;
import com.fpt.ems.be.model.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long>, JpaSpecificationExecutor<AppUser> {
    Optional<AppUser> findByUsername(String username);
    boolean existsByUsername(String username);

    Optional<AppUser> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT u FROM AppUser u WHERE u.role.name = 'TECHNICIAN' AND u.status = :status ORDER BY u.fullName ASC")
    List<AppUser> findActiveTechnicians(@Param("status") UserStatus status);
}
