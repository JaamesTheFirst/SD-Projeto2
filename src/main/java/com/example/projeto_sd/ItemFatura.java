package com.example.projeto_sd;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "item_fatura")
public class ItemFatura {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fatura_id", nullable = false)
    @JsonBackReference
    private Fatura fatura;
    
    @Column(nullable = false)
    private String nomeVeiculo;

    @Column(nullable = false)
    private double precoUnitario;

    @Column(nullable = false)
    private int quantidade;

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Fatura getFatura() { return fatura; }
    public void setFatura(Fatura fatura) { this.fatura = fatura; }

    public String getNomeVeiculo() { return nomeVeiculo; }
    public void setNomeVeiculo(String nomeVeiculo) { this.nomeVeiculo = nomeVeiculo; }

    public double getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(double precoUnitario) { this.precoUnitario = precoUnitario; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public double getSubtotal() {
        return precoUnitario * quantidade;
    }
}