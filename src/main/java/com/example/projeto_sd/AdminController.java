package com.example.projeto_sd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Optional;
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

    @Autowired
    private ClienteRepository clienteRepository;

    @GetMapping("/admin/clientes")
    public String listarClientes(Model model) {
        model.addAttribute("clientes", clienteRepository.findAll());
        return "redirect:/admin"; // já carrega na tab
    }

    @PostMapping("/admin/clientes/toggle")
    public String toggleCliente(@RequestParam String email, RedirectAttributes redirectAttributes) {
        Optional<Cliente> opt = clienteRepository.findById(email);
        if (opt.isPresent()) {
            Cliente c = opt.get();
            c.setAtivo(!c.isAtivo());
            clienteRepository.save(c);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Conta " + email + (c.isAtivo() ? " ativada" : " desativada") + " com sucesso!");
        }
        return "redirect:/admin";
    }

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
        model.addAttribute("encomendas", faturaRepository.findAllByOrderByDataCompraDesc());
        model.addAttribute("clientes", clienteRepository.findAll());

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

    @GetMapping("/admin/encomendas")
    public String listarEncomendas(Model model) {
        model.addAttribute("encomendas", faturaRepository.findAllByOrderByDataCompraDesc());
        return "redirect:/admin";
    }

    @PostMapping("/admin/encomendas/estado")
    public String atualizarEstado(@RequestParam Long faturaId,
                                  @RequestParam String estado,
                                  RedirectAttributes redirectAttributes) {
        Optional<Fatura> opt = faturaRepository.findById(faturaId);
        if (opt.isPresent()) {
            Fatura f = opt.get();
            f.setEstado(estado);
            faturaRepository.save(f);
            redirectAttributes.addFlashAttribute("successMessage", "Estado atualizado com sucesso!");
        }
        return "redirect:/admin";
    }
}