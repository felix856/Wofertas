package com.example.demo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.LegalConsentRequest;
import com.example.demo.dto.PrivacyDeletionRequestDTO;
import com.example.demo.model.DataPrivacyRequest;
import com.example.demo.security.CustomUserDetails;
import com.example.demo.service.PrivacyService;

@RestController
@RequestMapping({"/privacy", "/api/privacy"})
@CrossOrigin(origins = "*")
public class PrivacyController {

    private final PrivacyService privacyService;

    public PrivacyController(PrivacyService privacyService) {
        this.privacyService = privacyService;
    }

    @GetMapping("/legal")
    public Map<String, Object> legalInfo() {
        return privacyService.legalInfo();
    }

    @GetMapping("/me/export")
    public Map<String, Object> exportMyData(@AuthenticationPrincipal CustomUserDetails principal) {
        return privacyService.exportMyData(principal);
    }

    @PostMapping("/me/consent")
    public Map<String, Object> acceptLegal(@AuthenticationPrincipal CustomUserDetails principal,
                                           @RequestBody LegalConsentRequest request) {
        return privacyService.acceptLegal(principal, request);
    }

    @PostMapping("/me/deletion-request")
    public ResponseEntity<DataPrivacyRequest> requestMyDeletion(@AuthenticationPrincipal CustomUserDetails principal,
                                                                @RequestBody PrivacyDeletionRequestDTO request) {
        return ResponseEntity.status(202).body(privacyService.requestMyDeletion(principal, request));
    }

    @GetMapping("/me/requests")
    public List<DataPrivacyRequest> myRequests(@AuthenticationPrincipal CustomUserDetails principal) {
        return privacyService.myRequests(principal);
    }

    @PostMapping("/public/deletion-request")
    public ResponseEntity<DataPrivacyRequest> requestPublicDeletion(@RequestBody PrivacyDeletionRequestDTO request) {
        return ResponseEntity.status(202).body(privacyService.requestPublicDeletion(request));
    }
}
