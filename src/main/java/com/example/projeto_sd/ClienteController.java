package com.example.projeto_sd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class ClienteController {

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private FaturaRepository faturaRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @GetMapping("/cliente")
    public String exibirCatalogoCliente(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Double precoMin,
            @RequestParam(required = false) Double precoMax,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Boolean emStock,
            Authentication auth,
            Model model) {

        List<Veiculo> lista;

        if (nome != null || precoMin != null || precoMax != null || categoriaId != null || emStock != null) {
            lista = veiculoRepository.findByFiltros(nome, precoMin, precoMax, categoriaId, emStock);
        } else {
            lista = veiculoRepository.findAll();
        }

        boolean isGuest = (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken);

        model.addAttribute("veiculos", lista);
        model.addAttribute("categorias", categoriaRepository.findAll());
        model.addAttribute("filtroNome", nome);
        model.addAttribute("filtroPrecoMin", precoMin);
        model.addAttribute("filtroPrecoMax", precoMax);
        model.addAttribute("filtroCategoriaId", categoriaId);
        model.addAttribute("filtroEmStock", emStock);
        model.addAttribute("isGuest", isGuest);

        return "clientes";
    }

    @GetMapping("/perfil")
    public String exibirPerfil(Authentication auth, Model model) {
        String clienteEmail = auth.getName();

        // Histórico de compras
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
            Map<String, Object> dadosVazios = new HashMap<>();
            dadosVazios.put("gastosPorMes", List.of());
            dadosVazios.put("produtosMaisComprados", List.of());
            return ResponseEntity.ok(dadosVazios);
        }
    }

    @GetMapping("/cliente/veiculo/{id}")
    public String exibirDetalheVeiculo(@PathVariable Long id, Model model,
                                       Authentication auth,
                                       RedirectAttributes redirectAttributes) {
        Optional<Veiculo> veiculoOpt = veiculoRepository.findById(id);
        if (veiculoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Veículo não encontrado.");
            return "redirect:/cliente";
        }
        Veiculo veiculo = veiculoOpt.get();
        model.addAttribute("veiculo", veiculo);

        boolean isGuest = (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken);
        model.addAttribute("isGuest", isGuest);

        if (veiculo.getSpecs() != null && !veiculo.getSpecs().isBlank()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                LinkedHashMap<String, String> specsMap = mapper.readValue(
                    veiculo.getSpecs(), new TypeReference<LinkedHashMap<String, String>>() {});
                model.addAttribute("specsMap", specsMap);
            } catch (Exception ignored) {}
        }

        return "veiculo_detalhe";
    }

    @GetMapping("/perfil/encomendas-json")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getMinhasEncomendas(Authentication auth) {
        String email = auth.getName();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        List<Map<String, Object>> result = faturaRepository
                .findByClienteEmailOrderByDataCompraDesc(email)
                .stream().map(f -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", f.getId());
                    m.put("dataCompra", f.getDataCompra() != null ? f.getDataCompra().format(fmt) : "");
                    m.put("totalPago", f.getTotalPago());
                    m.put("metodoPagamento", f.getMetodoPagamento() != null ? f.getMetodoPagamento() : "");
                    m.put("morada", f.getMorada() != null ? f.getMorada() : "");
                    m.put("estado", f.getEstado() != null ? f.getEstado() : "Pendente");
                    return m;
                }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(result);
    }
}