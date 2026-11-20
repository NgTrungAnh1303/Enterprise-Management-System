package com.fpt.ems.be.repository;

import com.fpt.ems.be.model.StatusChangeRequest;
import com.fpt.ems.be.model.enums.StatusRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StatusChangeRequestRepository extends JpaRepository<StatusChangeRequest, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StatusChangeRequest s WHERE s.requestId = :id")
    Optional<StatusChangeRequest> findByIdWithLock(@Param("id") Long id);

    boolean existsByEquipment_EquipmentIdAndStatus(Long equipmentId, StatusRequestStatus status);

    List<StatusChangeRequest> findByEquipment_EquipmentIdAndStatus(Long equipmentId, StatusRequestStatus status);

    List<StatusChangeRequest> findByEquipment_EquipmentIdOrderByProposedAtDesc(Long equipmentId);

    List<StatusChangeRequest> findAllByOrderByProposedAtDesc();
}

