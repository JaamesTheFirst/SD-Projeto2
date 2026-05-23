package com.example.projeto_sd;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class RegisterController {

    private static final Log logger = LogFactory.getLog(RegisterController.class);

    @Autowired
    private ClienteRepository repo;

    @Autowired
    private CarrinhoRepository carrinhoRepository;

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/register")
    public String showForm(@RequestParam(required = false) String from, Model model) {
        logger.info("Acessando página de registo.");
        model.addAttribute("clienteForm", new ClienteForm());
        model.addAttribute("fromGuest", "guest".equals(from));
        return "register";
    }

    @PostMapping("/register")
    public String processRegister(
            @Valid @ModelAttribute("clienteForm") ClienteForm form,
            BindingResult br,
            @RequestParam(required = false) String from,
            Model model,
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        logger.info("Recebido pedido de registo para utilizador: " + form.getEmail());

        if (br.hasErrors() || !form.isPasswordConfirmed()) {
            logger.warn("Validação falhou para o utilizador: " + form.getEmail());
            if (!form.isPasswordConfirmed()) {
                model.addAttribute("passwordError", "As palavras-passe não coincidem.");
            }
            model.addAttribute("errorMessage", "Erro ao registar o utilizador. Verifique os dados e tente novamente.");
            model.addAttribute("fromGuest", "guest".equals(from));
            return "register";
        }

        try {
            Cliente c = new Cliente();
            c.setEmail(form.getEmail());
            c.setPassword(passwordEncoder.encode(form.getPassword()));
            c.setRole("USER");
            repo.save(c);
            logger.info("Cliente guardado com sucesso: " + c.getEmail());

            // Auto-login: create authentication token and store in session
            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                    c.getEmail(), null,
                    List.of(new SimpleGrantedAuthority("USER")));
            SecurityContext sc = SecurityContextHolder.createEmptyContext();
            sc.setAuthentication(authToken);
            SecurityContextHolder.setContext(sc);
            session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);

            // Merge guest session cart → DB cart
            Map<Long, Integer> guestCart = GuestCarrinhoController.getGuestCart(session);
            if (!guestCart.isEmpty()) {
                for (Map.Entry<Long, Integer> entry : guestCart.entrySet()) {
                    Optional<Veiculo> veiculoOpt = veiculoRepository.findById(entry.getKey());
                    if (veiculoOpt.isEmpty()) continue;
                    Veiculo veiculo = veiculoOpt.get();
                    int qty = Math.min(entry.getValue(), veiculo.getQuantidade());
                    if (qty <= 0) continue;

                    Optional<CarrinhoItem> existing =
                        carrinhoRepository.findByClienteEmailAndVeiculoId(c.getEmail(), veiculo.getId());
                    if (existing.isPresent()) {
                        CarrinhoItem item = existing.get();
                        item.setQuantidade(Math.min(item.getQuantidade() + qty, veiculo.getQuantidade()));
                        carrinhoRepository.save(item);
                    } else {
                        CarrinhoItem item = new CarrinhoItem();
                        item.setClienteEmail(c.getEmail());
                        item.setVeiculo(veiculo);
                        item.setQuantidade(qty);
                        carrinhoRepository.save(item);
                    }
                }
                session.removeAttribute(GuestCarrinhoController.GUEST_CART);
                return "redirect:/cliente/carrinho";
            }

            return "redirect:/cliente";

        } catch (Exception e) {
            logger.error("Erro ao guardar cliente na base de dados.", e);
            model.addAttribute("errorMessage", "Erro ao registar o utilizador. Tente novamente mais tarde.");
            model.addAttribute("fromGuest", "guest".equals(from));
            return "register";
        }
    }
}
