package com.example.projeto_sd;

import com.example.projeto_sd.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByEmail(String email);

    @Query("SELECT c FROM Cliente c WHERE LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(COALESCE(c.nome,'')) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY c.email ASC")
    List<Cliente> searchByEmailOrNome(@org.springframework.data.repository.query.Param("search") String search);

    List<Cliente> findAllByOrderByEmailAsc();
}