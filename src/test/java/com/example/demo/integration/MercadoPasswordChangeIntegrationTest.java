package com.example.demo.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.dto.PasswordChangeRequest;
import com.example.demo.model.Mercado;
import com.example.demo.repository.MercadoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2E Integration Tests para Mercado Password Change
 * Simula requisições do Android app para mercados
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class MercadoPasswordChangeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MercadoRepository mercadoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private Mercado testMercado;
    private String testMercadoId;
    private static final String SENHA_ORIGINAL = "SenhaOriginal123";
    private static final String SENHA_NOVA = "SenhaNovaSegura456";

    @BeforeEach
    void setup() {
        mercadoRepository.deleteAll();

        testMercado = new Mercado();
        testMercado.setNome("Mercado Teste");
        testMercado.setCnpj("12.345.678/0001-90");
        testMercado.setEndereco("Rua Teste, 123");
        testMercado.setTelefone("1133334444");
        testMercado.setEmail("mercado@example.com");
        testMercado.setSenha(passwordEncoder.encode(SENHA_ORIGINAL));
        testMercado.setImagemLogo("/uploads/logos/mercado.jpg");

        testMercado = mercadoRepository.save(testMercado);
        testMercadoId = testMercado.getId();
    }

    @Test
    @WithMockUser(username = "mercado@example.com")
    void testAlterarSenhaMercadoComSucesso() throws Exception {
        PasswordChangeRequest request = new PasswordChangeRequest(
                SENHA_ORIGINAL,
                SENHA_NOVA,
                SENHA_NOVA
        );

        mockMvc.perform(
                put("/mercados/{id}/senha", testMercadoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isNoContent())
                .andReturn();

        Mercado mercadoAtualizado = mercadoRepository.findById(testMercadoId).get();
        assertThat(passwordEncoder.matches(SENHA_NOVA, mercadoAtualizado.getSenha())).isTrue();
        assertThat(passwordEncoder.matches(SENHA_ORIGINAL, mercadoAtualizado.getSenha())).isFalse();
    }

    @Test
    @WithMockUser(username = "mercado@example.com")
    void testAlterarSenhaMercadoComSenhaAtualInvalida() throws Exception {
        PasswordChangeRequest request = new PasswordChangeRequest(
                "SenhaErrada",
                SENHA_NOVA,
                SENHA_NOVA
        );

        mockMvc.perform(
                put("/mercados/{id}/senha", testMercadoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isBadRequest());

        Mercado mercadoNaoAlterado = mercadoRepository.findById(testMercadoId).get();
        assertThat(passwordEncoder.matches(SENHA_ORIGINAL, mercadoNaoAlterado.getSenha())).isTrue();
    }

    @Test
    @WithMockUser(username = "mercado@example.com")
    void testAlterarSenhaMercadoComConfirmacaoNaoConferente() throws Exception {
        PasswordChangeRequest request = new PasswordChangeRequest(
                SENHA_ORIGINAL,
                SENHA_NOVA,
                "OutraSenha123"
        );

        mockMvc.perform(
                put("/mercados/{id}/senha", testMercadoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isBadRequest());

        Mercado mercadoNaoAlterado = mercadoRepository.findById(testMercadoId).get();
        assertThat(passwordEncoder.matches(SENHA_ORIGINAL, mercadoNaoAlterado.getSenha())).isTrue();
    }

    @Test
    @WithMockUser(username = "mercado@example.com")
    void testAlterarSenhaMercadoComMuitoCurta() throws Exception {
        PasswordChangeRequest request = new PasswordChangeRequest(
                SENHA_ORIGINAL,
                "123",
                "123"
        );

        mockMvc.perform(
                put("/mercados/{id}/senha", testMercadoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isBadRequest());

        Mercado mercadoNaoAlterado = mercadoRepository.findById(testMercadoId).get();
        assertThat(passwordEncoder.matches(SENHA_ORIGINAL, mercadoNaoAlterado.getSenha())).isTrue();
    }

    @Test
    @WithMockUser(username = "mercado@example.com")
    void testAlterarSenhaMercadoComIdInvalido() throws Exception {
        PasswordChangeRequest request = new PasswordChangeRequest(
                SENHA_ORIGINAL,
                SENHA_NOVA,
                SENHA_NOVA
        );

        mockMvc.perform(
                put("/mercados/{id}/senha", "idInvalido123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "mercado@example.com")
    void testAlterarSenhaMercadoComCamposVazios() throws Exception {
        PasswordChangeRequest request = new PasswordChangeRequest("", "", "");

        mockMvc.perform(
                put("/mercados/{id}/senha", testMercadoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isBadRequest());
    }
}
