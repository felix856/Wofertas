package com.example.demo.integration;

import java.util.Map;

import com.example.demo.dto.AuthRequest;
import com.example.demo.dto.ResetPasswordRequest;
import com.example.demo.model.Mercado;
import com.example.demo.model.Usuario;
import com.example.demo.repository.MercadoRepository;
import com.example.demo.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class AuthFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private MercadoRepository mercadoRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private JavaMailSender mailSender;

    private static final String USER_EMAIL = "cliente.login@wofertas.test";
    private static final String MARKET_EMAIL = "mercado.login@wofertas.test";
    private static final String ORIGINAL_PASSWORD = "SenhaOriginal123";
    private static final String NEW_PASSWORD = "SenhaNova456";

    @BeforeEach
    void setup() {
        usuarioRepository.deleteAll();
        mercadoRepository.deleteAll();

        Usuario usuario = new Usuario();
        usuario.setNome("Cliente Login");
        usuario.setEmail(USER_EMAIL);
        usuario.setSenha(passwordEncoder.encode(ORIGINAL_PASSWORD));
        usuarioRepository.save(usuario);

        Mercado mercado = new Mercado();
        mercado.setNome("Mercado Login");
        mercado.setCnpj("12345678000199");
        mercado.setEndereco("Rua Login, 100");
        mercado.setTelefone("48999999999");
        mercado.setEmail(MARKET_EMAIL);
        mercado.setSenha(passwordEncoder.encode(ORIGINAL_PASSWORD));
        mercadoRepository.save(mercado);
    }

    @Test
    void loginUsuarioRetornaJwtETipoUsuario() throws Exception {
        AuthRequest request = authRequest(USER_EMAIL, ORIGINAL_PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value(USER_EMAIL))
                .andExpect(jsonPath("$.tipo").value("USUARIO"));
    }

    @Test
    void loginMercadoRetornaJwtETipoMercado() throws Exception {
        AuthRequest request = authRequest(MARKET_EMAIL, ORIGINAL_PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value(MARKET_EMAIL))
                .andExpect(jsonPath("$.tipo").value("MERCADO"));
    }

    @Test
    void cadastroUsuarioDuplicadoRetornaConflict() throws Exception {
        Map<String, String> request = Map.of(
                "nome", "Cliente Login",
                "email", USER_EMAIL,
                "senha", "SenhaNova123"
        );

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("cadastrado")));
    }

    @Test
    void recuperacaoSenhaAceitaJsonEEmailComMaiusculas() throws Exception {
        Map<String, String> request = Map.of("email", USER_EMAIL.toUpperCase());

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Usuario usuarioComToken = usuarioRepository.findByEmail(USER_EMAIL);
        assertThat(usuarioComToken.getResetToken()).hasSize(6);
        assertThat(usuarioComToken.getResetTokenExpiration()).isNotNull();
    }

    @Test
    void recuperacaoUsuarioGeraCodigoERedefineSenha() throws Exception {
        mockMvc.perform(post("/auth/forgot-password").param("email", USER_EMAIL))
                .andExpect(status().isOk());

        Usuario usuarioComToken = usuarioRepository.findByEmail(USER_EMAIL);
        assertThat(usuarioComToken.getResetToken()).hasSize(6);
        assertThat(usuarioComToken.getResetTokenExpiration()).isNotNull();

        ResetPasswordRequest request = new ResetPasswordRequest(
                USER_EMAIL,
                usuarioComToken.getResetToken(),
                NEW_PASSWORD
        );

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Usuario usuarioAtualizado = usuarioRepository.findByEmail(USER_EMAIL);
        assertThat(passwordEncoder.matches(NEW_PASSWORD, usuarioAtualizado.getSenha())).isTrue();
        assertThat(passwordEncoder.matches(ORIGINAL_PASSWORD, usuarioAtualizado.getSenha())).isFalse();
        assertThat(usuarioAtualizado.getResetToken()).isNull();
        assertThat(usuarioAtualizado.getResetTokenExpiration()).isNull();
    }

    @Test
    void recuperacaoMercadoAceitaFormularioUrlEncodedERedefineSenha() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content("email=" + MARKET_EMAIL))
                .andExpect(status().isOk());

        Mercado mercadoComToken = mercadoRepository.findByEmail(MARKET_EMAIL);
        assertThat(mercadoComToken.getResetToken()).hasSize(6);
        assertThat(mercadoComToken.getResetTokenExpiration()).isNotNull();

        ResetPasswordRequest request = new ResetPasswordRequest(
                MARKET_EMAIL,
                mercadoComToken.getResetToken(),
                NEW_PASSWORD
        );

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Mercado mercadoAtualizado = mercadoRepository.findByEmail(MARKET_EMAIL);
        assertThat(passwordEncoder.matches(NEW_PASSWORD, mercadoAtualizado.getSenha())).isTrue();
        assertThat(passwordEncoder.matches(ORIGINAL_PASSWORD, mercadoAtualizado.getSenha())).isFalse();
        assertThat(mercadoAtualizado.getResetToken()).isNull();
        assertThat(mercadoAtualizado.getResetTokenExpiration()).isNull();
    }

    @Test
    void resetPasswordRejeitaCodigoInvalido() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest(
                USER_EMAIL,
                "000000",
                NEW_PASSWORD
        );

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        Usuario usuario = usuarioRepository.findByEmail(USER_EMAIL);
        assertThat(passwordEncoder.matches(ORIGINAL_PASSWORD, usuario.getSenha())).isTrue();
    }

    @Test
    void loginRejeitaSenhaIncorreta() throws Exception {
        AuthRequest request = authRequest(USER_EMAIL, "senhaErrada123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    private AuthRequest authRequest(String email, String senha) {
        AuthRequest request = new AuthRequest();
        request.setEmail(email);
        request.setSenha(senha);
        return request;
    }
}
