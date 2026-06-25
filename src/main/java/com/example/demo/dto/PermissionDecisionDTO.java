package com.example.demo.dto;

public record PermissionDecisionDTO(
    String permission,
    boolean allowed,
    boolean wouldAllowByPlan,
    boolean enforcementEnabled,
    int currentUsage,
    int limit,
    String planName,
    String reason
) {}
