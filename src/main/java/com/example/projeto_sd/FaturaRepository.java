package com.example.projeto_sd;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FaturaRepository extends JpaRepository<Fatura, Long> {
    List<Fatura> findByClienteEmailOrderByDataCompraDesc(String clienteEmail);

    @Query("SELECT SUM(f.totalPago) FROM Fatura f WHERE f.clienteEmail = ?1")
    Double getTotalGastoByCliente(String clienteEmail);

    @Query("SELECT COUNT(f) FROM Fatura f WHERE f.clienteEmail = ?1")
    Long getNumeroComprasByCliente(String clienteEmail);

    @Query("SELECT AVG(f.totalPago) FROM Fatura f WHERE f.clienteEmail = ?1")
    Double getMediaGastoByCliente(String clienteEmail);

    @Query("SELECT YEAR(f.dataCompra) as ano, MONTH(f.dataCompra) as mes, SUM(f.totalPago) as total " +
           "FROM Fatura f WHERE f.clienteEmail = ?1 " +
           "GROUP BY YEAR(f.dataCompra), MONTH(f.dataCompra) " +
           "ORDER BY ano DESC, mes DESC")
    List<Object[]> getGastosPorMes(String clienteEmail);

    @Query("SELECT it.nomeMobilia, SUM(it.quantidade) as quantidade " +
           "FROM ItemFatura it JOIN it.fatura f WHERE f.clienteEmail = ?1 " +
           "GROUP BY it.nomeMobilia " +
           "ORDER BY quantidade DESC")
    List<Object[]> getProdutosMaisComprados(String clienteEmail);

    @Query("SELECT f FROM Fatura f LEFT JOIN FETCH f.itens WHERE f.id = :id")
    Optional<Fatura> findByIdWithItens(@Param("id") Long id);

    // ---- Estatísticas globais (Admin) ----

    @Query("SELECT it.nomeMobilia, SUM(it.quantidade) as quantidadeVendida, SUM(it.precoUnitario * it.quantidade) as totalVendas " +
           "FROM ItemFatura it " +
           "GROUP BY it.nomeMobilia " +
           "ORDER BY quantidadeVendida DESC")
    List<Object[]> getProdutosMaisVendidos();

    @Query("SELECT it.nomeMobilia, SUM(it.quantidade) as quantidadeVendida, SUM(it.precoUnitario * it.quantidade) as totalVendas " +
           "FROM ItemFatura it " +
           "GROUP BY it.nomeMobilia " +
           "ORDER BY quantidadeVendida ASC")
    List<Object[]> getProdutosMenosVendidos();

    @Query("SELECT COUNT(f) FROM Fatura f")
    Long getTotalVendas();

    @Query("SELECT SUM(f.totalPago) FROM Fatura f")
    Double getReceitaTotal();

    @Query("SELECT AVG(f.totalPago) FROM Fatura f")
    Double getTicketMedio();

    @Query("SELECT YEAR(f.dataCompra) as ano, MONTH(f.dataCompra) as mes, COUNT(f) as vendas " +
           "FROM Fatura f " +
           "GROUP BY YEAR(f.dataCompra), MONTH(f.dataCompra) " +
           "ORDER BY ano DESC, mes DESC")
    List<Object[]> getVendasPorMes();

    @Query("SELECT COUNT(DISTINCT f.clienteEmail) FROM Fatura f")
    Long getClientesUnicos();

    @Query("SELECT f.clienteEmail, COUNT(f) as totalCompras, SUM(f.totalPago) as totalGasto " +
           "FROM Fatura f " +
           "GROUP BY f.clienteEmail " +
           "ORDER BY totalGasto DESC")
    List<Object[]> getMelhoresClientes();

    @Query("SELECT YEAR(f.dataCompra) as ano, MONTH(f.dataCompra) as mes, SUM(f.totalPago) as receita " +
           "FROM Fatura f " +
           "GROUP BY YEAR(f.dataCompra), MONTH(f.dataCompra) " +
           "ORDER BY ano DESC, mes DESC")
    List<Object[]> getReceitaPorMes();
}