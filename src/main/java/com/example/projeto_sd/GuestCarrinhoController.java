package com.example.projeto_sd;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
public class GuestCarrinhoController {

    public static final String GUEST_CART = "guestCart";

    @Autowired
    private VeiculoRepository veiculoRepository;

    @PostMapping("/guest/carrinho/adicionar/ajax")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> adicionarAjax(
            @RequestParam Long veiculoId,
            HttpSession session) {

        Map<String, Object> result = new HashMap<>();

        Optional<Veiculo> veiculoOpt = veiculoRepository.findById(veiculoId);
        if (veiculoOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Veículo não encontrado.");
            return ResponseEntity.status(404).body(result);
        }

        Veiculo veiculo = veiculoOpt.get();
        if (veiculo.getQuantidade() <= 0) {
            result.put("success", false);
            result.put("message", "Veículo esgotado.");
            return ResponseEntity.ok(result);
        }

        Map<Long, Integer> cart = getGuestCart(session);
        int currentQty = cart.getOrDefault(veiculoId, 0);

        if (currentQty >= veiculo.getQuantidade()) {
            result.put("success", false);
            result.put("message", "Quantidade em stock insuficiente.");
            return ResponseEntity.ok(result);
        }

        cart.put(veiculoId, currentQty + 1);

        int totalItems = cart.values().stream().mapToInt(Integer::intValue).sum();
        result.put("success", true);
        result.put("message", "Veículo adicionado ao carrinho!");
        result.put("cartCount", totalItems);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/guest/carrinho/count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> count(HttpSession session) {
        Map<Long, Integer> cart = getGuestCart(session);
        int count = cart.values().stream().mapToInt(Integer::intValue).sum();
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/guest/carrinho")
    public String verCarrinho(HttpSession session, Model model) {
        Map<Long, Integer> cart = getGuestCart(session);

        List<GuestCartItem> items = new ArrayList<>();
        double total = 0;
        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
            veiculoRepository.findById(entry.getKey()).ifPresent(v -> {
                items.add(new GuestCartItem(v, entry.getValue()));
            });
            Optional<Veiculo> v = veiculoRepository.findById(entry.getKey());
            if (v.isPresent()) {
                total += v.get().getPreco() * entry.getValue();
            }
        }

        model.addAttribute("guestItems", items);
        model.addAttribute("totalCarrinho", total);
        return "guest_carrinho";
    }

    @PostMapping("/guest/carrinho/remover/{veiculoId}")
    public String remover(@PathVariable Long veiculoId,
                          HttpSession session,
                          RedirectAttributes ra) {
        Map<Long, Integer> cart = getGuestCart(session);
        cart.remove(veiculoId);
        ra.addFlashAttribute("successMessage", "Item removido do carrinho.");
        return "redirect:/guest/carrinho";
    }

    @SuppressWarnings("unchecked")
    public static Map<Long, Integer> getGuestCart(HttpSession session) {
        Map<Long, Integer> cart = (Map<Long, Integer>) session.getAttribute(GUEST_CART);
        if (cart == null) {
            cart = new LinkedHashMap<>();
            session.setAttribute(GUEST_CART, cart);
        }
        return cart;
    }

    public static class GuestCartItem {
        private final Veiculo veiculo;
        private final int quantidade;

        public GuestCartItem(Veiculo veiculo, int quantidade) {
            this.veiculo = veiculo;
            this.quantidade = quantidade;
        }

        public Veiculo getVeiculo() { return veiculo; }
        public int getQuantidade() { return quantidade; }
        public double getSubtotal() { return veiculo.getPreco() * quantidade; }
    }
}
