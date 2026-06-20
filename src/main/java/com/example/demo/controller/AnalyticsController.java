package com.example.demo.controller;

import com.example.demo.dto.DashboardAnalyticsDTO;
import com.example.demo.dto.MercadoRankingDTO;
import java.util.List;
import com.example.demo.model.Mercado;
import com.example.demo.repository.MercadoRepository;
import com.example.demo.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/analytics", "/api/analytics"})
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AnalyticsController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);

    @Autowired 
    private AnalyticsService service;

    @Autowired
    private MercadoRepository mercadoRepository;

    /**
     * Retorna os dados de comportamento dos CLIENTES para o mercado logado.
     * Foco: Visualizações, Curtidas e Intenções de compra (RPA/Analytics).
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardAnalyticsDTO> getDashboard(Authentication auth) {
        try {
            // 1. Verificação de segurança: O usuário está autenticado?
            if (auth == null || !auth.isAuthenticated()) {
                logger.warn("Tentativa de acesso ao dashboard sem autenticação.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 2. O filtro de segurança coloca o e-mail no Name. Buscamos o ID real do mercado.
            String emailLogado = auth.getName();
            Mercado mercado = mercadoRepository.findByEmail(emailLogado);

            // 3. PROTEÇÃO: Se o mercado não existir no banco, retornamos 404 para o JS parar o loading
            if (mercado == null) {
                logger.error("Dashboard: Mercado não encontrado para o e-mail: {}", emailLogado);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // 4. Chamamos o serviço que processa as interações dos CLIENTES
            logger.info("Gerando análise de clientes para o mercado: {}", mercado.getNome());
            DashboardAnalyticsDTO dto = service.gerarDashboardMercado(mercado.getId());

            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            // Log detalhado no console do VS Code para você debugar
            logger.error("ERRO CRÍTICO no Dashboard: ", e);
            
            // Retornamos 500 para que o Front-end saiba que o servidor falhou e pare o spinner
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/ranking-mercados")
    public ResponseEntity<List<MercadoRankingDTO>> obterRankingMercados() {
        try {
            logger.info("Buscando ranking geral de competitividade de supermercados...");
            List<MercadoRankingDTO> ranking = service.obterRankingMercados();
            return ResponseEntity.ok(ranking);
        } catch (Exception e) {
            logger.error("Erro ao gerar ranking de mercados: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
