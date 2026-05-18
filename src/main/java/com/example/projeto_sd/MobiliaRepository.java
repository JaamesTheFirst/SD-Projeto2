package com.example.projeto_sd;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MobiliaRepository extends JpaRepository<Mobilia, Long> {

    @Query("SELECT m FROM Mobilia m WHERE m.preco BETWEEN :min AND :max")
    List<Mobilia> findByPrecoBetween(Double min, Double max);

    List<Mobilia> findByNomeContainingIgnoreCase(String nome);

    List<Mobilia> findByCategoriaId(Long categoriaId);

    @Query("SELECT m FROM Mobilia m WHERE " +
           "(:nome IS NULL OR LOWER(m.nome) LIKE LOWER(CONCAT('%', :nome, '%'))) AND " +
           "(:precoMin IS NULL OR m.preco >= :precoMin) AND " +
           "(:precoMax IS NULL OR m.preco <= :precoMax) AND " +
           "(:categoriaId IS NULL OR m.categoria.id = :categoriaId) AND " +
           "(:emStock IS NULL OR (:emStock = true AND m.quantidade > 0) OR (:emStock = false))")
    List<Mobilia> findByFiltros(@Param("nome") String nome,
                               @Param("precoMin") Double precoMin,
                               @Param("precoMax") Double precoMax,
                               @Param("categoriaId") Long categoriaId,
                               @Param("emStock") Boolean emStock);
}