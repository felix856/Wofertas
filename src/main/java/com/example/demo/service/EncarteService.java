package com.example.demo.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.EncarteDTO;
import com.example.demo.model.Encarte;
import com.example.demo.repository.EncarteRepository;

@Service
public class EncarteService {

    private static final String UPLOAD_DIR = "uploads/encartes/";

    private final EncarteRepository encarteRepository;
    private final MercadoService mercadoService;

    public EncarteService(EncarteRepository encarteRepository, MercadoService mercadoService) {
        this.encarteRepository = encarteRepository;
        this.mercadoService = mercadoService;
    }

    public EncarteDTO salvar(String mercadoId, String titulo, MultipartFile pdf) {
        validarMercado(mercadoId);
        
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("Titulo do encarte e obrigatorio");
        }
        if (pdf == null || pdf.isEmpty()) {
            throw new IllegalArgumentException("Arquivo PDF e obrigatorio");
        }

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (Files.notExists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // O uso do Objects.requireNonNullElse resolve os alertas de forma limpa e nativa do Java
            String originalName = Objects.requireNonNullElse(pdf.getOriginalFilename(), "encarte.pdf");
            
            String safeOriginal = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
            String fileName = UUID.randomUUID() + "_" + safeOriginal;
            Path filePath = uploadPath.resolve(fileName);

            // Sem checagens redundantes de "pdf == null" aqui, pois já foi validado no topo do método
            Files.copy(pdf.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            Encarte encarte = new Encarte(
                    mercadoId,
                    titulo.trim(),
                    "/uploads/encartes/" + fileName,
                    originalName
            );

            return toDTO(encarteRepository.save(encarte));
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar encarte", e);
        }
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
        if (mercadoLogadoId != null && !mercadoLogadoId.isBlank()
                && !mercadoLogadoId.equals(encarte.getMercadoId())) {
            throw new RuntimeException("Acao nao permitida");
        }
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
}
