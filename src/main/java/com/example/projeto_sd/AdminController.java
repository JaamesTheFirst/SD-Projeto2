package com.example.projeto_sd;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
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

    @GetMapping("/admin/fatura/{id}/pdf")
    @ResponseBody
    public ResponseEntity<byte[]> downloadFaturaPdfAdmin(@PathVariable Long id) {
        try {
            Optional<Fatura> faturaOpt = faturaRepository.findByIdWithItens(id);
            if (faturaOpt.isEmpty()) return ResponseEntity.notFound().build();
            Fatura fatura = faturaOpt.get();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24);
            Paragraph title = new Paragraph("AutoUBI", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 14, new java.awt.Color(80, 80, 80));
            Paragraph sub = new Paragraph("Fatura de Compra", subFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            doc.add(sub);
            doc.add(new Paragraph(" "));

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
            doc.add(new Paragraph("Nº Fatura: #" + fatura.getId(), infoFont));
            doc.add(new Paragraph("Cliente: " + fatura.getClienteEmail(), infoFont));
            doc.add(new Paragraph("Data: " + fatura.getDataCompra().format(fmt), infoFont));
            if (fatura.getMetodoPagamento() != null) {
                doc.add(new Paragraph("Método de Pagamento: " + fatura.getMetodoPagamento(), infoFont));
            }
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{50, 10, 20, 20});

            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            java.awt.Color headerBg = new java.awt.Color(200, 200, 200);
            for (String h : new String[]{"Veículo", "Qtd.", "Preço Unit.", "Subtotal"}) {
                PdfPCell cell = new PdfPCell(new Phrase(h, boldFont));
                cell.setBackgroundColor(headerBg);
                cell.setPadding(5);
                table.addCell(cell);
            }

            for (ItemFatura item : fatura.getItens()) {
                PdfPCell c1 = new PdfPCell(new Phrase(item.getNomeVeiculo(), infoFont));
                PdfPCell c2 = new PdfPCell(new Phrase(String.valueOf(item.getQuantidade()), infoFont));
                PdfPCell c3 = new PdfPCell(new Phrase(String.format("€%.2f", item.getPrecoUnitario()), infoFont));
                PdfPCell c4 = new PdfPCell(new Phrase(String.format("€%.2f", item.getSubtotal()), infoFont));
                for (PdfPCell c : new PdfPCell[]{c1, c2, c3, c4}) c.setPadding(4);
                table.addCell(c1);
                table.addCell(c2);
                table.addCell(c3);
                table.addCell(c4);
            }

            doc.add(table);
            doc.add(new Paragraph(" "));

            Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
            Paragraph total = new Paragraph(String.format("Total Pago: €%.2f", fatura.getTotalPago()), totalFont);
            total.setAlignment(Element.ALIGN_RIGHT);
            doc.add(total);

            doc.close();

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"fatura-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(baos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}