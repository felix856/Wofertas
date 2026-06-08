package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para alterar senha de usuários e mercados.
 * Requer validação de senha atual.
 */
public class PasswordChangeRequest {
    
    @NotBlank(message = "Senha atual é obrigatória")
    private String senhaAtual;
    
    @NotBlank(message = "Nova senha é obrigatória")
    @Size(min = 6, max = 128, message = "Senha deve ter entre 6 e 128 caracteres")
    private String novaSenha;
    
    @NotBlank(message = "Confirmação de senha é obrigatória")
    @Size(min = 6, max = 128, message = "Confirmação deve ter entre 6 e 128 caracteres")
    private String confirmacao;
    
    public PasswordChangeRequest() {}
    
    public PasswordChangeRequest(String senhaAtual, String novaSenha, String confirmacao) {
        this.senhaAtual = senhaAtual;
        this.novaSenha = novaSenha;
        this.confirmacao = confirmacao;
    }
    
    public String getSenhaAtual() { 
        return senhaAtual; 
    }
    
    public void setSenhaAtual(String senhaAtual) { 
        this.senhaAtual = senhaAtual; 
    }
    
    public String getNovaSenha() { 
        return novaSenha; 
    }
    
    public void setNovaSenha(String novaSenha) { 
        this.novaSenha = novaSenha; 
    }
    
    public String getConfirmacao() { 
        return confirmacao; 
    }
    
    public void setConfirmacao(String confirmacao) { 
        this.confirmacao = confirmacao; 
    }
}
