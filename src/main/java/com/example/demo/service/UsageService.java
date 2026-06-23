package com.example.demo.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.demo.dto.UsageSnapshotDTO;
import com.example.demo.model.UsageCounter;
import com.example.demo.repository.UsageCounterRepository;

@Service
public class UsageService {

    private static final String WEEK = "WEEK";
    private static final String MONTH = "MONTH";

    private final UsageCounterRepository repository;

    public UsageService(UsageCounterRepository repository) {
        this.repository = repository;
    }

    public void recordFlyerCreated(String mercadoId) {
        UsageCounter counter = currentWeek(mercadoId);
        counter.setFlyersCreated(counter.getFlyersCreated() + 1);
        counter.setUpdatedAt(LocalDateTime.now());
        repository.save(counter);
    }

    public void recordOfferCreated(String mercadoId) {
        UsageCounter counter = currentMonth(mercadoId);
        counter.setOffersCreated(counter.getOffersCreated() + 1);
        counter.setUpdatedAt(LocalDateTime.now());
        repository.save(counter);
    }

    public void recordBoostUsed(String mercadoId) {
        UsageCounter counter = currentWeek(mercadoId);
        counter.setBoostsUsed(counter.getBoostsUsed() + 1);
        counter.setUpdatedAt(LocalDateTime.now());
        repository.save(counter);
    }

    public UsageSnapshotDTO snapshot(String mercadoId) {
        UsageCounter week = currentWeek(mercadoId);
        UsageCounter month = currentMonth(mercadoId);
        return new UsageSnapshotDTO(
                mercadoId,
                week.getPeriodStart(),
                week.getPeriodEnd(),
                week.getFlyersCreated(),
                week.getBoostsUsed(),
                month.getPeriodStart(),
                month.getPeriodEnd(),
                month.getOffersCreated()
        );
    }

    private UsageCounter currentWeek(String mercadoId) {
        LocalDate start = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate end = start.plusDays(7);
        return repository.findByMercadoIdAndPeriodTypeAndPeriodStart(mercadoId, WEEK, start)
                .orElseGet(() -> repository.save(new UsageCounter(mercadoId, WEEK, start, end)));
    }

    private UsageCounter currentMonth(String mercadoId) {
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = start.plusMonths(1);
        return repository.findByMercadoIdAndPeriodTypeAndPeriodStart(mercadoId, MONTH, start)
                .orElseGet(() -> repository.save(new UsageCounter(mercadoId, MONTH, start, end)));
    }
}
