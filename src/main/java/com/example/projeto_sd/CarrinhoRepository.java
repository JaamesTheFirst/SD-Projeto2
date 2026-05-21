package com.example.projeto_sd;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CarrinhoRepository extends JpaRepository<CarrinhoItem, Long> {
    List<CarrinhoItem> findByClienteEmail(String clienteEmail);
    Optional<CarrinhoItem> findByClienteEmailAndVeiculoId(String clienteEmail, Long veiculoId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM CarrinhoItem c WHERE c.clienteEmail = ?1")
    void deleteByClienteEmail(String clienteEmail);
}