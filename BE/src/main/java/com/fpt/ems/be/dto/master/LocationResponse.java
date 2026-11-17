package com.fpt.ems.be.dto.master;

import com.fpt.ems.be.model.Location;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationResponse {
    private Long locationId;
    private String room;
    private String area;
    private String department;

    public static LocationResponse fromEntity(Location location) {
        if (location == null) return null;
        return LocationResponse.builder()
                .locationId(location.getLocationId())
                .room(location.getRoom())
                .area(location.getArea())
                .department(location.getDepartment())
                .build();
    }
}

