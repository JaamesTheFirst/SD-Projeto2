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
    private MobiliaRepository mobiliaRepository;
    
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
    public String adicionarAoCarrinho(@RequestParam Long mobiliaId, 
                                    Authentication auth, 
                                    RedirectAttributes redirectAttributes) {
        String clienteEmail = auth.getName();
        
        Optional<Mobilia> mobiliaOpt = mobiliaRepository.findById(mobiliaId);
        if (mobiliaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mobília não encontrada.");
            return "redirect:/cliente";
        }
        
        Mobilia mobilia = mobiliaOpt.get();
        if (mobilia.getQuantidade() <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mobília esgotada.");
            return "redirect:/cliente";
        }
        
        Optional<CarrinhoItem> itemExistente = carrinhoRepository.findByClienteEmailAndMobiliaId(clienteEmail, mobiliaId);
        
        if (itemExistente.isPresent()) {
            CarrinhoItem item = itemExistente.get();
            if (item.getQuantidade() < mobilia.getQuantidade()) {
                item.setQuantidade(item.getQuantidade() + 1);
                carrinhoRepository.save(item);
                redirectAttributes.addFlashAttribute("successMessage", "Quantidade atualizada no carrinho!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Quantidade em stock insuficiente.");
            }
        } else {
            CarrinhoItem novoItem = new CarrinhoItem();
            novoItem.setClienteEmail(clienteEmail);
            novoItem.setMobilia(mobilia);
            novoItem.setQuantidade(1);
            carrinhoRepository.save(novoItem);
            redirectAttributes.addFlashAttribute("successMessage", "Item adicionado ao carrinho!");
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
                if (item.getMobilia().getQuantidade() < item.getQuantidade()) {
                    redirectAttributes.addFlashAttribute("errorMessage", 
                        "Stock insuficiente para " + item.getMobilia().getNome());
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
                Mobilia mobilia = item.getMobilia();
                mobilia.setQuantidade(mobilia.getQuantidade() - item.getQuantidade());
                mobiliaRepository.save(mobilia);
                
                total += item.getSubtotal();
            }
            
            fatura.setTotalPago(total);
            fatura = faturaRepository.save(fatura);
            
            // Criar itens da fatura
            for (CarrinhoItem item : itens) {
                ItemFatura itemFatura = new ItemFatura();
                itemFatura.setFatura(fatura);
                itemFatura.setNomeMobilia(item.getMobilia().getNome());
                itemFatura.setPrecoUnitario(item.getMobilia().getPreco());
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
}