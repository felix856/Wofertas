package com.example.demo.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.dto.AnalyticsEventRequest;
import com.example.demo.model.AnalyticsEvent;
import com.example.demo.model.Oferta;
import com.example.demo.repository.AnalyticsEventRepository;
import com.example.demo.repository.OfertaRepository;

@Service
public class AnalyticsEventService {

    public static final String OFFER_VIEW = "offer_view";
    public static final String OFFER_CLICK = "offer_click";
    public static final String OFFER_FAVORITE = "offer_favorite";
    public static final String OFFER_SHARE = "offer_share";
    public static final String WHATSAPP_CLICK = "whatsapp_click";
    public static final String CALL_CLICK = "call_click";
    public static final String LOCATION_CLICK = "location_click";
    public static final String STORE_VIEW = "store_view";
    public static final String FLYER_VIEW = "flyer_view";

    private final AnalyticsEventRepository repository;
    private final OfertaRepository ofertaRepository;

    public AnalyticsEventService(AnalyticsEventRepository repository, OfertaRepository ofertaRepository) {
        this.repository = repository;
        this.ofertaRepository = ofertaRepository;
    }

    public AnalyticsEvent track(AnalyticsEventRequest request) {
        String eventType = normalizeEventType(request.eventType());
        AnalyticsEvent event = new AnalyticsEvent(
                eventType,
                request.userId(),
                request.storeId(),
                request.offerId(),
                request.metadata()
        );
        event.setFlyerId(request.flyerId());
        return repository.save(event);
    }

    public AnalyticsEvent trackOfferEvent(String eventType, String offerId, String userId, String origem) {
        String storeId = null;
        if (offerId != null && !offerId.isBlank()) {
            storeId = ofertaRepository.findById(offerId).map(Oferta::getMercadoId).orElse(null);
        }
        return repository.save(new AnalyticsEvent(
                normalizeEventType(eventType),
                userId,
                storeId,
                offerId,
                Map.of("origem", origem != null ? origem : "DESCONHECIDA")
        ));
    }

    public AnalyticsEvent trackStoreEvent(String eventType, String storeId, String userId, Map<String, Object> metadata) {
        return repository.save(new AnalyticsEvent(
                normalizeEventType(eventType),
                userId,
                storeId,
                null,
                metadata
        ));
    }

    public List<AnalyticsEvent> recentByStore(String storeId) {
        return repository.findTop100ByStoreIdOrderByCreatedAtDesc(storeId);
    }

    public long countAll() {
        return repository.count();
    }

    public long countByStore(String storeId) {
        return repository.countByStoreId(storeId);
    }

    private String normalizeEventType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("eventType e obrigatorio");
        }
        return raw.trim().toLowerCase();
    }
}
