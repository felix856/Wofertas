package com.example.demo.service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.demo.dto.ChatbotRequest;
import com.example.demo.dto.ChatbotResponse;
import com.example.demo.dto.DashboardAnalyticsDTO;
import com.example.demo.model.Mercado;
import com.example.demo.repository.MercadoRepository;
import com.example.demo.security.CustomUserDetails;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class ChatbotAssistantService {

    private static final Logger logger = LoggerFactory.getLogger(ChatbotAssistantService.class);
    private static final int MAX_MESSAGE_LENGTH = 1600;
    private static final String ANTHROPIC_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final String anthropicApiKey;
    private final String model;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final MercadoRepository mercadoRepository;
    private final AnalyticsService analyticsService;

    public ChatbotAssistantService(
            @Value("${anthropic.api.key:vazio}") String anthropicApiKey,
            @Value("${anthropic.model:claude-3-5-sonnet-20241022}") String model,
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper mapper,
            MercadoRepository mercadoRepository,
            AnalyticsService analyticsService
    ) {
        this.anthropicApiKey = anthropicApiKey;
        this.model = model;
        this.mapper = mapper;
        this.mercadoRepository = mercadoRepository;
        this.analyticsService = analyticsService;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(8))
                .setReadTimeout(Duration.ofSeconds(25))
                .build();
    }

    public ChatbotResponse responder(ChatbotRequest request, CustomUserDetails userDetails) {
        String mensagem = sanitizarMensagem(request == null ? null : request.mensagem());
        if (mensagem.isBlank()) {
            return respostaLocal(
                    "Envie uma pergunta sobre o painel, ofertas, encartes, ranking ou dashboard analytics.",
                    tipoUsuario(userDetails),
                    false
            );
        }

        AssistantContext context = montarContexto(userDetails, request);

        if (!anthropicConfigurado()) {
            return respostaLocal(gerarRespostaLocal(mensagem, context), context.tipoUsuario(), context.usouAnalytics());
        }

        try {
            String resposta = chamarAnthropic(mensagem, context);
            return new ChatbotResponse(
                    resposta,
                    context.tipoUsuario(),
                    "ANTHROPIC",
                    model,
                    context.usouAnalytics(),
                    Instant.now()
            );
        } catch (RestClientException | JsonProcessingException | IllegalStateException e) {
            logger.warn("Falha ao chamar Anthropic. Usando fallback local: {}", e.getMessage());
            return respostaLocal(gerarRespostaLocal(mensagem, context), context.tipoUsuario(), context.usouAnalytics());
        }
    }

    private AssistantContext montarContexto(CustomUserDetails userDetails, ChatbotRequest request) {
        String tipo = tipoUsuario(userDetails);
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("pagina", valorSeguro(request == null ? null : request.pagina()));
        dados.put("contextoTela", valorSeguro(request == null ? null : request.contextoTela()));

        if (!"MERCADO".equals(tipo) || userDetails == null) {
            return new AssistantContext(tipo, promptBase(tipo, dados), dados, false);
        }

        Mercado mercado = mercadoRepository.findByEmail(userDetails.getUsername());
        if (mercado == null) {
            return new AssistantContext(tipo, promptBase(tipo, dados), dados, false);
        }

        DashboardAnalyticsDTO dashboard = analyticsService.gerarDashboardMercado(mercado.getId());
        dados.put("mercado", mercado.getNome());
        dados.put("totalVisualizacoes", dashboard.totalVisualizacoes());
        dados.put("totalCurtidas", dashboard.totalCurtidas());
        dados.put("totalFavoritos", dashboard.totalFavoritos());
        dados.put("totalItensCarrinho", dashboard.totalItensCarrinho());
        dados.put("totalEncartes", dashboard.totalEncartes());
        dados.put("taxaConversaoCurtidas", dashboard.taxaConversaoVisualizacoesCurtidas());
        dados.put("taxaConversaoCarrinho", dashboard.taxaConversaoVisualizacoesCarrinho());
        dados.put("melhorEncarte", dashboard.insight().getEncarteMelhorPerformance());
        dados.put("clientesAtivos", dashboard.insight().getClientesAtivos());
        dados.put("tendencia", dashboard.insight().getTendencia());
        dados.put("recomendacaoAtual", dashboard.insight().getRecomendacao());
        dados.put("topProdutosCurtidas", dashboard.produtosComMaiorCurtidas());
        dados.put("topProdutosCarrinho", dashboard.produtosComMaiorCarrinho());

        return new AssistantContext(tipo, promptBase(tipo, dados), dados, true);
    }

    private String promptBase(String tipoUsuario, Map<String, Object> dados) {
        return """
                Voce e o Assistente Virtual Interativo do Wofertas, integrado ao painel web do supermercado.
                Responda sempre em portugues do Brasil, com tom claro, profissional e objetivo.
                Sua funcao e agir como tutorial dinamico para o lojista: explicar telas, orientar cadastro de ofertas,
                upload de encartes, leitura do dashboard analytics, ranking de mercados e proximos passos comerciais.
                Use os dados analiticos agregados apenas para gerar insights praticos. Nunca exponha dados pessoais,
                tokens, senhas, emails de clientes ou informacoes sensiveis. Nao invente metricas.
                Se a pergunta fugir do Wofertas, responda brevemente e conduza de volta para o uso da plataforma.
                Tipo de usuario: %s.
                Contexto seguro disponivel: %s
                """.formatted(tipoUsuario, dados);
    }

    private String chamarAnthropic(String mensagem, AssistantContext context) throws JsonProcessingException {
        ObjectNode requestBody = mapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.put("max_tokens", 650);
        requestBody.put("temperature", 0.3);
        requestBody.put("system", context.systemPrompt());

        ArrayNode messages = mapper.createArrayNode();
        messages.add(mapper.createObjectNode()
                .put("role", "user")
                .put("content", mensagem));
        requestBody.set("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", anthropicApiKey);
        headers.set("anthropic-version", ANTHROPIC_VERSION);

        ResponseEntity<String> response = restTemplate.postForEntity(
                ANTHROPIC_URL,
                new HttpEntity<>(mapper.writeValueAsString(requestBody), headers),
                String.class
        );

        JsonNode root = mapper.readTree(response.getBody());
        JsonNode content = root.path("content");
        if (!content.isArray() || content.isEmpty()) {
            throw new IllegalStateException("Resposta da Anthropic sem content.");
        }

        String resposta = content.get(0).path("text").asText("");
        if (resposta.isBlank()) {
            throw new IllegalStateException("Resposta da Anthropic vazia.");
        }
        return resposta.trim();
    }

    private String gerarRespostaLocal(String mensagem, AssistantContext context) {
        String m = mensagem.toLowerCase();

        if (m.contains("oferta") || m.contains("publicar")) {
            return "Para publicar uma oferta, acesse Nova Oferta, preencha nome, preco, mercado, imagem e validade. Use imagens claras e titulos objetivos para melhorar o engajamento.";
        }

        if (m.contains("encarte") || m.contains("pdf")) {
            return "Para trabalhar com encartes, envie o PDF promocional pelo painel e confira depois no historico. Se o encarte nao abrir, valide o arquivo e teste em uma rede estavel.";
        }

        if (m.contains("ranking") || m.contains("concorrente")) {
            return "O ranking compara mercados pelo engajamento das ofertas, principalmente curtidas e favoritos. Para subir posicoes, publique ofertas consistentes e acompanhe quais produtos geram mais interacao.";
        }

        if (m.contains("dashboard") || m.contains("metric") || m.contains("analytics")) {
            Optional<Object> recomendacao = Optional.ofNullable(context.dados().get("recomendacaoAtual"));
            return recomendacao
                    .map(valor -> "No dashboard, acompanhe visualizacoes, curtidas, favoritos e carrinhos. Insight atual: " + valor)
                    .orElse("No dashboard, acompanhe visualizacoes, curtidas, favoritos e carrinhos para entender quais ofertas atraem mais clientes.");
        }

        if (m.contains("come") || m.contains("primeiro") || m.contains("ajuda")) {
            return "Comece pelo fluxo basico: complete o perfil do mercado, cadastre uma oferta, publique um encarte e depois acompanhe o desempenho no dashboard analytics.";
        }

        return "Posso ajudar com cadastro de ofertas, upload de encartes, leitura de metricas, ranking competitivo e uso do painel web. Me diga em qual etapa voce esta.";
    }

    private boolean anthropicConfigurado() {
        if (anthropicApiKey == null) return false;
        String key = anthropicApiKey.trim();
        return !key.isBlank()
                && !"vazio".equalsIgnoreCase(key)
                && !"test-key".equalsIgnoreCase(key)
                && !"test-key-dev".equalsIgnoreCase(key);
    }

    private String sanitizarMensagem(String mensagem) {
        if (mensagem == null) return "";
        String limpa = mensagem.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", " ").trim();
        if (limpa.length() <= MAX_MESSAGE_LENGTH) return limpa;
        return limpa.substring(0, MAX_MESSAGE_LENGTH);
    }

    private String tipoUsuario(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getTipo() == null) return "ANONIMO";
        return userDetails.getTipo().toUpperCase();
    }

    private String valorSeguro(String valor) {
        if (valor == null || valor.isBlank()) return "nao informado";
        return valor.length() > 120 ? valor.substring(0, 120) : valor;
    }

    private ChatbotResponse respostaLocal(String resposta, String tipoUsuario, boolean contextoAnaliticoUsado) {
        return new ChatbotResponse(
                resposta,
                tipoUsuario,
                "FALLBACK_LOCAL",
                model,
                contextoAnaliticoUsado,
                Instant.now()
        );
    }

    private record AssistantContext(
            String tipoUsuario,
            String systemPrompt,
            Map<String, Object> dados,
            boolean usouAnalytics
    ) {}
}
