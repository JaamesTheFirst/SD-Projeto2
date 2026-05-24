package com.example.projeto_sd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class AdminController {

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private FaturaRepository faturaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

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

            // Receita por dia (últimos 30 dias)
            List<Object[]> receitaPorDia = faturaRepository.getReceitaPorDia();
            dados.put("receitaPorDia", receitaPorDia);

            // Receita por semana (últimas 52 semanas)
            List<Object[]> receitaPorSemana = faturaRepository.getReceitaPorSemana();
            dados.put("receitaPorSemana", receitaPorSemana);

            return ResponseEntity.ok(dados);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> dadosVazios = new HashMap<>();
            dadosVazios.put("vendasPorMes", List.of());
            dadosVazios.put("produtosMaisVendidos", List.of());
            dadosVazios.put("melhoresClientes", List.of());
            dadosVazios.put("receitaPorMes", List.of());
            dadosVazios.put("receitaPorDia", List.of());
            dadosVazios.put("receitaPorSemana", List.of());
            return ResponseEntity.ok(dadosVazios);
        }
    }


    //Endpoits a haver com os clientes
    @GetMapping("/admin/clientes-json")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> listarClientes(
            @RequestParam(required = false, defaultValue = "") String search) {
        List<Cliente> clientes = search.isBlank()
                ? clienteRepository.findAllByOrderByEmailAsc()
                : clienteRepository.searchByEmailOrNome(search);

        List<Map<String, Object>> result = clientes.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("email", c.getEmail());
            m.put("nome", c.getNome() != null ? c.getNome() : "");
            m.put("role", c.getRole());
            m.put("suspended", c.isSuspended());
            Long compras = faturaRepository.getNumeroComprasByCliente(c.getEmail());
            Double gasto = faturaRepository.getTotalGastoByCliente(c.getEmail());
            m.put("totalCompras", compras != null ? compras : 0);
            m.put("totalGasto", gasto != null ? gasto : 0.0);
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/admin/cliente/{email}/suspender")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> suspenderCliente(@PathVariable String email) {
        return clienteRepository.findByEmail(email).map(c -> {
            c.setSuspended(!c.isSuspended());
            clienteRepository.save(c);
            Map<String, Object> r = new HashMap<>();
            r.put("suspended", c.isSuspended());
            return ResponseEntity.ok(r);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/admin/cliente/{email}/remover")
    @ResponseBody
    public ResponseEntity<Void> removerCliente(@PathVariable String email) {
        clienteRepository.findByEmail(email).ifPresent(clienteRepository::delete);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/admin/compras-json")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> listarCompras(
            @RequestParam(required = false, defaultValue = "") String search) {
        List<Fatura> faturas = faturaRepository.findAllByOrderByDataCompraDesc();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        List<Map<String, Object>> result = faturas.stream()
                .filter(f -> search.isBlank() || f.getClienteEmail().toLowerCase().contains(search.toLowerCase()))
                .map(f -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", f.getId());
                    m.put("clienteEmail", f.getClienteEmail());
                    m.put("dataCompra", f.getDataCompra() != null ? f.getDataCompra().format(fmt) : "");
                    m.put("totalPago", f.getTotalPago());
                    m.put("metodoPagamento", f.getMetodoPagamento() != null ? f.getMetodoPagamento() : "");
                    m.put("estado", f.getEstado() != null ? f.getEstado() : "Pendente");
                    m.put("morada", f.getMorada() != null ? f.getMorada() : "");
                    return m;
                }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/admin/fatura/{id}/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> detalhesFatura(@PathVariable Long id) {
        return faturaRepository.findByIdWithItens(id).map(f -> {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", f.getId());
            m.put("clienteEmail", f.getClienteEmail());
            m.put("dataCompra", f.getDataCompra() != null ? f.getDataCompra().format(fmt) : "");
            m.put("totalPago", f.getTotalPago());
            m.put("metodoPagamento", f.getMetodoPagamento() != null ? f.getMetodoPagamento() : "");
            List<Map<String, Object>> itens = f.getItens() == null ? List.of() :
                f.getItens().stream().map(it -> {
                    Map<String, Object> i = new LinkedHashMap<>();
                    i.put("nome", it.getNomeVeiculo());
                    i.put("quantidade", it.getQuantidade());
                    i.put("precoUnitario", it.getPrecoUnitario());
                    i.put("subtotal", it.getSubtotal());
                    return i;
                }).collect(Collectors.toList());
            m.put("itens", itens);
            return ResponseEntity.ok(m);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/admin/fatura/{id}/estado")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> atualizarEstadoFatura(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return faturaRepository.findById(id).map(f -> {
            f.setEstado(body.get("estado"));
            faturaRepository.save(f);
            Map<String, Object> r = new HashMap<>();
            r.put("success", true);
            r.put("estado", f.getEstado());
            return ResponseEntity.ok(r);
        }).orElse(ResponseEntity.notFound().build());
    }
}