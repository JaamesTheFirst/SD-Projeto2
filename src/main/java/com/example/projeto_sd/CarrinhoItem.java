package com.example.projeto_sd;

import jakarta.persistence.*;

@Entity
@Table(name = "carrinho_item")
public class CarrinhoItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String clienteEmail;
    
    @ManyToOne
    @JoinColumn(name = "mobilia_id", nullable = false)
    private Mobilia mobilia;
    
    @Column(nullable = false)
    private int quantidade = 1;
    
    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getClienteEmail() { return clienteEmail; }
    public void setClienteEmail(String clienteEmail) { this.clienteEmail = clienteEmail; }
    
    public Mobilia getMobilia() { return mobilia; }
    public void setMobilia(Mobilia mobilia) { this.mobilia = mobilia; }
    
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
    
    public double getSubtotal() {
        return mobilia.getPreco() * quantidade;
    }
}