package com.example.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AdminMonetizationOverviewDTO;
import com.example.demo.dto.AnalyticsEventRequest;
import com.example.demo.dto.PermissionDecisionDTO;
import com.example.demo.dto.PlanAssignmentRequest;
import com.example.demo.dto.PlanStatusDTO;
import com.example.demo.dto.UsageSnapshotDTO;
import com.example.demo.model.AnalyticsEvent;
import com.example.demo.model.Plan;
import com.example.demo.model.Subscription;
import com.example.demo.model.SubscriptionStatus;
import com.example.demo.repository.AnalyticsEventRepository;
import com.example.demo.repository.MercadoRepository;
import com.example.demo.repository.SubscriptionRepository;
import com.example.demo.security.CustomUserDetails;
import com.example.demo.service.AnalyticsEventService;
import com.example.demo.service.PermissionService;
import com.example.demo.service.PlanService;
import com.example.demo.service.UsageService;

@RestController
@RequestMapping({"/monetization", "/api/monetization"})
@CrossOrigin(origins = "*")
public class MonetizationController {

    private final PlanService planService;
    private final PermissionService permissionService;
    private final UsageService usageService;
    private final AnalyticsEventService analyticsEventService;
    private final MercadoRepository mercadoRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AnalyticsEventRepository analyticsEventRepository;

    public MonetizationController(PlanService planService,
                                  PermissionService permissionService,
                                  UsageService usageService,
                                  AnalyticsEventService analyticsEventService,
                                  MercadoRepository mercadoRepository,
                                  SubscriptionRepository subscriptionRepository,
                                  AnalyticsEventRepository analyticsEventRepository) {
        this.planService = planService;
        this.permissionService = permissionService;
        this.usageService = usageService;
        this.analyticsEventService = analyticsEventService;
        this.mercadoRepository = mercadoRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.analyticsEventRepository = analyticsEventRepository;
    }

    @GetMapping("/plans")
    public List<Plan> listPlans() {
        return planService.listPlans();
    }

    @GetMapping("/mercados/{mercadoId}/status")
    public PlanStatusDTO getMercadoStatus(@PathVariable String mercadoId) {
        return buildStatus(mercadoId);
    }

    @PutMapping("/mercados/{mercadoId}/plan")
    public PlanStatusDTO assignPlan(@PathVariable String mercadoId,
                                    @RequestBody PlanAssignmentRequest request) {
        planService.assignPlan(mercadoId, request);
        return buildStatus(mercadoId);
    }

    @PostMapping("/mercados/{mercadoId}/plan")
    public PlanStatusDTO assignPlanPost(@PathVariable String mercadoId,
                                        @RequestBody PlanAssignmentRequest request) {
        planService.assignPlan(mercadoId, request);
        return buildStatus(mercadoId);
    }

    @GetMapping("/me/status")
    public PlanStatusDTO myStatus(@AuthenticationPrincipal CustomUserDetails principal) {
        validarMercado(principal);
        return buildStatus(principal.getId());
    }

    @PutMapping("/me/plan")
    public PlanStatusDTO assignMyPlan(@AuthenticationPrincipal CustomUserDetails principal,
                                      @RequestBody PlanAssignmentRequest request) {
        validarMercado(principal);
        planService.assignPlan(principal.getId(), request);
        return buildStatus(principal.getId());
    }

    @GetMapping("/mercados/{mercadoId}/permissions")
    public List<PermissionDecisionDTO> permissions(@PathVariable String mercadoId) {
        return permissionService.allForMercado(mercadoId);
    }

    @PostMapping("/events")
    public ResponseEntity<AnalyticsEvent> trackEvent(@RequestBody AnalyticsEventRequest request) {
        return ResponseEntity.ok(analyticsEventService.track(request));
    }

    @GetMapping("/admin/overview")
    public AdminMonetizationOverviewDTO overview() {
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        return new AdminMonetizationOverviewDTO(
                mercadoRepository.count(),
                planService.listPlans().size(),
                subscriptions.size(),
                analyticsEventRepository.count(),
                countStatus(subscriptions, SubscriptionStatus.TRIAL),
                countStatus(subscriptions, SubscriptionStatus.ACTIVE),
                countStatus(subscriptions, SubscriptionStatus.EXPIRED),
                countStatus(subscriptions, SubscriptionStatus.CANCELLED)
        );
    }

    private PlanStatusDTO buildStatus(String mercadoId) {
        Subscription subscription = planService.getOrCreateSubscription(mercadoId);
        Plan plan = planService.getPlanForMercado(mercadoId);
        UsageSnapshotDTO usage = usageService.snapshot(mercadoId);
        List<PermissionDecisionDTO> permissions = permissionService.allForMercado(mercadoId);
        return new PlanStatusDTO(mercadoId, plan, subscription, usage, permissions, "OBSERVE_ONLY_NO_BILLING");
    }

    private long countStatus(List<Subscription> subscriptions, SubscriptionStatus status) {
        return subscriptions.stream().filter(s -> s.getStatus() == status).count();
    }

    private void validarMercado(CustomUserDetails principal) {
        if (principal == null || !"MERCADO".equalsIgnoreCase(principal.getTipo())) {
            throw new IllegalArgumentException("Endpoint exclusivo para mercados autenticados");
        }
    }
}
