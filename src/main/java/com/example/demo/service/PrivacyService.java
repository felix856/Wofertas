package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.dto.LegalConsentRequest;
import com.example.demo.dto.PrivacyDeletionRequestDTO;
import com.example.demo.model.DataPrivacyRequest;
import com.example.demo.model.Mercado;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CurtidaRepository;
import com.example.demo.repository.DataPrivacyRequestRepository;
import com.example.demo.repository.FavoritoRepository;
import com.example.demo.repository.ItemCarrinhoRepository;
import com.example.demo.repository.MercadoRepository;
import com.example.demo.repository.OfertaRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.repository.VisualizacaoRepository;
import com.example.demo.security.CustomUserDetails;

@Service
public class PrivacyService {

    public static final String CURRENT_PRIVACY_VERSION = "2026-06-22";
    public static final String CURRENT_TERMS_VERSION = "2026-06-22";

    private final UsuarioRepository usuarioRepository;
    private final MercadoRepository mercadoRepository;
    private final DataPrivacyRequestRepository dataPrivacyRequestRepository;
    private final OfertaRepository ofertaRepository;
    private final FavoritoRepository favoritoRepository;
    private final CurtidaRepository curtidaRepository;
    private final VisualizacaoRepository visualizacaoRepository;
    private final ItemCarrinhoRepository itemCarrinhoRepository;

    public PrivacyService(UsuarioRepository usuarioRepository,
                          MercadoRepository mercadoRepository,
                          DataPrivacyRequestRepository dataPrivacyRequestRepository,
                          OfertaRepository ofertaRepository,
                          FavoritoRepository favoritoRepository,
                          CurtidaRepository curtidaRepository,
                          VisualizacaoRepository visualizacaoRepository,
                          ItemCarrinhoRepository itemCarrinhoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.mercadoRepository = mercadoRepository;
        this.dataPrivacyRequestRepository = dataPrivacyRequestRepository;
        this.ofertaRepository = ofertaRepository;
        this.favoritoRepository = favoritoRepository;
        this.curtidaRepository = curtidaRepository;
        this.visualizacaoRepository = visualizacaoRepository;
        this.itemCarrinhoRepository = itemCarrinhoRepository;
    }

    public Map<String, Object> legalInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("privacyPolicyVersion", CURRENT_PRIVACY_VERSION);
        info.put("termsVersion", CURRENT_TERMS_VERSION);
        info.put("privacyPolicyUrl", "/privacy-policy.html");
        info.put("termsUrl", "/termos.html");
        info.put("accountDeletionUrl", "/excluir-conta.html");
        info.put("dataProtectionContact", "privacidade@wofertas.com.br");
        return info;
    }

    public Map<String, Object> exportMyData(CustomUserDetails principal) {
        validarPrincipal(principal);
        return "MERCADO".equalsIgnoreCase(principal.getTipo())
                ? exportMercadoData(principal.getId())
                : exportUsuarioData(principal.getId());
    }

    public Map<String, Object> acceptLegal(CustomUserDetails principal, LegalConsentRequest request) {
        validarPrincipal(principal);
        if ("MERCADO".equalsIgnoreCase(principal.getTipo())) {
            Mercado mercado = mercadoRepository.findById(principal.getId())
                    .orElseThrow(() -> new RuntimeException("Mercado nao encontrado"));
            aplicarConsentimentoMercado(mercado, request);
            mercadoRepository.save(mercado);
        } else {
            Usuario usuario = usuarioRepository.findById(principal.getId())
                    .orElseThrow(() -> new RuntimeException("Usuario nao encontrado"));
            aplicarConsentimentoUsuario(usuario, request);
            usuarioRepository.save(usuario);
        }
        return legalInfo();
    }

    public DataPrivacyRequest requestMyDeletion(CustomUserDetails principal, PrivacyDeletionRequestDTO request) {
        validarPrincipal(principal);
        String email = principal.getUsername();
        DataPrivacyRequest privacyRequest = new DataPrivacyRequest(
                principal.getTipo(),
                principal.getId(),
                email,
                "ACCOUNT_DELETION",
                request.source() != null ? request.source() : "APP"
        );
        privacyRequest.setNotes(request.reason());
        DataPrivacyRequest saved = dataPrivacyRequestRepository.save(privacyRequest);
        marcarExclusaoSolicitada(principal.getTipo(), principal.getId());
        return saved;
    }

    public DataPrivacyRequest requestPublicDeletion(PrivacyDeletionRequestDTO request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("E-mail e obrigatorio para solicitar exclusao");
        }
        DataPrivacyRequest privacyRequest = new DataPrivacyRequest(
                normalizarTipo(request.requesterType()),
                null,
                request.email().trim().toLowerCase(),
                "ACCOUNT_DELETION",
                request.source() != null ? request.source() : "WEB"
        );
        privacyRequest.setNotes(request.reason());
        return dataPrivacyRequestRepository.save(privacyRequest);
    }

    public List<DataPrivacyRequest> myRequests(CustomUserDetails principal) {
        validarPrincipal(principal);
        return dataPrivacyRequestRepository.findByRequesterIdOrderByRequestedAtDesc(principal.getId());
    }

    private Map<String, Object> exportUsuarioData(String id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario nao encontrado"));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tipo", "USUARIO");
        Map<String, Object> perfil = new LinkedHashMap<>();
        perfil.put("id", usuario.getId());
        perfil.put("nome", safe(usuario.getNome()));
        perfil.put("email", safe(usuario.getEmail()));
        perfil.put("imagemPerfil", safe(usuario.getImagemPerfil()));
        perfil.put("privacyPolicyVersion", safe(usuario.getPrivacyPolicyVersion()));
        perfil.put("termsVersion", safe(usuario.getTermsVersion()));
        perfil.put("privacyAcceptedAt", usuario.getPrivacyAcceptedAt());
        perfil.put("termsAcceptedAt", usuario.getTermsAcceptedAt());
        perfil.put("marketingConsent", usuario.getMarketingConsent());
        perfil.put("analyticsConsent", usuario.getAnalyticsConsent());
        perfil.put("deletionRequestedAt", usuario.getDeletionRequestedAt());
        data.put("perfil", perfil);
        data.put("resumo", Map.of(
                "mercadosFavoritados", favoritoRepository.findByIdUsuario(id).size(),
                "curtidas", curtidaRepository.findByIdUsuario(id).size(),
                "itensCarrinho", itemCarrinhoRepository.findByIdUsuario(id).size()
        ));
        data.put("solicitacoesPrivacidade", dataPrivacyRequestRepository.findByRequesterIdOrderByRequestedAtDesc(id));
        return data;
    }

    private Map<String, Object> exportMercadoData(String id) {
        Mercado mercado = mercadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mercado nao encontrado"));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tipo", "MERCADO");
        Map<String, Object> perfil = new LinkedHashMap<>();
        perfil.put("id", mercado.getId());
        perfil.put("nome", safe(mercado.getNome()));
        perfil.put("email", safe(mercado.getEmail()));
        perfil.put("cnpj", safe(mercado.getCnpj()));
        perfil.put("telefone", safe(mercado.getTelefone()));
        perfil.put("endereco", safe(mercado.getEndereco()));
        perfil.put("latitude", mercado.getLatitude());
        perfil.put("longitude", mercado.getLongitude());
        perfil.put("privacyPolicyVersion", safe(mercado.getPrivacyPolicyVersion()));
        perfil.put("termsVersion", safe(mercado.getTermsVersion()));
        perfil.put("privacyAcceptedAt", mercado.getPrivacyAcceptedAt());
        perfil.put("termsAcceptedAt", mercado.getTermsAcceptedAt());
        perfil.put("marketingConsent", mercado.getMarketingConsent());
        perfil.put("analyticsConsent", mercado.getAnalyticsConsent());
        perfil.put("deletionRequestedAt", mercado.getDeletionRequestedAt());
        data.put("perfil", perfil);
        data.put("resumo", Map.of(
                "ofertasPublicadas", ofertaRepository.findByMercadoId(id).size(),
                "favoritosRecebidos", favoritoRepository.countByIdMercado(id),
                "visualizacoesRegistradas", ofertaRepository.findByMercadoId(id).stream()
                        .mapToLong(oferta -> visualizacaoRepository.countByIdOferta(oferta.getId()))
                        .sum()
        ));
        data.put("solicitacoesPrivacidade", dataPrivacyRequestRepository.findByRequesterIdOrderByRequestedAtDesc(id));
        return data;
    }

    private void aplicarConsentimentoUsuario(Usuario usuario, LegalConsentRequest request) {
        LocalDateTime now = LocalDateTime.now();
        if (Boolean.TRUE.equals(request.acceptPrivacyPolicy())) {
            usuario.setPrivacyPolicyVersion(versionOrCurrent(request.privacyPolicyVersion(), CURRENT_PRIVACY_VERSION));
            usuario.setPrivacyAcceptedAt(now);
        }
        if (Boolean.TRUE.equals(request.acceptTerms())) {
            usuario.setTermsVersion(versionOrCurrent(request.termsVersion(), CURRENT_TERMS_VERSION));
            usuario.setTermsAcceptedAt(now);
        }
        if (request.marketingConsent() != null) usuario.setMarketingConsent(request.marketingConsent());
        if (request.analyticsConsent() != null) usuario.setAnalyticsConsent(request.analyticsConsent());
    }

    private void aplicarConsentimentoMercado(Mercado mercado, LegalConsentRequest request) {
        LocalDateTime now = LocalDateTime.now();
        if (Boolean.TRUE.equals(request.acceptPrivacyPolicy())) {
            mercado.setPrivacyPolicyVersion(versionOrCurrent(request.privacyPolicyVersion(), CURRENT_PRIVACY_VERSION));
            mercado.setPrivacyAcceptedAt(now);
        }
        if (Boolean.TRUE.equals(request.acceptTerms())) {
            mercado.setTermsVersion(versionOrCurrent(request.termsVersion(), CURRENT_TERMS_VERSION));
            mercado.setTermsAcceptedAt(now);
        }
        if (request.marketingConsent() != null) mercado.setMarketingConsent(request.marketingConsent());
        if (request.analyticsConsent() != null) mercado.setAnalyticsConsent(request.analyticsConsent());
    }

    private void marcarExclusaoSolicitada(String tipo, String id) {
        LocalDateTime now = LocalDateTime.now();
        if ("MERCADO".equalsIgnoreCase(tipo)) {
            mercadoRepository.findById(id).ifPresent(mercado -> {
                mercado.setDeletionRequestedAt(now);
                mercadoRepository.save(mercado);
            });
            return;
        }
        usuarioRepository.findById(id).ifPresent(usuario -> {
            usuario.setDeletionRequestedAt(now);
            usuarioRepository.save(usuario);
        });
    }

    private void validarPrincipal(CustomUserDetails principal) {
        if (principal == null || principal.getId() == null || principal.getId().isBlank()) {
            throw new RuntimeException("Usuario nao autenticado");
        }
    }

    private String normalizarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) return "DESCONHECIDO";
        String normalized = tipo.trim().toUpperCase();
        return List.of("USUARIO", "MERCADO").contains(normalized) ? normalized : "DESCONHECIDO";
    }

    private String versionOrCurrent(String candidate, String current) {
        return candidate == null || candidate.isBlank() ? current : candidate;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
