package com.example.projeto_sd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String irParaCheckout(Authentication auth, RedirectAttributes redirectAttributes) {
        String clienteEmail = auth.getName();
        List<CarrinhoItem> itens = carrinhoRepository.findByClienteEmail(clienteEmail);
        if (itens.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Carrinho vazio!");
            return "redirect:/cliente/carrinho";
        }
        return "redirect:/cliente/carrinho/checkout";
    }

    @GetMapping("/checkout")
    public String mostrarCheckout(Authentication auth, Model model) {
        String clienteEmail = auth.getName();
        List<CarrinhoItem> itens = carrinhoRepository.findByClienteEmail(clienteEmail);
        if (itens.isEmpty()) {
            return "redirect:/cliente/carrinho";
        }
        double total = itens.stream().mapToDouble(CarrinhoItem::getSubtotal).sum();
        model.addAttribute("itensCarrinho", itens);
        model.addAttribute("totalCarrinho", total);
        return "checkout";
    }

    @PostMapping("/checkout")
    public String confirmarCompra(@RequestParam String metodoPagamento,
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
            fatura.setMetodoPagamento(metodoPagamento);

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

            redirectAttributes.addFlashAttribute("faturaId", fatura.getId());
            redirectAttributes.addFlashAttribute("successMessage",
                "Compra finalizada com sucesso! Total: €" + String.format("%.2f", total));
            return "redirect:/cliente/carrinho/confirmacao";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao processar a compra. Tente novamente.");
            return "redirect:/cliente/carrinho";
        }
    }

    @GetMapping("/confirmacao")
    public String mostrarConfirmacao(@ModelAttribute("faturaId") Long faturaId,
                                     @ModelAttribute("successMessage") String successMessage,
                                     Model model) {
        if (faturaId == null) {
            return "redirect:/cliente";
        }
        model.addAttribute("faturaId", faturaId);
        model.addAttribute("successMessage", successMessage);
        return "confirmacao";
    }
}