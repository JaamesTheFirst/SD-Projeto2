package com.example.projeto_sd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.Optional;

@Controller
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String showLoginForm(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "registered", required = false) String registered,
            @RequestParam(value = "access", required = false) String access,
            Model model) {

        logger.info("Acessando página de login.");

        if (error != null) {
            model.addAttribute("errorMessage", "Email ou password incorretos");
        }

        if (logout != null) {
            model.addAttribute("successMessage", "Logout efetuado com sucesso");
        }

        if (registered != null) {
            model.addAttribute("successMessage", "Utilizador registado com sucesso! Faça login");
        }

        if ("denied".equals(access)) {
            model.addAttribute("errorMessage", "Acesso negado. Por favor, faça login com as credenciais adequadas.");
        }

        model.addAttribute("clienteForm", new ClienteForm());
        return "login";
    }

    @PostMapping("/login-process")
    public String processLogin(@ModelAttribute("clienteForm") ClienteForm form, Model model) {
        String email = form.getEmail();
        String rawPassword = form.getPassword();
        String adminUser = "admin@mobiliubi.pt";
        String adminPass = "admin123";

        logger.info("Tentando autenticar utilizador com email: {}", email);

        // Verifica se é o admin hardcoded
        if(adminUser.equalsIgnoreCase(email) && adminPass.equals(rawPassword)) {
            logger.info("Utilizador admin autenticado com sucesso.");

            // Define a autenticação no contexto de segurança
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(email, null,
                            List.of(new SimpleGrantedAuthority("ADMIN")));
            SecurityContextHolder.getContext().setAuthentication(auth);

            return "redirect:/admin";
        }

        // Busca o cliente na base de dados
        Optional<Cliente> optionalCliente = clienteRepository.findByEmail(email);
        if (optionalCliente.isEmpty()) {
            model.addAttribute("errorMessage", "Email ou password incorretos");
            model.addAttribute("clienteForm", new ClienteForm());
            return "login";
        }

        Cliente cliente = optionalCliente.get();
        logger.info("Utilizador encontrado: {}", cliente.getEmail());
        logger.debug("Role do utilizador: {}", cliente.getRole());

        // Verifica a password
        if (!passwordEncoder.matches(rawPassword, cliente.getPassword())) {
            logger.warn("Password incorreta para utilizador '{}'", email);
            model.addAttribute("errorMessage", "Email ou password incorretos");
            model.addAttribute("clienteForm", new ClienteForm());
            return "login";
        }

        // Define a autenticação no contexto de segurança baseada na role
        String authority = cliente.getRole(); // "USER" ou "ADMIN"
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(email, null,
                        List.of(new SimpleGrantedAuthority(authority)));
        SecurityContextHolder.getContext().setAuthentication(auth);

        logger.info("Autenticação bem-sucedida para '{}' com role '{}'", email, authority);

        // Redireciona baseado na role
        if ("ADMIN".equals(authority)) {
            return "redirect:/admin";
        } else if ("USER".equals(authority)) {
            return "redirect:/cliente";
        } else {
            logger.warn("Role desconhecida '{}' para utilizador '{}'", authority, email);
            model.addAttribute("errorMessage", "Acesso negado. Contacte o administrador.");
            model.addAttribute("clienteForm", new ClienteForm());
            return "login";
        }
    }

    @GetMapping("/clientes")
    public String mostrarPaginaClientes() {
        return "clientes"; // Renderiza clientes.html
    }

    /*@GetMapping("/admin")
    public String mostrarAdministrador() {
        return "admin"; // Renderiza administradores
    }*/
}