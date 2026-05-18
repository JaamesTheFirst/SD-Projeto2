package com.example.projeto_sd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/mobilia")
public class MobiliaController {

    private final MobiliaRepository mobiliaRepository;
    private final CategoriaRepository categoriaRepository;
    private final FileStorageService fileStorageService;

    @Autowired
    public MobiliaController(MobiliaRepository mobiliaRepository,
                             CategoriaRepository categoriaRepository,
                             FileStorageService fileStorageService) {
        this.mobiliaRepository = mobiliaRepository;
        this.categoriaRepository = categoriaRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Lista todas as mobílias (será incluído no admin).
     */
    @GetMapping("/lista")
    public String listarMobilias(Model model) {
        List<Mobilia> lista = mobiliaRepository.findAll();
        model.addAttribute("mobilias", lista);
        return "admin";
    }

    /**
     * Mostra o formulário para adicionar nova mobília.
     */
    @GetMapping("/nova")
    public String mostrarFormularioCriacao(Model model) {
        model.addAttribute("mobilia", new Mobilia());
        model.addAttribute("categorias", categoriaRepository.findAll());
        return "add_mobilia";
    }

    /**
     * Processa o formulário de criação: grava imagem, preenche entidade e guarda no BD.
     */
    @PostMapping("/salvar")
    public String salvarMobilia(
            @Valid @ModelAttribute("mobilia") Mobilia mobilia,
            BindingResult bindingResult,
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam(value = "categoriaId", required = false) Long categoriaId,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("categorias", categoriaRepository.findAll());
            return "add_mobilia";
        }

        if (imageFile.isEmpty()) {
            model.addAttribute("errorMessage", "É obrigatório adicionar uma imagem para a mobília.");
            model.addAttribute("categorias", categoriaRepository.findAll());
            return "add_mobilia";
        }

        try {
            String imagePath = fileStorageService.storeFile(imageFile);
            mobilia.setImagem(imagePath);

            // Associar categoria
            if (categoriaId != null) {
                categoriaRepository.findById(categoriaId).ifPresent(mobilia::setCategoria);
            }

            mobiliaRepository.save(mobilia);
            redirectAttributes.addFlashAttribute("successMessage", "Mobília adicionada com sucesso!");
        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "Ocorreu um erro ao gravar a imagem: " + e.getMessage());
            model.addAttribute("categorias", categoriaRepository.findAll());
            return "add_mobilia";
        }

        return "redirect:/admin";
    }

    /**
     * Mostra o formulário de edição de uma mobília existente.
     */
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEdicao(@PathVariable Long id, Model model,
                                          RedirectAttributes redirectAttributes) {
        Optional<Mobilia> mobiliaOpt = mobiliaRepository.findById(id);
        if (mobiliaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mobília não encontrada.");
            return "redirect:/admin";
        }

        model.addAttribute("mobilia", mobiliaOpt.get());
        model.addAttribute("categorias", categoriaRepository.findAll());
        return "editar_mobilia";
    }

    /**
     * Processa a edição de uma mobília.
     */
    @PostMapping("/atualizar/{id}")
    public String atualizarMobilia(
            @PathVariable Long id,
            @RequestParam String nome,
            @RequestParam(required = false) String descricao,
            @RequestParam double preco,
            @RequestParam int quantidade,
            @RequestParam(value = "categoriaId", required = false) Long categoriaId,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes) {

        Optional<Mobilia> mobiliaOpt = mobiliaRepository.findById(id);
        if (mobiliaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mobília não encontrada.");
            return "redirect:/admin";
        }

        Mobilia mobilia = mobiliaOpt.get();
        mobilia.setNome(nome);
        mobilia.setDescricao(descricao);
        mobilia.setPreco(preco);
        mobilia.setQuantidade(quantidade);

        // Atualizar categoria
        if (categoriaId != null) {
            categoriaRepository.findById(categoriaId).ifPresent(mobilia::setCategoria);
        } else {
            mobilia.setCategoria(null);
        }

        // Atualizar imagem (se fornecida nova imagem)
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String imagePath = fileStorageService.storeFile(imageFile);
                mobilia.setImagem(imagePath);
            } catch (IOException e) {
                e.printStackTrace();
                redirectAttributes.addFlashAttribute("errorMessage", "Erro ao atualizar imagem: " + e.getMessage());
                return "redirect:/mobilia/editar/" + id;
            }
        }

        mobiliaRepository.save(mobilia);
        redirectAttributes.addFlashAttribute("successMessage", "Mobília atualizada com sucesso!");
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
}