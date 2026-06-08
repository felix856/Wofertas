package com.example.demo.dto;

/**
 * Resposta do POST /auth/login
 *
 * MUDANÇA: id era Long (JPA auto-increment); agora é String (ObjectId MongoDB).
 * O app Android deve salvar id como String e enviar nas chamadas autenticadas.
 */
public class AuthResponse {

    private String token;
    private String id;     // ObjectId do MongoDB  ← era Long
    private String tipo;   // "USUARIO" ou "MERCADO"
    private String email;

    public AuthResponse() {}

    public AuthResponse(String token, String id, String tipo, String email) {
        this.token = token;
        this.id    = id;
        this.tipo  = tipo;
        this.email = email;
    }

    public String getToken()          { return token; }
    public void   setToken(String t)  { this.token = t; }

    public String getId()             { return id; }
    public void   setId(String id)    { this.id = id; }

    public String getTipo()           { return tipo; }
    public void   setTipo(String t)   { this.tipo = t; }

    public String getEmail()          { return email; }
    public void   setEmail(String e)  { this.email = e; }
}