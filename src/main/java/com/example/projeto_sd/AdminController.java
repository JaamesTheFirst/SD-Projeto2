package com.example.projeto_sd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class AdminController {

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private FaturaRepository faturaRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @GetMapping("/admin")
    public String mostrarPaginaAdmin(Model model) {
        List<Veiculo> veiculos = veiculoRepository.findAll();
        List<Categoria> categorias = categoriaRepository.findAll();

        // Estatísticas gerais
        Long totalVendas = faturaRepository.getTotalVendas();
        Double receitaTotal = faturaRepository.getReceitaTotal();
        Double ticketMedio = faturaRepository.getTicketMedio();
        Long clientesUnicos = faturaRepository.getClientesUnicos();

        // Produto mais vendido
        List<Object[]> produtosMaisVendidos = faturaRepository.getProdutosMaisVendidos();
        String produtoMaisVendido = "Nenhum";
        Long quantidadeMaisVendida = 0L;

        if (!produtosMaisVendidos.isEmpty()) {
            Object[] primeiro = produtosMaisVendidos.get(0);
            produtoMaisVendido = (String) primeiro[0];
            quantidadeMaisVendida = ((Number) primeiro[1]).longValue();
        }

        // Produto menos vendido
        List<Object[]> produtosMenosVendidos = faturaRepository.getProdutosMenosVendidos();
        String produtoMenosVendido = "Nenhum";
        Long quantidadeMenosVendida = 0L;

        if (!produtosMenosVendidos.isEmpty()) {
            Object[] ultimo = produtosMenosVendidos.get(0);
            produtoMenosVendido = (String) ultimo[0];
            quantidadeMenosVendida = ((Number) ultimo[1]).longValue();
        }

        // Melhores clientes
        List<Object[]> melhoresClientes = faturaRepository.getMelhoresClientes();

        model.addAttribute("veiculos", veiculos);
        model.addAttribute("categorias", categorias);
        model.addAttribute("totalVendas", totalVendas != null ? totalVendas : 0);
        model.addAttribute("receitaTotal", receitaTotal != null ? receitaTotal : 0.0);
        model.addAttribute("ticketMedio", ticketMedio != null ? ticketMedio : 0.0);
        model.addAttribute("clientesUnicos", clientesUnicos != null ? clientesUnicos : 0);
        model.addAttribute("produtoMaisVendido", produtoMaisVendido);
        model.addAttribute("quantidadeMaisVendida", quantidadeMaisVendida);
        model.addAttribute("produtoMenosVendido", produtoMenosVendido);
        model.addAttribute("quantidadeMenosVendida", quantidadeMenosVendida);
        model.addAttribute("melhoresClientes", melhoresClientes);

        return "admin";
    }

    @GetMapping("/admin/estatisticas-json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obterEstatisticasAdmin() {
        try {
            Map<String, Object> dados = new HashMap<>();

            // Vendas por mês
            List<Object[]> vendasPorMes = faturaRepository.getVendasPorMes();
            dados.put("vendasPorMes", vendasPorMes);

            // Produtos mais vendidos
            List<Object[]> produtosMaisVendidos = faturaRepository.getProdutosMaisVendidos();
            dados.put("produtosMaisVendidos", produtosMaisVendidos);

            // Melhores clientes
            List<Object[]> melhoresClientes = faturaRepository.getMelhoresClientes();
            dados.put("melhoresClientes", melhoresClientes);

            // Receita por mês (valor faturado)
            List<Object[]> receitaPorMes = faturaRepository.getReceitaPorMes();
            dados.put("receitaPorMes", receitaPorMes);

            return ResponseEntity.ok(dados);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> dadosVazios = new HashMap<>();
            dadosVazios.put("vendasPorMes", List.of());
            dadosVazios.put("produtosMaisVendidos", List.of());
            dadosVazios.put("melhoresClientes", List.of());
            dadosVazios.put("receitaPorMes", List.of());
            return ResponseEntity.ok(dadosVazios);
        }
    }
}