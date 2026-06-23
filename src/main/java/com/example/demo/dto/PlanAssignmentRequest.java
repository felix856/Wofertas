package com.example.demo.dto;

import java.time.LocalDateTime;

public record PlanAssignmentRequest(
    String planName,
    String status,
    Boolean autoRenew,
    LocalDateTime expiresAt
) {}
