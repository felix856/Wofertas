package com.example.demo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.example.demo.dto.ChatbotResponse;
import com.example.demo.model.Mercado;
import com.example.demo.repository.MercadoRepository;
import com.example.demo.security.JwtUtil;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class ChatbotAssistantIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MercadoRepository mercadoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private Mercado mercado;

    @BeforeEach
    void setup() {
        mercadoRepository.deleteAll();

        mercado = new Mercado();
        mercado.setNome("Mercado IA Teste");
        mercado.setCnpj("11.111.111/0001-11");
        mercado.setEndereco("Rua dos Testes, 123");
        mercado.setTelefone("48999999999");
        mercado.setEmail("mercado-ia@example.com");
        mercado.setSenha(passwordEncoder.encode("SenhaTeste123"));
        mercado.setImagemLogo("/uploads/logos/mercado-ia.png");

        mercado = mercadoRepository.save(mercado);
    }

    @Test
    void deveResponderEmModoLocalQuandoAnthropicNaoEstaConfigurado() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtUtil.generateToken(mercado.getId(), "MERCADO", mercado.getEmail()));

        Map<String, String> request = Map.of(
                "mensagem", "Como publico uma oferta?",
                "pagina", "/dashboard-analytics.html",
                "contextoTela", "dashboard-analytics"
        );

        ResponseEntity<ChatbotResponse> response = restTemplate.postForEntity(
                "/api/chatbot/mensagem",
                new HttpEntity<>(request, headers),
                ChatbotResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().modo()).isEqualTo("FALLBACK_LOCAL");
        assertThat(response.getBody().tipoUsuario()).isEqualTo("MERCADO");
        assertThat(response.getBody().contextoAnaliticoUsado()).isTrue();
        assertThat(response.getBody().resposta()).containsIgnoringCase("oferta");
    }

    @Test
    void deveExigirAutenticacaoParaUsarAssistente() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/chatbot/mensagem",
                new HttpEntity<>(Map.of("mensagem", "Preciso de ajuda"), headers),
                String.class
        );

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }
}
