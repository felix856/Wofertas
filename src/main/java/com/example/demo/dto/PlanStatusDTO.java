package com.example.demo.dto;

import java.util.List;

import com.example.demo.model.Plan;
import com.example.demo.model.Subscription;

public record PlanStatusDTO(
    String mercadoId,
    Plan plan,
    Subscription subscription,
    UsageSnapshotDTO usage,
    List<PermissionDecisionDTO> permissions,
    String mode
) {}
