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
    private MobiliaRepository mobiliaRepository;
    
    @Autowired
    private FaturaRepository faturaRepository;

    @GetMapping("/admin")
    public String mostrarPaginaAdmin(Model model) {
        List<Mobilia> mobilias = mobiliaRepository.findAll();
        
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
        
        model.addAttribute("mobilias", mobilias);
        model.addAttribute("totalVendas", totalVendas != null ? totalVendas : 0);
        model.addAttribute("receitaTotal", receitaTotal != null ? receitaTotal : 0.0);
        model.addAttribute("ticketMedio", ticketMedio != null ? ticketMedio : 0.0);
        model.addAttribute("clientesUnicos", clientesUnicos != null ? clientesUnicos : 0);
        model.addAttribute("produtoMaisVendido", produtoMaisVendido);
        model.addAttribute("quantidadeMaisVendida", quantidadeMaisVendida);
        
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
            
            return ResponseEntity.ok(dados);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> dadosVazios = new HashMap<>();
            dadosVazios.put("vendasPorMes", List.of());
            dadosVazios.put("produtosMaisVendidos", List.of());
            return ResponseEntity.ok(dadosVazios);
        }
    }
}