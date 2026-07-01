package com.example.demo.integration;

import com.example.demo.model.Encarte;
import com.example.demo.repository.EncarteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class EncarteControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EncarteRepository encarteRepository;

    @MockBean private JavaMailSender mailSender;

    @BeforeEach
    void setup() {
        encarteRepository.deleteAll();

        Encarte antigo = new Encarte("mercado-1", "Encarte antigo", "/uploads/encartes/antigo.pdf", "antigo.pdf");
        antigo.setDataCriacao(LocalDateTime.now().minusDays(1));
        encarteRepository.save(antigo);

        Encarte recente = new Encarte("mercado-1", "Encarte recente", "/uploads/encartes/recente.pdf", "recente.pdf");
        recente.setDataCriacao(LocalDateTime.now());
        encarteRepository.save(recente);
    }

    @Test
    void listarTodosEncartesRetornaListaPublicaOrdenada() throws Exception {
        mockMvc.perform(get("/api/encartes").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Encarte recente"))
                .andExpect(jsonPath("$[1].titulo").value("Encarte antigo"));
    }
}
