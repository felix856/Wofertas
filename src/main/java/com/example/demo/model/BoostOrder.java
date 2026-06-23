package com.example.demo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "boost_orders")
public class BoostOrder {

    @Id
    private String id;

    private String storeId;
    private String offerId;
    private BigDecimal amount;
    private int durationHours;
    private String status;
    private LocalDateTime createdAt;

    public BoostOrder() {
        this.status = "draft";
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public String getOfferId() { return offerId; }
    public void setOfferId(String offerId) { this.offerId = offerId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public int getDurationHours() { return durationHours; }
    public void setDurationHours(int durationHours) { this.durationHours = durationHours; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
