package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.security.CustomUserDetails;
import com.example.demo.service.CurtidaService;

@RestController
@RequestMapping({"/curtidas", "/api/curtidas"})
@CrossOrigin(origins = "*")
public class CurtidaController {

    @Autowired private CurtidaService curtidaService;

    @PostMapping("/toggle/{idOferta}")
    public String toggleCurtida(@PathVariable String idOferta,
                                 @AuthenticationPrincipal CustomUserDetails principal) {
        String userId = principal.getId();
        boolean isCurtido = curtidaService.isCurtido(idOferta, userId);
        
        if (isCurtido) {
            curtidaService.descurtir(idOferta, userId);
            return "DESCURTIDO";
        } else {
            curtidaService.curtir(idOferta, userId);
            return "CURTIDO";
        }
    }

    @GetMapping("/check/{idOferta}")
    public boolean isCurtida(@PathVariable String idOferta,
                              @AuthenticationPrincipal CustomUserDetails principal) {
        return curtidaService.isCurtido(idOferta, principal.getId());
    }

    @GetMapping("/verificar/{idOferta}")
    public boolean verificarCurtida(@PathVariable String idOferta,
                                    @AuthenticationPrincipal CustomUserDetails principal) {
        return curtidaService.isCurtido(idOferta, principal.getId());
    }

    @GetMapping("/usuario")
    public java.util.List<com.example.demo.model.Curtida> minhasCurtidas(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return curtidaService.listarCurtidasPorUsuario(principal.getId());
    }

    @GetMapping("/count/{idOferta}")
    public long contagemCurtidas(@PathVariable String idOferta) {
        return curtidaService.contagemCurtidas(idOferta);
    }
}
