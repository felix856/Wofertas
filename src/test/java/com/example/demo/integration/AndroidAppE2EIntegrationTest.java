package com.example.demo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.example.demo.dto.PasswordChangeRequest;
import com.example.demo.model.Mercado;
import com.example.demo.model.Usuario;
import com.example.demo.repository.MercadoRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.security.JwtUtil;
/**
 * E2E Tests Simulando Requisições do Android App
 * Testa a integração completa entre App e Backend
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class AndroidAppE2EIntegrationTest {

@Autowired
private TestRestTemplate restTemplate;

@Autowired
private UsuarioRepository usuarioRepository;

@Autowired
private MercadoRepository mercadoRepository;

@Autowired
private PasswordEncoder passwordEncoder;

@Autowired
private JwtUtil jwtUtil;

private Usuario testUsuario;
private Mercado testMercado;
private static final String SENHA_ORIGINAL = "SenhaApp123";
private static final String SENHA_NOVA = "NovaSenhaApp456";

private ResponseEntity<Void> putSenha(String url, PasswordChangeRequest request, String id) {
        boolean mercado = url.startsWith("/mercados");
        String tipo = mercado ? "MERCADO" : "USUARIO";
        String email = mercado ? testMercado.getEmail() : testUsuario.getEmail();
        String token = jwtUtil.generateToken(id, tipo, email);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        return restTemplate.exchange(
                url,
                HttpMethod.PUT,
                new HttpEntity<>(request, headers),
                Void.class,
                id
);
}

@BeforeEach
public void setup() {
        usuarioRepository.deleteAll();
        mercadoRepository.deleteAll();

        // Criar usuário de teste
        testUsuario = new Usuario();
        testUsuario.setNome("App User");
        testUsuario.setEmail("appuser@example.com");
        testUsuario.setSenha(passwordEncoder.encode(SENHA_ORIGINAL));
        testUsuario.setImagemPerfil("/uploads/perfis/app-user.jpg");
        testUsuario = usuarioRepository.save(testUsuario);

        // Criar mercado de teste
        testMercado = new Mercado();
        testMercado.setNome("App Mercado");
        testMercado.setCnpj("98.765.432/0001-12");
        testMercado.setEndereco("Rua App, 456");
        testMercado.setTelefone("1144445555");
        testMercado.setEmail("appmercado@example.com");
        testMercado.setSenha(passwordEncoder.encode(SENHA_ORIGINAL));
        testMercado.setImagemLogo("/uploads/logos/app-mercado.jpg");
        testMercado = mercadoRepository.save(testMercado);
}

@Test
@WithMockUser(username = "appuser@example.com")
void testFluxoCompletoMudancaSenhaUsuarioAndroid() {
        // 1. Usuário celular faz requisição de mudança de senha
        PasswordChangeRequest changeRequest = new PasswordChangeRequest(
                SENHA_ORIGINAL,
                SENHA_NOVA,
                SENHA_NOVA
        );

        ResponseEntity<Void> response = putSenha(
                "/usuarios/{id}/senha",
                changeRequest,
                testUsuario.getId()
        );

        // 2. Verificar resposta do backend
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 3. Verificar que a senha foi atualizada no banco
        Usuario usuarioAtualizado = usuarioRepository.findById(testUsuario.getId()).get();
        assertThat(passwordEncoder.matches(SENHA_NOVA, usuarioAtualizado.getSenha())).isTrue();

        // 4. Tentar fazer login com senha antiga (deve falhar)
        PasswordChangeRequest oldPasswordAttempt = new PasswordChangeRequest(
                SENHA_ORIGINAL,
                "OutraSenha",
                "OutraSenha"
        );

        ResponseEntity<Void> failResponse = putSenha(
                "/usuarios/{id}/senha",
                oldPasswordAttempt,
                testUsuario.getId()
        );

        assertThat(failResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
}

@Test
@WithMockUser(username = "appmercado@example.com")
void testFluxoCompletoMudancaSenhaMercadoAndroid() {
        // 1. Mercado celular faz requisição de mudança de senha
        PasswordChangeRequest changeRequest = new PasswordChangeRequest(
                SENHA_ORIGINAL,
                SENHA_NOVA,
                SENHA_NOVA
        );

        ResponseEntity<Void> response = putSenha(
                "/mercados/{id}/senha",
                changeRequest,
                testMercado.getId()
        );

        // 2. Verificar resposta do backend
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 3. Verificar que a senha foi atualizada no banco
        Mercado mercadoAtualizado = mercadoRepository.findById(testMercado.getId()).get();
        assertThat(passwordEncoder.matches(SENHA_NOVA, mercadoAtualizado.getSenha())).isTrue();

        // 4. Tentar fazer login com senha antiga (deve falhar)
        PasswordChangeRequest oldPasswordAttempt = new PasswordChangeRequest(
                SENHA_ORIGINAL,
                "OutraSenha",
                "OutraSenha"
        );

        ResponseEntity<Void> failResponse = putSenha(
                "/mercados/{id}/senha",
                oldPasswordAttempt,
                testMercado.getId()
        );

        assertThat(failResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
}

@Test
@WithMockUser(username = "appuser@example.com")
void testCenarioMobilidadeAndroid() {
        // Simula: App em celular muda de rede, precisa alterar senha
        // 1. Primeira tentativa (conectado à rede 1)
        PasswordChangeRequest request1 = new PasswordChangeRequest(
                SENHA_ORIGINAL,
                "SenhaRede1",
                "SenhaRede1"
        );

        ResponseEntity<Void> response1 = putSenha(
                "/usuarios/{id}/senha",
                request1,
                testUsuario.getId()
        );
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 2. Segunda tentativa após mudança de rede
        PasswordChangeRequest request2 = new PasswordChangeRequest(
                "SenhaRede1",
                "SenhaRede2Final",
                "SenhaRede2Final"
        );

        ResponseEntity<Void> response2 = putSenha(
                "/usuarios/{id}/senha",
                request2,
                testUsuario.getId()
        );
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 3. Verificar que a senha final é a correta
        Usuario usuarioFinal = usuarioRepository.findById(testUsuario.getId()).get();
        assertThat(passwordEncoder.matches("SenhaRede2Final", usuarioFinal.getSenha())).isTrue();
        assertThat(passwordEncoder.matches(SENHA_ORIGINAL, usuarioFinal.getSenha())).isFalse();}

@Test
@WithMockUser(username = "appuser@example.com")
void testErrosComunsDoAppAndroid() {
        // Teste 1: Confirmação não confere
        PasswordChangeRequest invalidRequest = new PasswordChangeRequest(
                SENHA_ORIGINAL,
                SENHA_NOVA,
                "ConfirmacaoDiferente"
        );

        ResponseEntity<Void> response = putSenha(
                "/usuarios/{id}/senha",
                invalidRequest,
                testUsuario.getId()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
}
}
