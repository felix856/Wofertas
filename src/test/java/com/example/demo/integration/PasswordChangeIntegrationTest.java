package com.example.demo.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.example.demo.dto.PasswordChangeRequest;
import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2E Integration Tests para Password Change Endpoints
 * Simula requisições do Android app
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class PasswordChangeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private Usuario testUsuario;
    private String testUsuarioId;
    private static final String SENHA_ORIGINAL = "SenhaOriginal123";
    private static final String SENHA_NOVA = "SenhaNovaSegura456";

 @BeforeEach
public void setup() {
        usuarioRepository.deleteAll();

        testUsuario = new Usuario();
        testUsuario.setNome("Teste Usuario");
        testUsuario.setEmail("teste@example.com");
        testUsuario.setSenha(passwordEncoder.encode(SENHA_ORIGINAL));
        testUsuario.setImagemPerfil("/uploads/perfis/default.jpg");

        testUsuario = usuarioRepository.save(testUsuario);
        testUsuarioId = testUsuario.getId();
    }

   @Test
    @WithMockUser(username = "teste@example.com")
    void testAlterarSenhaComSucesso() throws Exception {
        PasswordChangeRequest request = new PasswordChangeRequest(
                SENHA_ORIGINAL,
                SENHA_NOVA,
                SENHA_NOVA
        );

        // Chamada direta sem criar a variável 'result'
        mockMvc.perform(
                put("/usuarios/{id}/senha", testUsuarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isNoContent());

        Usuario usuarioAtualizado = usuarioRepository.findById(testUsuarioId).get();
        assertThat(passwordEncoder.matches(SENHA_NOVA, usuarioAtualizado.getSenha())).isTrue();
        assertThat(passwordEncoder.matches(SENHA_ORIGINAL, usuarioAtualizado.getSenha())).isFalse();
    }

    @Test
    @WithMockUser(username = "teste@example.com")
    void testAlterarSenhaComSenhaAtualInvalida() throws Exception {
        PasswordChangeRequest request = new PasswordChangeRequest(
                "SenhaErrada",
                SENHA_NOVA,
                SENHA_NOVA
        );

        mockMvc.perform(
                put("/usuarios/{id}/senha", testUsuarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isBadRequest());

        Usuario usuarioNaoAlterado = usuarioRepository.findById(testUsuarioId).get();
        assertThat(passwordEncoder.matches(SENHA_ORIGINAL, usuarioNaoAlterado.getSenha())).isTrue();
    }

    @Test
    @WithMockUser(username = "teste@example.com")
    void testAlterarSenhaComConfirmacaoNaoConferente() throws Exception {
        PasswordChangeRequest request = new PasswordChangeRequest(
                SENHA_ORIGINAL,
                SENHA_NOVA,
                "OutraSenha123"
        );

        mockMvc.perform(
                put("/usuarios/{id}/senha", testUsuarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isBadRequest());

        Usuario usuarioNaoAlterado = usuarioRepository.findById(testUsuarioId).get();
        assertThat(passwordEncoder.matches(SENHA_ORIGINAL, usuarioNaoAlterado.getSenha())).isTrue();
    }

    @Test
    @WithMockUser(username = "teste@example.com")
    void testAlterarSenhaComMuitoCurta() throws Exception {
        PasswordChangeRequest request = new PasswordChangeRequest(
                SENHA_ORIGINAL,
                "123",
                "123"
        );

        mockMvc.perform(
                put("/usuarios/{id}/senha", testUsuarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isBadRequest());

        Usuario usuarioNaoAlterado = usuarioRepository.findById(testUsuarioId).get();
        assertThat(passwordEncoder.matches(SENHA_ORIGINAL, usuarioNaoAlterado.getSenha())).isTrue();
    }

    @Test
    @WithMockUser(username = "teste@example.com")
    void testAlterarSenhaComIdInvalido() throws Exception {
        PasswordChangeRequest request = new PasswordChangeRequest(
                SENHA_ORIGINAL,
                SENHA_NOVA,
                SENHA_NOVA
        );

        mockMvc.perform(
                put("/usuarios/{id}/senha", "idInvalido123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "teste@example.com")
    void testAlterarSenhaComCamposVazios() throws Exception {
        PasswordChangeRequest request = new PasswordChangeRequest("", "", "");

        mockMvc.perform(
                put("/usuarios/{id}/senha", testUsuarioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isBadRequest());
    }
}
