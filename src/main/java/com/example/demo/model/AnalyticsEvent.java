package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "analytics_events")
public class AnalyticsEvent {

    @Id
    private String id;

    @Indexed
    private String eventType;

    private String userId;

    @Indexed
    private String storeId;

    @Indexed
    private String offerId;

    private String flyerId;
    private Map<String, Object> metadata = new HashMap<>();
    private LocalDateTime createdAt;

    public AnalyticsEvent() {}

    public AnalyticsEvent(String eventType, String userId, String storeId, String offerId, Map<String, Object> metadata) {
        this.eventType = eventType;
        this.userId = userId;
        this.storeId = storeId;
        this.offerId = offerId;
        this.metadata = metadata != null ? metadata : new HashMap<>();
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public String getOfferId() { return offerId; }
    public void setOfferId(String offerId) { this.offerId = offerId; }

    public String getFlyerId() { return flyerId; }
    public void setFlyerId(String flyerId) { this.flyerId = flyerId; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
