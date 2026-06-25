package com.example.demo.dto;

import java.util.Map;

public record AnalyticsEventRequest(
    String eventType,
    String userId,
    String storeId,
    String offerId,
    String flyerId,
    Map<String, Object> metadata
) {}
