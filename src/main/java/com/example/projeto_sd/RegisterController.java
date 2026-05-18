package com.example.projeto_sd;

import jakarta.validation.Valid;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RegisterController {

    private static final Log logger = LogFactory.getLog(RegisterController.class);

    @Autowired
    private ClienteRepository repo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/register")
    public String showForm(Model model) {
        logger.info("Acessando página de registo.");
        model.addAttribute("clienteForm", new ClienteForm());
        return "register";
    }

    @PostMapping("/register")
    public String processRegister(
            @Valid @ModelAttribute("clienteForm") ClienteForm form,
            BindingResult br,
            Model model
    ) {
        logger.info("Recebido pedido de registo para utilizador: " + form.getEmail());

        if (br.hasErrors() || !form.isPasswordConfirmed()) {
            logger.warn("Validação falhou para o utilizador: " + form.getEmail());
            if (!form.isPasswordConfirmed()) {
                logger.warn("As senhas não coincidem.");
                model.addAttribute("passwordError", "As senhas não conferem.");
            }
            model.addAttribute("errorMessage", "Erro ao registar o utilizador. Verifique os dados e tente novamente.");
            return "register";
        }

        try {
            //Cliente c = new Cliente();
            Cliente c = form.toCliente(passwordEncoder);
            c.setEmail(form.getEmail());
            String encoded = passwordEncoder.encode(form.getPassword());
            c.setPassword(encoded);
            c.setRole("USER");

            logger.info("Tentando guardar o cliente no repositório: " + c.getEmail());
            repo.save(c);
            logger.info("Cliente guardado com sucesso.");

            model.addAttribute("successMessage", "Utilizador registado com sucesso!");
        } catch (Exception e) {
            logger.error("Erro ao guardar cliente na base de dados.", e);
            model.addAttribute("errorMessage", "Erro ao registar o utilizador. Tente novamente mais tarde.");
        }

        return "redirect:/login?registered";
    }
}