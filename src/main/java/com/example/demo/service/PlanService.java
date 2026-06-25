package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.dto.PlanAssignmentRequest;
import com.example.demo.model.Mercado;
import com.example.demo.model.Plan;
import com.example.demo.model.PlanName;
import com.example.demo.model.Subscription;
import com.example.demo.model.SubscriptionStatus;
import com.example.demo.repository.MercadoRepository;
import com.example.demo.repository.PlanRepository;
import com.example.demo.repository.SubscriptionRepository;

@Service
public class PlanService {

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final MercadoRepository mercadoRepository;

    public PlanService(PlanRepository planRepository,
                       SubscriptionRepository subscriptionRepository,
                       MercadoRepository mercadoRepository) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.mercadoRepository = mercadoRepository;
    }

    public void ensureDefaultPlans() {
        upsertPlan(new Plan(PlanName.FREE, BigDecimal.ZERO, 2, 10, 0, 0, "basic"));
        upsertPlan(new Plan(PlanName.BASIC, new BigDecimal("49.90"), 10, 50, 1, 1, "intermediate"));
        upsertPlan(new Plan(PlanName.PRO, new BigDecimal("99.90"), -1, -1, 5, 2, "advanced"));
        upsertPlan(new Plan(PlanName.PREMIUM, new BigDecimal("199.90"), -1, -1, -1, 3, "premium"));
    }

    public List<Plan> listPlans() {
        ensureDefaultPlans();
        return planRepository.findAll();
    }

    public Plan getPlan(PlanName name) {
        ensureDefaultPlans();
        return planRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Plano nao encontrado: " + name));
    }

    public Subscription assignPlan(String mercadoId, PlanAssignmentRequest request) {
        Mercado mercado = mercadoRepository.findById(mercadoId)
                .orElseThrow(() -> new IllegalArgumentException("Mercado nao encontrado"));

        PlanName planName = parsePlanName(request.planName());
        Plan plan = getPlan(planName);
        SubscriptionStatus status = parseStatus(request.status());

        Subscription subscription = subscriptionRepository.findTopByMercadoIdOrderByCreatedAtDesc(mercado.getId())
                .orElseGet(() -> new Subscription(mercado.getId(), plan.getId(), plan.getName(), status));

        subscription.setMercadoId(mercado.getId());
        subscription.setUserId(mercado.getId());
        subscription.setPlanId(plan.getId());
        subscription.setPlanName(plan.getName());
        subscription.setStatus(status);
        subscription.setProvider("manual");
        subscription.setAutoRenew(Boolean.TRUE.equals(request.autoRenew()));
        subscription.setExpiresAt(request.expiresAt());
        if (subscription.getStartedAt() == null) {
            subscription.setStartedAt(LocalDateTime.now());
        }
        if (subscription.getCreatedAt() == null) {
            subscription.setCreatedAt(LocalDateTime.now());
        }
        subscription.setUpdatedAt(LocalDateTime.now());

        return subscriptionRepository.save(subscription);
    }

    public Subscription getOrCreateSubscription(String mercadoId) {
        return subscriptionRepository.findTopByMercadoIdOrderByCreatedAtDesc(mercadoId)
                .orElseGet(() -> {
                    Plan free = getPlan(PlanName.FREE);
                    return subscriptionRepository.save(new Subscription(
                            mercadoId,
                            free.getId(),
                            PlanName.FREE,
                            SubscriptionStatus.TRIAL
                    ));
                });
    }

    public Plan getPlanForMercado(String mercadoId) {
        Subscription subscription = getOrCreateSubscription(mercadoId);
        PlanName planName = subscription.getPlanName() != null ? subscription.getPlanName() : PlanName.FREE;
        return getPlan(planName);
    }

    public List<Subscription> listSubscriptions() {
        return subscriptionRepository.findAll();
    }

    private void upsertPlan(Plan defaults) {
        Plan plan = planRepository.findByName(defaults.getName()).orElse(defaults);
        plan.setPrice(defaults.getPrice());
        plan.setWeeklyFlyerLimit(defaults.getWeeklyFlyerLimit());
        plan.setMonthlyOfferLimit(defaults.getMonthlyOfferLimit());
        plan.setBoostCredits(defaults.getBoostCredits());
        plan.setPriorityLevel(defaults.getPriorityLevel());
        plan.setAnalyticsLevel(defaults.getAnalyticsLevel());
        if (plan.getCreatedAt() == null) {
            plan.setCreatedAt(LocalDateTime.now());
        }
        plan.setUpdatedAt(LocalDateTime.now());
        planRepository.save(plan);
    }

    private PlanName parsePlanName(String raw) {
        if (raw == null || raw.isBlank()) {
            return PlanName.FREE;
        }
        return PlanName.valueOf(raw.trim().toUpperCase());
    }

    private SubscriptionStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return SubscriptionStatus.TRIAL;
        }
        return SubscriptionStatus.valueOf(raw.trim().toUpperCase());
    }
}
