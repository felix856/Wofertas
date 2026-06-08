package com.example.demo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.MercadoUpdateDTO;
import com.example.demo.model.Mercado;
import com.example.demo.model.Usuario;
import com.example.demo.repository.MercadoRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.security.CustomUserDetails;
import com.example.demo.service.MercadoService;
import com.example.demo.service.VisualizacaoService;

@RestController
public class AndroidCompatibilityController {

    private final UsuarioRepository usuarioRepository;
    private final MercadoService mercadoService;
    private final VisualizacaoService visualizacaoService;
    private final PasswordEncoder passwordEncoder;

    public AndroidCompatibilityController(UsuarioRepository usuarioRepository,
    MercadoRepository mercadoRepository,
    MercadoService mercadoService,
    VisualizacaoService visualizacaoService,
    PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.mercadoService = mercadoService;
        this.visualizacaoService = visualizacaoService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/usuario/perfil")
    public Usuario perfilUsuario(@AuthenticationPrincipal CustomUserDetails principal) {
        validarPrincipal(principal);
        return usuarioRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    @PutMapping("/usuario/atualizar")
    public Usuario atualizarUsuario(@AuthenticationPrincipal CustomUserDetails principal,
                                    @RequestBody Map<String, Object> request) {
        validarPrincipal(principal);
        Usuario usuario = usuarioRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (request.containsKey("nome")) usuario.setNome(asString(request.get("nome")));
        if (request.containsKey("email")) usuario.setEmail(asString(request.get("email")));
        if (request.containsKey("imagemPerfil")) usuario.setImagemPerfil(asString(request.get("imagemPerfil")));

        return usuarioRepository.save(usuario);
    }

    @PostMapping("/usuario/mudar-senha")
    public ResponseEntity<String> mudarSenhaUsuario(@AuthenticationPrincipal CustomUserDetails principal,
                                                    @RequestBody Map<String, Object> request) {
        validarPrincipal(principal);
        Usuario usuario = usuarioRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        String senhaAtual = asString(request.get("senhaAtual"));
        String novaSenha = asString(request.get("novaSenha"));
        if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            throw new RuntimeException("Senha atual inválida");
        }
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
        return ResponseEntity.ok("Senha alterada com sucesso");
    }

    @PostMapping("/usuarios/fcm-token")
    public ResponseEntity<Void> registrarFcmToken(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mercado/cadastro")
    public ResponseEntity<Mercado> cadastroMercado(@RequestBody Map<String, Object> request) {
        Mercado mercado = new Mercado(
                asString(request.get("nome")),
                asString(request.get("cnpj")),
                asString(request.getOrDefault("endereco", "")),
                asString(request.get("telefone")),
                asString(request.get("email")),
                asString(request.get("senha")),
                asString(request.get("imagemLogo"))
        );
        return ResponseEntity.status(201).body(mercadoService.criar(mercado));
    }

    @GetMapping("/mercado/perfil")
    public Mercado perfilMercado(@AuthenticationPrincipal CustomUserDetails principal) {
        validarPrincipal(principal);
        return mercadoService.buscarPorId(principal.getId());
    }

    @PutMapping("/mercado/atualizar")
    public Mercado atualizarMercado(@AuthenticationPrincipal CustomUserDetails principal,
                                    @RequestBody Map<String, Object> request) {
        validarPrincipal(principal);
        MercadoUpdateDTO dto = new MercadoUpdateDTO();
        dto.setNome(asString(request.get("nome")));
        dto.setCnpj(asString(request.get("cnpj")));
        dto.setEndereco(asString(request.get("endereco")));
        dto.setEmail(asString(request.get("email")));
        dto.setSenha(asString(request.get("senha")));
        dto.setImagemLogo(asString(request.get("imagemLogo")));
        dto.setTelefone(asString(request.get("telefone")));
        dto.setLatitude(asDouble(request.get("latitude")));
        dto.setLongitude(asDouble(request.get("longitude")));
        return mercadoService.atualizar(principal.getId(), dto);
    }

    @GetMapping("/mercado/todas")
    public List<Mercado> listarMercadosPaginado(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return mercadoService.listar();
    }

    @PostMapping("/interacoes/{tipo}")
    public ResponseEntity<Void> registrarInteracao(@org.springframework.web.bind.annotation.PathVariable String tipo,
    @RequestParam String ofertaId,
    @RequestParam(required = false) String usuarioId,
    @RequestParam(defaultValue = "ANDROID") String origem) {
        visualizacaoService.registrarVisualizacao(ofertaId, usuarioId, tipo + ":" + origem);
        return ResponseEntity.ok().build();
    }

    private void validarPrincipal(CustomUserDetails principal) {
        if (principal == null) {
            throw new RuntimeException("Usuário não autenticado");
        }
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private Double asDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.doubleValue();
        try {
            return Double.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
