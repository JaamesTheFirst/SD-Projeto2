package com.example.projeto_sd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

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
}