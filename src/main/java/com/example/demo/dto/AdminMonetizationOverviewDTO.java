package com.example.demo.dto;

public record AdminMonetizationOverviewDTO(
    long totalMercados,
    long totalPlans,
    long totalSubscriptions,
    long totalAnalyticsEvents,
    long trialSubscriptions,
    long activeSubscriptions,
    long expiredSubscriptions,
    long cancelledSubscriptions
) {}
