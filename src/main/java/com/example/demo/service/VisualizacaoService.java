package com.example.demo.service;

import com.example.demo.model.Visualizacao;
import com.example.demo.repository.VisualizacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VisualizacaoService {

    @Autowired private VisualizacaoRepository visualizacaoRepository;

    public Visualizacao registrarVisualizacao(String idOferta, String idUsuario, String origem) {
        Visualizacao viz = new Visualizacao(idOferta, idUsuario, origem);
        return visualizacaoRepository.save(viz);
    }

    public long contagemVisualizacoes(String idOferta) {
        return visualizacaoRepository.countByIdOferta(idOferta);
    }

    public List<Visualizacao> listarVisualizacoesPorOferta(String idOferta) {
        return visualizacaoRepository.findByIdOferta(idOferta);
    }

    public List<Visualizacao> listarVisualizacoesPorOrigem(String idOferta, String origem) {
        return visualizacaoRepository.findByIdOfertaAndOrigem(idOferta, origem);
    }
}
