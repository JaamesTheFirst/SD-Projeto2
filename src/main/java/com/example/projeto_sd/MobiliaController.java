package com.example.projeto_sd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.projeto_sd.MobiliaRepository;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/mobilia")
public class MobiliaController {

    private final MobiliaRepository mobiliaRepository;
    private final FileStorageService fileStorageService;

    @Autowired
    public MobiliaController(MobiliaRepository mobiliaRepository,
                             FileStorageService fileStorageService) {
        this.mobiliaRepository = mobiliaRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Lista todas as mobílias (será incluído no admin).
     */
    @GetMapping("/lista")
    public String listarMobilias(Model model) {
        List<Mobilia> lista = mobiliaRepository.findAll();
        model.addAttribute("mobilias", lista);
        return "admin"; // Faz render do template admin.html
    }

    @GetMapping("/cliente")
    public String listarMobiliasCliente(Model model) {
        List<Mobilia> lista = mobiliaRepository.findAll();
        model.addAttribute("mobilias", lista);
        return "clientes"; // renderiza clientes.html (precisa existir esse template)
    }
    /**
     * Mostra o formulário para adicionar nova mobília.
     */
    @GetMapping("/nova")
    public String mostrarFormularioCriacao(Model model) {
        model.addAttribute("mobilia", new Mobilia());
        return "add_mobilia"; // Template Thymeleaf que vamos criar abaixo
    }

    /**
     * Processa o formulário de criação: grava imagem, preenche entidade e guarda no BD.
     */
    @PostMapping("/salvar")
    public String salvarMobilia(
            @Valid @ModelAttribute("mobilia") Mobilia mobilia,
            BindingResult bindingResult,
            @RequestParam("imageFile") MultipartFile imageFile,
            RedirectAttributes redirectAttributes,
            Model model) {

        // Validações básicas (pode expandir, por ex., verificar preço >= 0, etc.)
        if (bindingResult.hasErrors()) {
            return "add_mobilia";
        }

        // Se não submeteram uma imagem, podes definir valor default ou rejeitar
        if (imageFile.isEmpty()) {
            model.addAttribute("errorMessage", "É obrigatório adicionar uma imagem para a mobília.");
            return "add_mobilia";
        }

        try {
            // Armazena a imagem no disco e obtém caminho relativo
            String imagePath = fileStorageService.storeFile(imageFile);
            mobilia.setImagem(imagePath);

            // Persiste no BD
            mobiliaRepository.save(mobilia);
            redirectAttributes.addFlashAttribute("successMessage", "Mobília adicionada com sucesso!");
        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "Ocorreu um erro ao gravar a imagem: " + e.getMessage());
            return "add_mobilia";
        }

        // Redireciona de volta à listagem de mobílias (rota GET /mobilia/lista)
        return "redirect:/admin";
    }

    @PostMapping("/deletar/{id}")
    public String deletarMobilia(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            mobiliaRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Mobília removida com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao remover mobília: " + e.getMessage());
        }
        return "redirect:/admin";
    }

    // Aqui podes adicionar métodos para editar e remover, caso queiras:
    // @GetMapping("/editar/{id}") ...
    // @PostMapping("/atualizar") ...
    // @GetMapping("/remover/{id}") ...
}