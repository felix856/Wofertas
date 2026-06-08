package com.example.demo.controller;

import com.example.demo.dto.DashboardAnalyticsDTO;
import com.example.demo.repository.MercadoRepository;
import com.example.demo.security.JwtUtil;
import com.example.demo.service.AnalyticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;

@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "*")
public class ChatbotController {

    private static final Logger logger =
            LoggerFactory.getLogger(ChatbotController.class);

    @Value("${anthropic.api.key}")
    private String anthropicApiKey;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private MercadoRepository mercadoRepository;

    private static final String ANTHROPIC_URL =
            "https://api.anthropic.com/v1/messages";

    private static final String MODEL =
            "claude-3-5-sonnet-20241022";

    private static final String BASE =
            "Você é o assistente virtual do Wofertas. Responda em português, de forma curta e direta.";

    private static final String PROMPT_ANONIMO =
            BASE + " CONTEXTO: Visitante não logado.";

    private static final String PROMPT_USUARIO =
            BASE + " CONTEXTO: Consumidor logado.";

    private static final String PROMPT_MERCADO =
            BASE + " CONTEXTO: Mercado parceiro.";

    @PostMapping("/mensagem")
    public ResponseEntity<ObjectNode> responder(
            @RequestBody ObjectNode body,
            @RequestHeader(
                    value="Authorization",
                    required=false
            ) String authHeader
    ) {

        ObjectMapper mapper = new ObjectMapper();

        try {

            String mensagem =
                    body.has("mensagem")
                            ? body.get("mensagem")
                            .asText()
                            .trim()
                            : "";

            if(mensagem.isEmpty()){

                return ResponseEntity
                        .badRequest()
                        .body(
                                mapper.createObjectNode()
                                        .put(
                                                "resposta",
                                                "Envie uma mensagem."
                                        )
                        );
            }

            String systemPrompt =
                    PROMPT_ANONIMO;

            String tipoRetorno =
                    "ANONIMO";

            String token =
                    extrairToken(authHeader);

            if(token!=null &&
                    jwtUtil.isTokenValid(token)){

                try{

                    String tipo =
                            jwtUtil.extractTipo(token);

                    String email =
                            jwtUtil.extractUsername(token);

                    if("MERCADO".equals(tipo)
                            && email!=null){

                        tipoRetorno="MERCADO";

                        var mercado=
                                mercadoRepository
                                        .findByEmail(email);

                        if(mercado!=null){

                            DashboardAnalyticsDTO stats =
                                    analyticsService
                                            .gerarDashboardMercado(
                                                    mercado.getId()
                                            );

                            systemPrompt=
                                    PROMPT_MERCADO
                                            + "\n[DADOS]: "
                                            + stats
                                            .insight()
                                            .getRecomendacao();
                        }

                    }else{

                        tipoRetorno=
                                "USUARIO";

                        systemPrompt=
                                PROMPT_USUARIO;
                    }

                }
                catch(Exception e){

                    logger.warn(
                            "Erro JWT: {}",
                            e.getMessage()
                    );
                }
            }

            ObjectNode requestBody =
                    mapper.createObjectNode();

            requestBody.put(
                    "model",
                    MODEL
            );

            requestBody.put(
                    "max_tokens",
                    512
            );

            requestBody.put(
                    "system",
                    systemPrompt
            );

            ArrayNode messages =
                    mapper.createArrayNode();

            messages.add(
                    mapper.createObjectNode()
                            .put(
                                    "role",
                                    "user"
                            )
                            .put(
                                    "content",
                                    mensagem
                            )
            );

            requestBody.set(
                    "messages",
                    messages
            );

            HttpHeaders headers=
                    new HttpHeaders();

            headers.setContentType(
                    MediaType.APPLICATION_JSON
            );

            headers.set(
                    "x-api-key",
                    anthropicApiKey
            );

            headers.set(
                    "anthropic-version",
                    "2023-06-01"
            );

            HttpEntity<String> entity=
                    new HttpEntity<>(
                            mapper.writeValueAsString(
                                    requestBody
                            ),
                            headers
                    );

            ResponseEntity<String> apiResponse=
                    new RestTemplate()
                            .postForEntity(
                                    ANTHROPIC_URL,
                                    entity,
                                    String.class
                            );

            String resposta=
                    mapper.readTree(
                                    apiResponse.getBody()
                            )
                            .path("content")
                            .get(0)
                            .path("text")
                            .asText();

            return ResponseEntity.ok(
                    mapper.createObjectNode()
                            .put(
                                    "resposta",
                                    resposta
                            )
                            .put(
                                    "tipo",
                                    tipoRetorno
                            )
            );

        }
        catch(
                JsonProcessingException |
                RestClientException e
        ){

            logger.error(
                    "Erro chatbot",
                    e
            );

            return ResponseEntity
                    .status(500)
                    .body(
                            mapper.createObjectNode()
                                    .put(
                                            "resposta",
                                            "Erro temporário."
                                    )
                    );
        }
    }

    private String extrairToken(
            String authHeader
    ){

        if(authHeader!=null &&
                authHeader.startsWith(
                        "Bearer "
                )){

            return authHeader.substring(
                    7
            );
        }

        return null;
    }
}