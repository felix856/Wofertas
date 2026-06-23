package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.model.ItemCarrinho;
import com.example.demo.repository.ItemCarrinhoRepository;

@Service
public class ItemCarrinhoService {

    private final ItemCarrinhoRepository itemCarrinhoRepository;
    private final AnalyticsEventService analyticsEventService;

    public ItemCarrinhoService(ItemCarrinhoRepository itemCarrinhoRepository,
                               AnalyticsEventService analyticsEventService) {
        this.itemCarrinhoRepository = itemCarrinhoRepository;
        this.analyticsEventService = analyticsEventService;
    }

    public ItemCarrinho adicionarAoCarrinho(String idUsuario, String idOferta, String nomeOferta, String mercadoId, int quantidade) {
        ItemCarrinho item = new ItemCarrinho(idUsuario, idOferta, nomeOferta, mercadoId, quantidade);
        ItemCarrinho salvo = itemCarrinhoRepository.save(item);
        registrarEventoCarrinho(idUsuario, idOferta);
        return salvo;
    }

    public List<ItemCarrinho> listarPorMercado(String mercadoId) {
        return itemCarrinhoRepository.findByMercadoId(mercadoId);
    }

    public List<ItemCarrinho> listarPorOferta(String idOferta) {
        return itemCarrinhoRepository.findByIdOferta(idOferta);
    }

    public List<ItemCarrinho> listarPorUsuario(String idUsuario) {
        return itemCarrinhoRepository.findByIdUsuario(idUsuario);
    }

    public ItemCarrinho buscarPorId(String id) {
        return itemCarrinhoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item do carrinho não encontrado"));
    }

    public ItemCarrinho atualizarQuantidade(String id, int quantidade, String idUsuario) {
        ItemCarrinho item = buscarPorId(id);
        if (!item.getIdUsuario().equals(idUsuario)) {
            throw new RuntimeException("Ação não permitida");
        }
        item.setQuantidade(quantidade);
        return itemCarrinhoRepository.save(item);
    }

    public void remover(String id, String idUsuario) {
        ItemCarrinho item = buscarPorId(id);
        if (!item.getIdUsuario().equals(idUsuario)) {
            throw new RuntimeException("Ação não permitida");
        }
        itemCarrinhoRepository.delete(item);
    }

    public long contagemItensCarrinho(String mercadoId) {
        return itemCarrinhoRepository.countByMercadoId(mercadoId);
    }

    public long contagemItensCarrinhoPorOferta(String idOferta) {
        return itemCarrinhoRepository.countByIdOferta(idOferta);
    }

    private void registrarEventoCarrinho(String idUsuario, String idOferta) {
        try {
            analyticsEventService.trackOfferEvent("cart_add", idOferta, idUsuario, "CARRINHO");
        } catch (RuntimeException ignored) {
            // Eventos de monetizacao/analytics nao devem bloquear o carrinho.
        }
    }
}
