package com.example.demo.service;

import com.example.demo.dto.OfertaDTO;
import com.example.demo.dto.OfertaRequest;
import com.example.demo.model.Mercado;
import com.example.demo.model.Oferta;
import com.example.demo.repository.FavoritoRepository;
import com.example.demo.repository.OfertaRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class OfertaService {

    private final OfertaRepository ofertaRepository;
    private final MercadoService mercadoService;
    private final FavoritoRepository favoritoRepository;

    private static final String UPLOAD_DIR = "uploads/ofertas/";

    public OfertaService(OfertaRepository ofertaRepository,
                         MercadoService mercadoService,
                         FavoritoRepository favoritoRepository) {
        this.ofertaRepository = ofertaRepository;
        this.mercadoService = mercadoService;
        this.favoritoRepository = favoritoRepository;
    }

    public OfertaDTO criar(OfertaRequest dto, String mercadoIdLogado) {

        validarId(mercadoIdLogado);

        Mercado mercado = mercadoService.buscarPorId(mercadoIdLogado);

        Oferta oferta = new Oferta();
        oferta.setNome(dto.getNome());
        oferta.setStatus(dto.getStatus());
        oferta.setImagemOferta(dto.getImagemOferta());
        oferta.setDataFim(dto.getData());
        oferta.setMercadoId(mercado.getId());
        oferta.setMercadoNome(mercado.getNome());
        oferta.setMercadoLogo(mercado.getImagemLogo());

        return toDTO(ofertaRepository.save(oferta), mercado);
    }

    public List<OfertaDTO> listar() {
        return listar(null);
    }

    public List<OfertaDTO> listar(Boolean ativo) {
        return ofertaRepository.findAll().stream()
                .filter(o -> ativo == null || statusConfere(o.getStatus(), ativo))
                .map(o -> toDTO(o, mercadoDaOferta(o)))
                .toList();
    }

    public List<OfertaDTO> listarProximas(double latitude, double longitude, double raioKm, Boolean ativo) {
        validarCoordenada(latitude, longitude);
        double raioSeguro = raioKm > 0 ? raioKm : 10.0;

        return listar(ativo).stream()
                .filter(dto -> dto.getMercado() != null)
                .filter(dto -> dto.getMercado().getLatitude() != null && dto.getMercado().getLongitude() != null)
                .filter(dto -> calcularDistanciaKm(
                        latitude,
                        longitude,
                        dto.getMercado().getLatitude(),
                        dto.getMercado().getLongitude()
                ) <= raioSeguro)
                .sorted((a, b) -> Double.compare(
                        calcularDistanciaKm(latitude, longitude, a.getMercado().getLatitude(), a.getMercado().getLongitude()),
                        calcularDistanciaKm(latitude, longitude, b.getMercado().getLatitude(), b.getMercado().getLongitude())
                ))
                .toList();
    }

    public List<OfertaDTO> listarPorFavoritos(String idUsuario) {

        validarId(idUsuario);

        List<String> idsMercados = favoritoRepository.findByIdUsuario(idUsuario)
                .stream()
                .map(f -> f.getIdMercado())
                .toList();

        if (idsMercados.isEmpty()) return List.of();

        return ofertaRepository.findByMercadoIdIn(idsMercados).stream()
                .map(o -> toDTO(o, mercadoDaOferta(o)))
                .toList();
    }

    public List<OfertaDTO> listarPorMercado(String mercadoId) {

        validarId(mercadoId);

        Mercado mercado = buscarMercadoOuFallback(mercadoId, null, null);

        return ofertaRepository.findByMercadoId(mercadoId).stream()
                .map(o -> toDTO(o, mercado))
                .toList();
    }

    public OfertaDTO buscarPorId(String id) {

        validarId(id);

        Oferta o = ofertaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Oferta não encontrada"));

        Mercado m = mercadoDaOferta(o);

        return toDTO(o, m);
    }

    public void deletar(String id, String mercadoLogadoId) {

        validarId(id);
        validarId(mercadoLogadoId);

        Oferta o = ofertaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Oferta não encontrada"));

        validarDono(o, mercadoLogadoId);

        ofertaRepository.delete(o);
    }

    public OfertaDTO atualizar(String id, OfertaRequest dto, String mercadoLogadoId) {

        validarId(id);
        validarId(mercadoLogadoId);

        Oferta o = ofertaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Oferta não encontrada"));

        validarDono(o, mercadoLogadoId);

        o.setNome(dto.getNome());
        o.setStatus(dto.getStatus());

        if (dto.getImagemOferta() != null)
            o.setImagemOferta(dto.getImagemOferta());

        if (dto.getData() != null)
            o.setDataFim(dto.getData());

        Mercado m = mercadoDaOferta(o);

        return toDTO(ofertaRepository.save(o), m);
    }

    public OfertaDTO salvarImagem(String id, MultipartFile file) {

        validarId(id);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo inválido");
        }

        Oferta o = ofertaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Oferta não encontrada"));

        try {

            Path uploadPath = Paths.get(UPLOAD_DIR);

            if (Files.notExists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/uploads/ofertas/" + fileName;

            o.setImagemOferta(fileUrl);

            Oferta salva = ofertaRepository.save(o);

            return toDTO(salva, mercadoDaOferta(salva));

        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar imagem", e);
        }
    }

    // 🔥 MÉTODOS PRIVADOS (NÍVEL PRO)

    private void validarId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ID inválido");
        }
    }

    private void validarDono(Oferta oferta, String mercadoId) {
        if (!oferta.getMercadoId().equals(mercadoId)) {
            throw new RuntimeException("Ação não permitida");
        }
    }

    private Mercado mercadoDaOferta(Oferta oferta) {
        return buscarMercadoOuFallback(
                oferta.getMercadoId(),
                oferta.getMercadoNome(),
                oferta.getMercadoLogo()
        );
    }

    private Mercado buscarMercadoOuFallback(String mercadoId, String mercadoNome, String mercadoLogo) {
        try {
            return mercadoService.buscarPorId(mercadoId);
        } catch (RuntimeException e) {
            Mercado fallback = new Mercado();
            fallback.setId(mercadoId);
            fallback.setNome(mercadoNome != null && !mercadoNome.isBlank() ? mercadoNome : "Mercado");
            fallback.setImagemLogo(mercadoLogo);
            return fallback;
        }
    }

    private boolean statusConfere(String status, boolean ativo) {
        boolean ofertaAtiva = status == null || "ATIVO".equalsIgnoreCase(status);
        return ativo == ofertaAtiva;
    }

    private void validarCoordenada(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude invalida");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude invalida");
        }
    }

    private double calcularDistanciaKm(double latOrigem, double lonOrigem, double latDestino, double lonDestino) {
        final double raioTerraKm = 6371.0;
        double dLat = Math.toRadians(latDestino - latOrigem);
        double dLon = Math.toRadians(lonDestino - lonOrigem);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(latOrigem))
                * Math.cos(Math.toRadians(latDestino))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return raioTerraKm * c;
    }

    private OfertaDTO toDTO(Oferta o, Mercado m) {

        OfertaDTO dto = new OfertaDTO();

        dto.setId(Objects.requireNonNullElse(o.getId(), ""));
        dto.setNome(Objects.requireNonNullElse(o.getNome(), "Oferta"));
        dto.setStatus(o.getStatus());
        dto.setData(o.getDataFim() != null ? o.getDataFim().toString() : null);
        dto.setImagemOferta(o.getImagemOferta());

        OfertaDTO.MercadoResumoDTO mr = new OfertaDTO.MercadoResumoDTO();

        mr.setId(Objects.requireNonNullElse(m.getId(), ""));
        mr.setNome(Objects.requireNonNullElse(m.getNome(), "Mercado"));
        mr.setCnpj(m.getCnpj());
        mr.setEndereco(m.getEndereco());
        mr.setImagemLogo(m.getImagemLogo());
        mr.setEmail(m.getEmail());
        mr.setLatitude(m.getLatitude());
        mr.setLongitude(m.getLongitude());

        dto.setMercado(mr);

        return dto;
    }
}
