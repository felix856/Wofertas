package com.example.demo.dto;

public record PrivacyDeletionRequestDTO(
        String email,
        String requesterType,
        String reason,
        String source
) {}
