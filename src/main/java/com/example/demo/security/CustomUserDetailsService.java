package com.example.demo.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.demo.model.Mercado;
import com.example.demo.model.Usuario;
import com.example.demo.repository.MercadoRepository;
import com.example.demo.repository.UsuarioRepository;

/**
 * Carrega o usuário (ou mercado) pelo email para validação do JWT.
 * Procura primeiro em "usuarios", depois em "mercados".
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private MercadoRepository mercadoRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Usuario u = usuarioRepository.findByEmail(email);
        if (u != null) {
            return new CustomUserDetails(u.getId(), u.getEmail(), u.getSenha(), "USUARIO");
        }

        Mercado m = mercadoRepository.findByEmail(email);
        if (m != null) {
            return new CustomUserDetails(m.getId(), m.getEmail(), m.getSenha(), "MERCADO");
        }

        throw new UsernameNotFoundException("Nenhum usuário/mercado com email: " + email);
    }
}