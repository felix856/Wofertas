package com.example.demo.service;

import com.example.demo.model.Curtida;
import com.example.demo.repository.CurtidaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CurtidaService {

    @Autowired private CurtidaRepository curtidaRepository;

    public void curtir(String idOferta, String idUsuario) {
        if (!curtidaRepository.existsByIdOfertaAndIdUsuario(idOferta, idUsuario)) {
            Curtida curtida = new Curtida(idOferta, idUsuario);
            curtidaRepository.save(curtida);
        }
    }

    public void descurtir(String idOferta, String idUsuario) {
        curtidaRepository.deleteByIdOfertaAndIdUsuario(idOferta, idUsuario);
    }

    public boolean isCurtido(String idOferta, String idUsuario) {
        return curtidaRepository.existsByIdOfertaAndIdUsuario(idOferta, idUsuario);
    }

    public long contagemCurtidas(String idOferta) {
        return curtidaRepository.countByIdOferta(idOferta);
    }

    public List<Curtida> listarCurtidasPorOferta(String idOferta) {
        return curtidaRepository.findByIdOferta(idOferta);
    }

    public List<Curtida> listarCurtidasPorUsuario(String idUsuario) {
        return curtidaRepository.findByIdUsuario(idUsuario);
    }
}
