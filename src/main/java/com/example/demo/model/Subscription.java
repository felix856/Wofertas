package com.example.demo.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "subscriptions")
public class Subscription {

    @Id
    private String id;

    @Indexed
    private String mercadoId;

    private String userId;
    private String planId;
    private PlanName planName;
    private SubscriptionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private boolean autoRenew;
    private String provider;
    private String externalSubscriptionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Subscription() {}

    public Subscription(String mercadoId, String planId, PlanName planName, SubscriptionStatus status) {
        this.mercadoId = mercadoId;
        this.userId = mercadoId;
        this.planId = planId;
        this.planName = planName;
        this.status = status;
        this.startedAt = LocalDateTime.now();
        this.autoRenew = false;
        this.provider = "manual";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMercadoId() { return mercadoId; }
    public void setMercadoId(String mercadoId) { this.mercadoId = mercadoId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public PlanName getPlanName() { return planName; }
    public void setPlanName(PlanName planName) { this.planName = planName; }

    public SubscriptionStatus getStatus() { return status; }
    public void setStatus(SubscriptionStatus status) { this.status = status; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public boolean isAutoRenew() { return autoRenew; }
    public void setAutoRenew(boolean autoRenew) { this.autoRenew = autoRenew; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getExternalSubscriptionId() { return externalSubscriptionId; }
    public void setExternalSubscriptionId(String externalSubscriptionId) { this.externalSubscriptionId = externalSubscriptionId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
