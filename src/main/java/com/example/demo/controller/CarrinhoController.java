package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.ItemCarrinhoDTO;
import com.example.demo.security.CustomUserDetails;
import com.example.demo.service.ItemCarrinhoService;
import com.example.demo.service.OfertaService;
import com.example.demo.dto.OfertaDTO;
import com.example.demo.model.ItemCarrinho;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/carrinho", "/api/carrinho"})
@CrossOrigin(origins = "*")
public class CarrinhoController {

    @Autowired private ItemCarrinhoService itemCarrinhoService;
    @Autowired private OfertaService ofertaService;

    @PostMapping("/adicionar")
    public ResponseEntity<ItemCarrinhoDTO> adicionarAoCarrinho(
            @RequestBody java.util.Map<String, Object> request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        String idOferta = String.valueOf(request.get("idOferta"));
        int quantidade = request.get("quantidade") instanceof Number number ? number.intValue() : 1;
        OfertaDTO oferta = ofertaService.buscarPorId(idOferta);

        ItemCarrinho item = itemCarrinhoService.adicionarAoCarrinho(
                principal.getId(),
                idOferta,
                oferta.getNome(),
                oferta.getMercado() != null ? oferta.getMercado().getId() : null,
                quantidade
        );

        return ResponseEntity.ok(toDTO(item));
    }

    @PostMapping("/adicionar/{idOferta}")
    public ResponseEntity<ItemCarrinhoDTO> adicionarAoCarrinhoPath(
            @PathVariable String idOferta,
            @RequestParam String nomeOferta,
            @RequestParam String mercadoId,
            @RequestParam(defaultValue = "1") int quantidade,
            @AuthenticationPrincipal CustomUserDetails principal) {

        ItemCarrinho item = itemCarrinhoService.adicionarAoCarrinho(
            principal.getId(),
            idOferta,
            nomeOferta,
            mercadoId,
            quantidade
        );

        return ResponseEntity.ok(toDTO(item));
    }

    @GetMapping("/count/{idOferta}")
    public long contagemItensCarrinho(@PathVariable String idOferta) {
        return itemCarrinhoService.contagemItensCarrinhoPorOferta(idOferta);
    }

    @GetMapping("/usuario")
    public List<ItemCarrinhoDTO> meuCarrinho(@AuthenticationPrincipal CustomUserDetails principal) {
        return itemCarrinhoService.listarPorUsuario(principal.getId())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemCarrinhoDTO> atualizarCarrinho(@PathVariable String id,
                                                              @RequestBody java.util.Map<String, Object> request,
                                                              @AuthenticationPrincipal CustomUserDetails principal) {
        int quantidade = request.get("quantidade") instanceof Number number ? number.intValue() : 1;
        return ResponseEntity.ok(toDTO(itemCarrinhoService.atualizarQuantidade(id, quantidade, principal.getId())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removerCarrinho(@PathVariable String id,
                                                @AuthenticationPrincipal CustomUserDetails principal) {
        itemCarrinhoService.remover(id, principal.getId());
        return ResponseEntity.ok().build();
    }

    // Converte o model para DTO com todos os campos que o mobile espera
    private ItemCarrinhoDTO toDTO(ItemCarrinho item) {
        return new ItemCarrinhoDTO(
            item.getId(),
            item.getIdUsuario(),
            item.getIdOferta(),
            item.getNomeOferta(),
            item.getMercadoId(),
            item.getQuantidade(),
            item.getDataAdicao()
        );
    }
}
