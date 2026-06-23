package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.dto.PermissionDecisionDTO;
import com.example.demo.dto.UsageSnapshotDTO;
import com.example.demo.model.Plan;

@Service
public class PermissionService {

    private static final boolean ENFORCEMENT_ENABLED = false;

    private final PlanService planService;
    private final UsageService usageService;

    public PermissionService(PlanService planService, UsageService usageService) {
        this.planService = planService;
        this.usageService = usageService;
    }

    public PermissionDecisionDTO canCreateFlyer(String mercadoId) {
        Plan plan = planService.getPlanForMercado(mercadoId);
        UsageSnapshotDTO usage = usageService.snapshot(mercadoId);
        boolean unlimited = plan.hasUnlimitedFlyers();
        boolean wouldAllow = unlimited || usage.flyersCreatedThisWeek() < plan.getWeeklyFlyerLimit();
        return decision("create_flyer", plan, usage.flyersCreatedThisWeek(), plan.getWeeklyFlyerLimit(), wouldAllow);
    }

    public PermissionDecisionDTO canCreateUnlimitedOffers(String mercadoId) {
        Plan plan = planService.getPlanForMercado(mercadoId);
        UsageSnapshotDTO usage = usageService.snapshot(mercadoId);
        boolean unlimited = plan.hasUnlimitedOffers();
        boolean wouldAllow = unlimited || usage.offersCreatedThisMonth() < plan.getMonthlyOfferLimit();
        return decision("create_offer", plan, usage.offersCreatedThisMonth(), plan.getMonthlyOfferLimit(), wouldAllow);
    }

    public PermissionDecisionDTO canUseBoost(String mercadoId) {
        Plan plan = planService.getPlanForMercado(mercadoId);
        UsageSnapshotDTO usage = usageService.snapshot(mercadoId);
        boolean unlimited = plan.hasUnlimitedBoosts();
        boolean wouldAllow = unlimited || usage.boostsUsedThisWeek() < plan.getBoostCredits();
        return decision("use_boost", plan, usage.boostsUsedThisWeek(), plan.getBoostCredits(), wouldAllow);
    }

    public PermissionDecisionDTO canAccessAdvancedAnalytics(String mercadoId) {
        Plan plan = planService.getPlanForMercado(mercadoId);
        boolean wouldAllow = List.of("advanced", "premium").contains(plan.getAnalyticsLevel());
        return decision("advanced_analytics", plan, 0, 0, wouldAllow);
    }

    public List<PermissionDecisionDTO> allForMercado(String mercadoId) {
        return List.of(
                canCreateFlyer(mercadoId),
                canCreateUnlimitedOffers(mercadoId),
                canUseBoost(mercadoId),
                canAccessAdvancedAnalytics(mercadoId)
        );
    }

    private PermissionDecisionDTO decision(String permission, Plan plan, int currentUsage, int limit, boolean wouldAllow) {
        boolean allowed = ENFORCEMENT_ENABLED ? wouldAllow : true;
        String reason = wouldAllow
                ? "Permitido pelo plano " + plan.getName()
                : "Limite do plano " + plan.getName() + " atingido; modo observacao nao bloqueia.";
        return new PermissionDecisionDTO(
                permission,
                allowed,
                wouldAllow,
                ENFORCEMENT_ENABLED,
                currentUsage,
                limit,
                plan.getName().name(),
                reason
        );
    }
}
