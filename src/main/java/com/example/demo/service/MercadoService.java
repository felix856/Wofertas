package com.example.demo.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.MercadoUpdateDTO;
import com.example.demo.model.Mercado;
import com.example.demo.repository.MercadoRepository;

@Service
public class MercadoService {

    private static final Logger logger = LoggerFactory.getLogger(MercadoService.class);
    private static final String UPLOAD_DIR = "uploads/logos/";

    private final MercadoRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final GeocodingService geocodingService;

    public MercadoService(MercadoRepository repository,
                          PasswordEncoder passwordEncoder,
                          GeocodingService geocodingService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.geocodingService = geocodingService;
    }

    public Mercado criar(Mercado mercado) {
        logger.info("Criando mercado: {}", mercado.getNome());
        if (mercado.getSenha() != null && !mercado.getSenha().startsWith("$2")) {
            mercado.setSenha(passwordEncoder.encode(mercado.getSenha()));
        }
        preencherCoordenadasSePossivel(mercado);
        return repository.save(mercado);
    }

    public List<Mercado> listar() {
        logger.debug("Listando mercados");
        return repository.findAll().stream()
                .map(this::garantirCoordenadas)
                .toList();
    }

    public List<Mercado> listarProximos(double latitude, double longitude, double raioKm) {
        validarCoordenada(latitude, longitude);
        double raioSeguro = raioKm > 0 ? raioKm : 10.0;

        return listar().stream()
                .filter(mercado -> mercado.getLatitude() != null && mercado.getLongitude() != null)
                .filter(mercado -> calcularDistanciaKm(
                        latitude,
                        longitude,
                        mercado.getLatitude(),
                        mercado.getLongitude()
                ) <= raioSeguro)
                .sorted((a, b) -> Double.compare(
                        calcularDistanciaKm(latitude, longitude, a.getLatitude(), a.getLongitude()),
                        calcularDistanciaKm(latitude, longitude, b.getLatitude(), b.getLongitude())
                ))
                .toList();
    }

    public Mercado buscarPorId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ID nao pode ser nulo ou vazio");
        }

        Mercado mercado = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mercado nao encontrado"));
        return garantirCoordenadas(mercado);
    }

    public Mercado atualizar(String id, MercadoUpdateDTO dto) {
        Mercado existente = buscarPorId(id);

        if (dto.getNome() != null) existente.setNome(dto.getNome());
        if (dto.getCnpj() != null) existente.setCnpj(dto.getCnpj());
        if (dto.getEndereco() != null) existente.setEndereco(dto.getEndereco());
        if (dto.getTelefone() != null) existente.setTelefone(dto.getTelefone());
        if (dto.getEmail() != null) existente.setEmail(dto.getEmail());
        if (dto.getImagemLogo() != null) existente.setImagemLogo(dto.getImagemLogo());
        if (dto.getLatitude() != null) existente.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != null) existente.setLongitude(dto.getLongitude());
        if ((dto.getLatitude() == null || dto.getLongitude() == null) && dto.getEndereco() != null) {
            preencherCoordenadasSePossivel(existente);
        }

        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            if (dto.getSenha().length() < 6) {
                throw new IllegalArgumentException("Senha curta demais");
            }
            existente.setSenha(passwordEncoder.encode(dto.getSenha()));
        }

        return repository.save(existente);
    }

    public void deletar(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ID invalido");
        }

        logger.warn("Deletando mercado: {}", id);

        if (!repository.existsById(id)) {
            throw new RuntimeException("Mercado nao encontrado");
        }

        repository.deleteById(id);
    }

    public Mercado salvarLogo(String id, MultipartFile file) {
        Mercado mercado = buscarPorId(id);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo de logo nao pode estar vazio");
        }

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            mercado.setImagemLogo("/uploads/logos/" + fileName);
            return repository.save(mercado);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar logo", e);
        }
    }

    private void preencherCoordenadasSePossivel(Mercado mercado) {
        if (mercado.getLatitude() != null && mercado.getLongitude() != null) {
            return;
        }
        geocodingService.geocode(mercado.getEndereco()).ifPresent(coords -> {
            mercado.setLatitude(coords.latitude());
            mercado.setLongitude(coords.longitude());
        });
    }

    private Mercado garantirCoordenadas(Mercado mercado) {
        if (mercado.getLatitude() != null && mercado.getLongitude() != null) {
            return mercado;
        }
        preencherCoordenadasSePossivel(mercado);
        if (mercado.getLatitude() != null && mercado.getLongitude() != null) {
            return repository.save(mercado);
        }
        return mercado;
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

    /**
     * Altera a senha do mercado após validar a senha atual
     * @param id ID do mercado
     * @param senhaAtual Senha atual para validação
     * @param novaSenha Nova senha
     * @param confirmacao Confirmação da nova senha
     * @return Mercado com senha atualizada
     */
    public Mercado alterarSenha(String id, String senhaAtual, String novaSenha, String confirmacao) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ID do mercado não pode ser nulo");
        }

        if (!novaSenha.equals(confirmacao)) {
            throw new IllegalArgumentException("Nova senha e confirmação não conferem");
        }

        if (novaSenha.length() < 6) {
            throw new IllegalArgumentException("Senha deve ter no mínimo 6 caracteres");
        }

        Mercado m = buscarPorId(id);

        if (!passwordEncoder.matches(senhaAtual, m.getSenha())) {
            throw new IllegalArgumentException("Senha atual está incorreta");
        }

        m.setSenha(passwordEncoder.encode(novaSenha));
        logger.info("Senha alterada para mercado: {}", id);
        return repository.save(m);
    }
}
