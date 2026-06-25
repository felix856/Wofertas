package com.example.demo.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class MonetizationBootstrap implements ApplicationRunner {

    private final PlanService planService;

    public MonetizationBootstrap(PlanService planService) {
        this.planService = planService;
    }

    @Override
    public void run(ApplicationArguments args) {
        planService.ensureDefaultPlans();
    }
}
