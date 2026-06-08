// ─────────────────────────────────────────────────────────────────────────────
// AuthRequest.java
// ─────────────────────────────────────────────────────────────────────────────
package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthRequest {
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, max = 128, message = "Senha deve ter entre 6 e 128 caracteres")
    private String senha;

    public AuthRequest() {}
    public String getEmail()          { return email; }
    public void   setEmail(String e)  { this.email = e; }
    public String getSenha()          { return senha; }
    public void   setSenha(String s)  { this.senha = s; }
}