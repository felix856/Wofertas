package com.example.demo.dto;

public record LegalConsentRequest(
        Boolean acceptPrivacyPolicy,
        Boolean acceptTerms,
        Boolean marketingConsent,
        Boolean analyticsConsent,
        String privacyPolicyVersion,
        String termsVersion
) {}
