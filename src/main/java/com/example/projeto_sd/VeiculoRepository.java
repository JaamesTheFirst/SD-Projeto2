package com.example.projeto_sd;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    List<Veiculo> findByNomeContainingIgnoreCase(String nome);

    List<Veiculo> findByCategoriaId(Long categoriaId);

    @Query(value = "SELECT * FROM veiculo WHERE " +
           "(:nome IS NULL OR LOWER(nome) LIKE '%' || LOWER(:nome) || '%') AND " +
           "(:precoMin IS NULL OR preco >= :precoMin) AND " +
           "(:precoMax IS NULL OR preco <= :precoMax) AND " +
           "(:categoriaId IS NULL OR categoria_id = :categoriaId) AND " +
           "(:emStock IS NULL OR (:emStock = TRUE AND quantidade > 0) OR :emStock = FALSE)",
           nativeQuery = true)
    List<Veiculo> findByFiltros(@Param("nome") String nome,
                                @Param("precoMin") Double precoMin,
                                @Param("precoMax") Double precoMax,
                                @Param("categoriaId") Long categoriaId,
                                @Param("emStock") Boolean emStock);
}
