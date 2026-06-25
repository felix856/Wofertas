package com.example.demo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "usage_counters")
public class UsageCounter {

    @Id
    private String id;

    @Indexed
    private String mercadoId;

    private String periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private int flyersCreated;
    private int offersCreated;
    private int boostsUsed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UsageCounter() {}

    public UsageCounter(String mercadoId, String periodType, LocalDate periodStart, LocalDate periodEnd) {
        this.mercadoId = mercadoId;
        this.periodType = periodType;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMercadoId() { return mercadoId; }
    public void setMercadoId(String mercadoId) { this.mercadoId = mercadoId; }

    public String getPeriodType() { return periodType; }
    public void setPeriodType(String periodType) { this.periodType = periodType; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public int getFlyersCreated() { return flyersCreated; }
    public void setFlyersCreated(int flyersCreated) { this.flyersCreated = flyersCreated; }

    public int getOffersCreated() { return offersCreated; }
    public void setOffersCreated(int offersCreated) { this.offersCreated = offersCreated; }

    public int getBoostsUsed() { return boostsUsed; }
    public void setBoostsUsed(int boostsUsed) { this.boostsUsed = boostsUsed; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
