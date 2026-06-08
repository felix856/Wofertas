package com.example.demo.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * MUDANÇA: id era Long; agora é String (ObjectId MongoDB).
 */
public class CustomUserDetails implements UserDetails {

    private final String id;       // ObjectId (String) ← era Long
    private final String email;
    private final String password;
    private final String tipo;     // "USUARIO" ou "MERCADO"

    public CustomUserDetails(String id, String email, String password, String tipo) {
        this.id       = id;
        this.email    = email;
        this.password = password;
        this.tipo     = tipo;
    }

    public String getId()   { return id; }
    public String getTipo() { return tipo; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + tipo));
    }

    @Override public String  getPassword()                  { return password; }
    @Override public String  getUsername()                  { return email; }
    @Override public boolean isAccountNonExpired()          { return true; }
    @Override public boolean isAccountNonLocked()           { return true; }
    @Override public boolean isCredentialsNonExpired()      { return true; }
    @Override public boolean isEnabled()                    { return true; }
}