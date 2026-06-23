package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "data_privacy_requests")
public class DataPrivacyRequest {

    @Id
    private String id;

    @Indexed
    private String requesterId;

    private String requesterType;

    @Indexed
    private String email;

    private String requestType;
    private String status;
    private String source;
    private String notes;
    private Map<String, Object> metadata = new HashMap<>();
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;

    public DataPrivacyRequest() {}

    public DataPrivacyRequest(String requesterType, String requesterId, String email, String requestType, String source) {
        this.requesterType = requesterType;
        this.requesterId = requesterId;
        this.email = email;
        this.requestType = requestType;
        this.source = source;
        this.status = "PENDING";
        this.requestedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequesterId() { return requesterId; }
    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }

    public String getRequesterType() { return requesterType; }
    public void setRequesterType(String requesterType) { this.requesterType = requesterType; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
