package com.example.demo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "plans")
public class Plan {

    @Id
    private String id;

    @Indexed(unique = true)
    private PlanName name;

    private BigDecimal price;
    private int weeklyFlyerLimit;
    private int monthlyOfferLimit;
    private int boostCredits;
    private int priorityLevel;
    private String analyticsLevel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Plan() {}

    public Plan(PlanName name, BigDecimal price, int weeklyFlyerLimit, int monthlyOfferLimit,
                int boostCredits, int priorityLevel, String analyticsLevel) {
        this.name = name;
        this.price = price;
        this.weeklyFlyerLimit = weeklyFlyerLimit;
        this.monthlyOfferLimit = monthlyOfferLimit;
        this.boostCredits = boostCredits;
        this.priorityLevel = priorityLevel;
        this.analyticsLevel = analyticsLevel;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public PlanName getName() { return name; }
    public void setName(PlanName name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public int getWeeklyFlyerLimit() { return weeklyFlyerLimit; }
    public void setWeeklyFlyerLimit(int weeklyFlyerLimit) { this.weeklyFlyerLimit = weeklyFlyerLimit; }

    public int getMonthlyOfferLimit() { return monthlyOfferLimit; }
    public void setMonthlyOfferLimit(int monthlyOfferLimit) { this.monthlyOfferLimit = monthlyOfferLimit; }

    public int getBoostCredits() { return boostCredits; }
    public void setBoostCredits(int boostCredits) { this.boostCredits = boostCredits; }

    public int getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(int priorityLevel) { this.priorityLevel = priorityLevel; }

    public String getAnalyticsLevel() { return analyticsLevel; }
    public void setAnalyticsLevel(String analyticsLevel) { this.analyticsLevel = analyticsLevel; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean hasUnlimitedFlyers() { return weeklyFlyerLimit < 0; }
    public boolean hasUnlimitedOffers() { return monthlyOfferLimit < 0; }
    public boolean hasUnlimitedBoosts() { return boostCredits < 0; }
}
