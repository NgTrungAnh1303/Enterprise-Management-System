package com.fpt.ems.be.config;

import com.fpt.ems.be.util.DueLabelCalculator;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DueSoonConfig {

    @Value("${app.maintenance.due-soon-days:7}")
    private int dueSoonDays;

    @PostConstruct
    public void init() {
        DueLabelCalculator.setDueSoonDays(dueSoonDays);
    }
}

