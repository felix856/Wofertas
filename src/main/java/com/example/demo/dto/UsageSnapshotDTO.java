package com.example.demo.dto;

import java.time.LocalDate;

public record UsageSnapshotDTO(
    String mercadoId,
    LocalDate weekStart,
    LocalDate weekEnd,
    int flyersCreatedThisWeek,
    int boostsUsedThisWeek,
    LocalDate monthStart,
    LocalDate monthEnd,
    int offersCreatedThisMonth
) {}
