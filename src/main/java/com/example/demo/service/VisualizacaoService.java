package com.example.demo.service;

import com.example.demo.model.Visualizacao;
import com.example.demo.repository.VisualizacaoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VisualizacaoService {

    private final VisualizacaoRepository visualizacaoRepository;
    private final AnalyticsEventService analyticsEventService;

    public VisualizacaoService(VisualizacaoRepository visualizacaoRepository,
                               AnalyticsEventService analyticsEventService) {
        this.visualizacaoRepository = visualizacaoRepository;
        this.analyticsEventService = analyticsEventService;
    }

    public Visualizacao registrarVisualizacao(String idOferta, String idUsuario, String origem) {
        Visualizacao viz = new Visualizacao(idOferta, idUsuario, origem);
        Visualizacao salva = visualizacaoRepository.save(viz);
        registrarEventoAnalytics(idOferta, idUsuario, origem);
        return salva;
    }

    public long contagemVisualizacoes(String idOferta) {
        return visualizacaoRepository.countByIdOferta(idOferta);
    }

    public List<Visualizacao> listarVisualizacoesPorOferta(String idOferta) {
        return visualizacaoRepository.findByIdOferta(idOferta);
    }

    public List<Visualizacao> listarVisualizacoesPorOrigem(String idOferta, String origem) {
        return visualizacaoRepository.findByIdOfertaAndOrigem(idOferta, origem);
    }

    private void registrarEventoAnalytics(String idOferta, String idUsuario, String origem) {
        try {
            analyticsEventService.trackOfferEvent(detectarTipoEvento(origem), idOferta, idUsuario, origem);
        } catch (RuntimeException ignored) {
            // Analytics desacoplado: uma falha aqui nao deve impedir a interacao principal.
        }
    }

    private String detectarTipoEvento(String origem) {
        String valor = origem == null ? "" : origem.toLowerCase();
        if (valor.contains("whatsapp")) return AnalyticsEventService.WHATSAPP_CLICK;
        if (valor.contains("call") || valor.contains("telefone")) return AnalyticsEventService.CALL_CLICK;
        if (valor.contains("location") || valor.contains("localizacao")) return AnalyticsEventService.LOCATION_CLICK;
        if (valor.contains("share") || valor.contains("compart")) return AnalyticsEventService.OFFER_SHARE;
        if (valor.contains("click")) return AnalyticsEventService.OFFER_CLICK;
        if (valor.contains("flyer") || valor.contains("encarte")) return AnalyticsEventService.FLYER_VIEW;
        return AnalyticsEventService.OFFER_VIEW;
    }
}
