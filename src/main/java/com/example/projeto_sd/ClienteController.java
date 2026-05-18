package com.example.projeto_sd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ClienteController {

    @Autowired
    private MobiliaRepository mobiliaRepository;
    
    @Autowired
    private FaturaRepository faturaRepository;

    @GetMapping("/cliente")
    public String exibirCatalogoCliente(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Double precoMin,
            @RequestParam(required = false) Double precoMax,
            @RequestParam(required = false) Boolean emStock,
            Model model) {
        
        List<Mobilia> lista;
        
        if (nome != null || precoMin != null || precoMax != null || emStock != null) {
            lista = mobiliaRepository.findByFiltros(nome, precoMin, precoMax, emStock);
        } else {
            lista = mobiliaRepository.findAll();
        }
        
        model.addAttribute("mobilias", lista);
        model.addAttribute("filtroNome", nome);
        model.addAttribute("filtroPrecoMin", precoMin);
        model.addAttribute("filtroPrecoMax", precoMax);
        model.addAttribute("filtroEmStock", emStock);
        
        return "clientes";
    }
    
    @GetMapping("/perfil")
    public String exibirPerfil(Authentication auth, Model model) {
        String clienteEmail = auth.getName();
        
        // Histórico de compras (sem carregar os itens aqui, serão carregados via AJAX)
        List<Fatura> faturas = faturaRepository.findByClienteEmailOrderByDataCompraDesc(clienteEmail);
        
        // Estatísticas básicas
        Double totalGasto = faturaRepository.getTotalGastoByCliente(clienteEmail);
        Long numeroCompras = faturaRepository.getNumeroComprasByCliente(clienteEmail);
        Double mediaGasto = faturaRepository.getMediaGastoByCliente(clienteEmail);
        
        model.addAttribute("clienteEmail", clienteEmail);
        model.addAttribute("faturas", faturas);
        model.addAttribute("totalGasto", totalGasto != null ? totalGasto : 0.0);
        model.addAttribute("numeroCompras", numeroCompras != null ? numeroCompras : 0);
        model.addAttribute("mediaGasto", mediaGasto != null ? mediaGasto : 0.0);
        
        return "perfil";
    }
    
    @GetMapping("/perfil/estatisticas-json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obterEstatisticas(Authentication auth) {
        try {
            String clienteEmail = auth.getName();
            
            Map<String, Object> dados = new HashMap<>();
            
            // Gastos por mês
            List<Object[]> gastosPorMes = faturaRepository.getGastosPorMes(clienteEmail);
            dados.put("gastosPorMes", gastosPorMes);
            
            // Produtos mais comprados
            List<Object[]> produtosMaisComprados = faturaRepository.getProdutosMaisComprados(clienteEmail);
            dados.put("produtosMaisComprados", produtosMaisComprados);
            
            return ResponseEntity.ok(dados);
        } catch (Exception e) {
            e.printStackTrace();
            // Retornar dados vazios em caso de erro
            Map<String, Object> dadosVazios = new HashMap<>();
            dadosVazios.put("gastosPorMes", List.of());
            dadosVazios.put("produtosMaisComprados", List.of());
            return ResponseEntity.ok(dadosVazios);
        }
    }
}