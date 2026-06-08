// ─────────────────────────────────────────────────────────────────────────────
// UsuarioDTO.java  – usado em POST /usuarios e GET /usuarios/{id}
// ─────────────────────────────────────────────────────────────────────────────
package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UsuarioDTO {
    private String id;

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 255, message = "Nome deve ter entre 3 e 255 caracteres")
    private String nome;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, max = 128, message = "Senha deve ter entre 6 e 128 caracteres")
    private String senha;  // apenas no cadastro; ignorado nas respostas

    public UsuarioDTO() {}

    public String getId()               { return id; }
    public void   setId(String id)      { this.id = id; }
    public String getNome()             { return nome; }
    public void   setNome(String n)     { this.nome = n; }
    public String getEmail()            { return email; }
    public void   setEmail(String e)    { this.email = e; }
    public String getSenha()            { return senha; }
    public void   setSenha(String s)    { this.senha = s; }
}