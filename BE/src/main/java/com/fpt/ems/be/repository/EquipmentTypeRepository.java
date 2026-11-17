package com.fpt.ems.be.repository;

import com.fpt.ems.be.model.EquipmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EquipmentTypeRepository extends JpaRepository<EquipmentType, Long> {
    Optional<EquipmentType> findByName(String name);
    boolean existsByName(String name);
    boolean existsByNameAndTypeIdNot(String name, Long typeId);
}
