package com.linepulse.maintenance;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MaintenancePlanScheduler {
    private final MaintenancePlanService maintenancePlanService;

    public MaintenancePlanScheduler(MaintenancePlanService maintenancePlanService) {
        this.maintenancePlanService = maintenancePlanService;
    }

    @Scheduled(initialDelay = 15000, fixedDelay = 3600000)
    public void generateDuePreventiveOrders() {
        maintenancePlanService.generateDuePlans();
    }
}
