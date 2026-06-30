package com.example.demo.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.AnalyticsEventRequest;
import com.example.demo.dto.EncarteDTO;
import com.example.demo.model.Encarte;
import com.example.demo.repository.EncarteRepository;

@Service
public class EncarteService {

    private static final String UPLOAD_DIR = "uploads/encartes/";

    private final EncarteRepository encarteRepository;
    private final MercadoService mercadoService;
    private final UsageService usageService;
    private final AnalyticsEventService analyticsEventService;

    public EncarteService(EncarteRepository encarteRepository,
                          MercadoService mercadoService,
                          UsageService usageService,
                          AnalyticsEventService analyticsEventService) {
        this.encarteRepository = encarteRepository;
        this.mercadoService = mercadoService;
        this.usageService = usageService;
        this.analyticsEventService = analyticsEventService;
    }

    public EncarteDTO salvar(String mercadoId, String titulo, MultipartFile pdf) {
        validarMercado(mercadoId);
        validarTitulo(titulo);
        if (pdf == null || pdf.isEmpty()) {
            throw new IllegalArgumentException("Arquivo PDF e obrigatorio");
        }

        try {
            ArquivoEncarte arquivo = salvarArquivo(pdf);
            Encarte encarte = new Encarte(
                    mercadoId,
                    titulo.trim(),
                    arquivo.url(),
                    arquivo.nomeOriginal()
            );

            Encarte salvo = encarteRepository.save(encarte);
            registrarCriacaoEncarte(salvo);

            return toDTO(salvo);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar encarte", e);
        }
    }

    public EncarteDTO atualizar(String id, String titulo, MultipartFile pdf, String mercadoLogadoId) {
        validarTitulo(titulo);
        Encarte encarte = buscarEntidade(id);
        validarDono(encarte, mercadoLogadoId);

        encarte.setTitulo(titulo.trim());

        if (pdf != null && !pdf.isEmpty()) {
            try {
                ArquivoEncarte arquivo = salvarArquivo(pdf);
                encarte.setUrlPdf(arquivo.url());
                encarte.setNomeArquivoOriginal(arquivo.nomeOriginal());
            } catch (IOException e) {
                throw new RuntimeException("Erro ao atualizar arquivo do encarte", e);
            }
        }

        Encarte salvo = encarteRepository.save(encarte);
        registrarAtualizacaoEncarte(salvo);
        return toDTO(salvo);
    }

    public List<EncarteDTO> listarPorMercado(String mercadoId) {
        validarMercado(mercadoId);
        return encarteRepository.findByMercadoIdOrderByDataCriacaoDesc(mercadoId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public EncarteDTO buscarPorId(String id) {
        return toDTO(buscarEntidade(id));
    }

    public void deletar(String id, String mercadoLogadoId) {
        Encarte encarte = buscarEntidade(id);
        validarDono(encarte, mercadoLogadoId);
        encarteRepository.delete(encarte);
    }

    private Encarte buscarEntidade(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ID do encarte invalido");
        }
        return encarteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Encarte nao encontrado"));
    }

    private void validarMercado(String mercadoId) {
        if (mercadoId == null || mercadoId.isBlank()) {
            throw new IllegalArgumentException("ID do mercado e obrigatorio");
        }
        mercadoService.buscarPorId(mercadoId);
    }

    private void validarTitulo(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("Titulo do encarte e obrigatorio");
        }
    }

    private void validarDono(Encarte encarte, String mercadoLogadoId) {
        if (mercadoLogadoId == null || mercadoLogadoId.isBlank()) {
            throw new RuntimeException("Permissao negada para gerenciar encartes");
        }
        if (!mercadoLogadoId.equals(encarte.getMercadoId())) {
            throw new RuntimeException("Acao nao permitida");
        }
    }

    private ArquivoEncarte salvarArquivo(MultipartFile pdf) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (Files.notExists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalName = Objects.requireNonNullElse(pdf.getOriginalFilename(), "encarte.pdf");
        String safeOriginal = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String fileName = UUID.randomUUID() + "_" + safeOriginal;
        Path filePath = uploadPath.resolve(fileName);

        Files.copy(pdf.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return new ArquivoEncarte("/uploads/encartes/" + fileName, originalName);
    }

    private void registrarCriacaoEncarte(Encarte encarte) {
        try {
            usageService.recordFlyerCreated(encarte.getMercadoId());
            analyticsEventService.track(new AnalyticsEventRequest(
                    "flyer_created",
                    encarte.getMercadoId(),
                    encarte.getMercadoId(),
                    null,
                    encarte.getId(),
                    Map.of("titulo", encarte.getTitulo())
            ));
        } catch (RuntimeException ignored) {
            // Monetizacao/analytics ficam em observacao e nunca impedem o upload do encarte.
        }
    }

    private void registrarAtualizacaoEncarte(Encarte encarte) {
        try {
            analyticsEventService.track(new AnalyticsEventRequest(
                    "flyer_updated",
                    encarte.getMercadoId(),
                    encarte.getMercadoId(),
                    null,
                    encarte.getId(),
                    Map.of("titulo", encarte.getTitulo())
            ));
        } catch (RuntimeException ignored) {
            // Analytics nao pode impedir a atualizacao do encarte.
        }
    }

    private EncarteDTO toDTO(Encarte encarte) {
        String data = encarte.getDataCriacao() != null ? encarte.getDataCriacao().toString() : "";
        return new EncarteDTO(
                encarte.getId(),
                encarte.getMercadoId(),
                encarte.getTitulo(),
                encarte.getUrlPdf(),
                data
        );
    }

    private record ArquivoEncarte(String url, String nomeOriginal) {}
}
