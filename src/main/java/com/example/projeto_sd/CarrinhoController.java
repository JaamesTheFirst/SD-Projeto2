package com.example.projeto_sd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/cliente/carrinho")
public class CarrinhoController {

    @Autowired
    private CarrinhoRepository carrinhoRepository;
    
    @Autowired
    private VeiculoRepository veiculoRepository;
    
    @Autowired
    private FaturaRepository faturaRepository;
    
    @Autowired
    private ItemFaturaRepository itemFaturaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @GetMapping
    public String verCarrinho(Authentication auth, Model model) {
        String clienteEmail = auth.getName();
        List<CarrinhoItem> itens = carrinhoRepository.findByClienteEmail(clienteEmail);
        
        double total = itens.stream().mapToDouble(CarrinhoItem::getSubtotal).sum();
        
        model.addAttribute("itensCarrinho", itens);
        model.addAttribute("totalCarrinho", total);
        return "carrinho";
    }

    @PostMapping("/adicionar")
    public String adicionarAoCarrinho(@RequestParam Long veiculoId,
                                    Authentication auth,
                                    RedirectAttributes redirectAttributes) {
        String clienteEmail = auth.getName();

        Optional<Veiculo> veiculoOpt = veiculoRepository.findById(veiculoId);
        if (veiculoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Veículo não encontrado.");
            return "redirect:/cliente";
        }

        Veiculo veiculo = veiculoOpt.get();
        if (veiculo.getQuantidade() <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Veículo esgotado.");
            return "redirect:/cliente";
        }

        Optional<CarrinhoItem> itemExistente = carrinhoRepository.findByClienteEmailAndVeiculoId(clienteEmail, veiculoId);

        if (itemExistente.isPresent()) {
            CarrinhoItem item = itemExistente.get();
            if (item.getQuantidade() < veiculo.getQuantidade()) {
                item.setQuantidade(item.getQuantidade() + 1);
                carrinhoRepository.save(item);
                redirectAttributes.addFlashAttribute("successMessage", "Quantidade atualizada no carrinho!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Quantidade em stock insuficiente.");
            }
        } else {
            CarrinhoItem novoItem = new CarrinhoItem();
            novoItem.setClienteEmail(clienteEmail);
            novoItem.setVeiculo(veiculo);
            novoItem.setQuantidade(1);
            carrinhoRepository.save(novoItem);
            redirectAttributes.addFlashAttribute("successMessage", "Veículo adicionado ao carrinho!");
        }

        return "redirect:/cliente";
    }

    @PostMapping("/remover/{id}")
    public String removerDoCarrinho(@PathVariable Long id, 
                                  Authentication auth, 
                                  RedirectAttributes redirectAttributes) {
        String clienteEmail = auth.getName();
        Optional<CarrinhoItem> itemOpt = carrinhoRepository.findById(id);
        
        if (itemOpt.isPresent() && itemOpt.get().getClienteEmail().equals(clienteEmail)) {
            carrinhoRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Item removido do carrinho!");
        }
        
        return "redirect:/cliente/carrinho";
    }

    @PostMapping("/finalizar")
    public String finalizarCompra(Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            String clienteEmail = auth.getName();
            List<CarrinhoItem> itens = carrinhoRepository.findByClienteEmail(clienteEmail);
            
            if (itens.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Carrinho vazio!");
                return "redirect:/cliente/carrinho";
            }
            
            // Verificar se há stock suficiente
            for (CarrinhoItem item : itens) {
                if (item.getVeiculo().getQuantidade() < item.getQuantidade()) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                        "Stock insuficiente para " + item.getVeiculo().getNome());
                    return "redirect:/cliente/carrinho";
                }
            }

            // Criar fatura
            Fatura fatura = new Fatura();
            fatura.setClienteEmail(clienteEmail);
            fatura.setDataCompra(LocalDateTime.now());

            double total = 0;
            for (CarrinhoItem item : itens) {
                // Reduzir stock
                Veiculo veiculo = item.getVeiculo();
                veiculo.setQuantidade(veiculo.getQuantidade() - item.getQuantidade());
                veiculoRepository.save(veiculo);

                total += item.getSubtotal();
            }

            fatura.setTotalPago(total);
            fatura = faturaRepository.save(fatura);

            // Criar itens da fatura
            for (CarrinhoItem item : itens) {
                ItemFatura itemFatura = new ItemFatura();
                itemFatura.setFatura(fatura);
                itemFatura.setNomeVeiculo(item.getVeiculo().getNome());
                itemFatura.setPrecoUnitario(item.getVeiculo().getPreco());
                itemFatura.setQuantidade(item.getQuantidade());
                itemFaturaRepository.save(itemFatura);
            }
            
            // Limpar carrinho SEMPRE após criar a fatura
            carrinhoRepository.deleteByClienteEmail(clienteEmail);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Compra finalizada com sucesso! Total: €" + String.format("%.2f", total));
            
            // Redirecionar para a página principal do cliente, não para o carrinho
            return "redirect:/cliente";
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erro ao processar a compra. Tente novamente.");
            return "redirect:/cliente/carrinho";
        }
    }

    // Mostrar página de checkout
    @GetMapping("/checkout")
    public String mostrarCheckout(Authentication auth, Model model) {
        String clienteEmail = auth.getName();
        List<CarrinhoItem> itens = carrinhoRepository.findByClienteEmail(clienteEmail);
        if (itens.isEmpty()) return "redirect:/cliente/carrinho";
        double total = itens.stream().mapToDouble(CarrinhoItem::getSubtotal).sum();
        model.addAttribute("itensCarrinho", itens);
        model.addAttribute("totalCarrinho", total);
        return "checkout";
    }

    // Processar checkout
    @PostMapping("/checkout")
    public String processarCheckout(
            @RequestParam String morada,
            @RequestParam String tipoPagamento,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        try {
            String clienteEmail = auth.getName();
            List<CarrinhoItem> itens = carrinhoRepository.findByClienteEmail(clienteEmail);

            if (itens.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Carrinho vazio!");
                return "redirect:/cliente/carrinho";
            }

            for (CarrinhoItem item : itens) {
                if (item.getVeiculo().getQuantidade() < item.getQuantidade()) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                            "Stock insuficiente para " + item.getVeiculo().getNome());
                    return "redirect:/cliente/carrinho";
                }
            }

            Fatura fatura = new Fatura();
            fatura.setClienteEmail(clienteEmail);
            fatura.setDataCompra(LocalDateTime.now());
            fatura.setMorada(morada);
            fatura.setTipoPagamento(tipoPagamento);

            double total = 0;
            for (CarrinhoItem item : itens) {
                Veiculo veiculo = item.getVeiculo();
                veiculo.setQuantidade(veiculo.getQuantidade() - item.getQuantidade());
                veiculoRepository.save(veiculo);
                total += item.getSubtotal();
            }

            fatura.setTotalPago(total);
            fatura = faturaRepository.save(fatura);

            for (CarrinhoItem item : itens) {
                ItemFatura itemFatura = new ItemFatura();
                itemFatura.setFatura(fatura);
                itemFatura.setNomeVeiculo(item.getVeiculo().getNome());
                itemFatura.setPrecoUnitario(item.getVeiculo().getPreco());
                itemFatura.setQuantidade(item.getQuantidade());
                itemFaturaRepository.save(itemFatura);
            }

            carrinhoRepository.deleteByClienteEmail(clienteEmail);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Encomenda realizada! Total: €" + String.format("%.2f", total));
            redirectAttributes.addFlashAttribute("faturaId", fatura.getId());
            return "redirect:/cliente/carrinho/confirmacao/" + fatura.getId();

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao processar. Tente novamente.");
            return "redirect:/cliente/carrinho";
        }
    }

    // Página de confirmação
    @GetMapping("/confirmacao/{faturaId}")
    public String paginaConfirmacao(@PathVariable Long faturaId, Authentication auth, Model model) {
        String clienteEmail = auth.getName();
        Optional<Fatura> faturaOpt = faturaRepository.findById(faturaId);
        if (faturaOpt.isEmpty() || !faturaOpt.get().getClienteEmail().equals(clienteEmail)) {
            return "redirect:/cliente";
        }
        model.addAttribute("fatura", faturaOpt.get());
        return "confirmacao";
    }

    // Download PDF da fatura
    @GetMapping("/fatura/{faturaId}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long faturaId, Authentication auth) {
        try {
            String clienteEmail = auth.getName();
            Optional<Fatura> faturaOpt = faturaRepository.findById(faturaId);
            if (faturaOpt.isEmpty() || !faturaOpt.get().getClienteEmail().equals(clienteEmail)) {
                return ResponseEntity.status(403).build();
            }

            Fatura fatura = faturaOpt.get();
            List<ItemFatura> itens = fatura.getItens();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);

            doc.add(new Paragraph("FATURA / ENCOMENDA").setBold().setFontSize(20));
            doc.add(new Paragraph("AutoUBI").setFontSize(12));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Nº Encomenda: #" + fatura.getId()));
            doc.add(new Paragraph("Data: " + fatura.getDataCompra().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
            doc.add(new Paragraph("Cliente: " + fatura.getClienteEmail()));
            doc.add(new Paragraph("Morada: " + fatura.getMorada()));
            doc.add(new Paragraph("Pagamento: " + fatura.getTipoPagamento()));
            doc.add(new Paragraph(" "));

            Table table = new Table(UnitValue.createPercentArray(new float[]{50, 20, 15, 15}));
            table.setWidth(UnitValue.createPercentValue(100));
            table.addHeaderCell(new Cell().add(new Paragraph("Produto").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Preço Unit.").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Qtd").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Subtotal").setBold()));

            for (ItemFatura item : itens) {
                table.addCell(item.getNomeVeiculo());
                table.addCell(String.format("€%.2f", item.getPrecoUnitario()));
                table.addCell(String.valueOf(item.getQuantidade()));
                table.addCell(String.format("€%.2f", item.getPrecoUnitario() * item.getQuantidade()));
            }

            doc.add(table);
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("TOTAL: €" + String.format("%.2f", fatura.getTotalPago())).setBold().setFontSize(14));
            doc.close();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fatura-" + faturaId + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(baos.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/encomendas")
    public String verEncomendas(Authentication auth, Model model) {
        String clienteEmail = auth.getName();
        List<Fatura> encomendas = faturaRepository.findByClienteEmailWithItens(clienteEmail);
        model.addAttribute("encomendas", encomendas);
        return "encomendas";
    }
}