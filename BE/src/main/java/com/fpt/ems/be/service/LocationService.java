package com.fpt.ems.be.service;

import com.fpt.ems.be.dto.master.LocationRequest;
import com.fpt.ems.be.dto.master.LocationResponse;
import com.fpt.ems.be.exception.BusinessException;
import com.fpt.ems.be.model.Location;
import com.fpt.ems.be.model.enums.AuditOutcome;
import com.fpt.ems.be.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private final LocationRepository locationRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<LocationResponse> getAllLocations() {
        return locationRepository.findAll().stream()
                .map(LocationResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public LocationResponse getLocationById(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "LOCATION_NOT_FOUND", "Vị trí không tồn tại."));
        return LocationResponse.fromEntity(location);
    }

    @Transactional
    public LocationResponse createLocation(LocationRequest request) {
        Location location = Location.builder()
                .room(request.getRoom().trim())
                .area(request.getArea().trim())
                .department(request.getDepartment() != null ? request.getDepartment().trim() : null)
                .build();

        Location saved = locationRepository.save(location);

        auditService.record(
                "LOCATION_CREATE",
                "LOCATION",
                saved.getLocationId().toString(),
                AuditOutcome.SUCCESS,
                "OK",
                null,
                Map.of("room", saved.getRoom(), "area", saved.getArea())
        );

        return LocationResponse.fromEntity(saved);
    }

    @Transactional
    public LocationResponse updateLocation(Long id, LocationRequest request) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "LOCATION_NOT_FOUND", "Vị trí không tồn tại."));

        Map<String, Object> oldVal = Map.of("room", location.getRoom(), "area", location.getArea());

        location.setRoom(request.getRoom().trim());
        location.setArea(request.getArea().trim());
        if (request.getDepartment() != null) {
            location.setDepartment(request.getDepartment().trim());
        }

        Location updated = locationRepository.save(location);

        auditService.record(
                "LOCATION_UPDATE",
                "LOCATION",
                updated.getLocationId().toString(),
                AuditOutcome.SUCCESS,
                "OK",
                oldVal,
                Map.of("room", updated.getRoom(), "area", updated.getArea())
        );

        return LocationResponse.fromEntity(updated);
    }
}

