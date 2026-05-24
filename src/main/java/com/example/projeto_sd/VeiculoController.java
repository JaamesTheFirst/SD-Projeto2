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
import java.util.Optional;

@Controller
@RequestMapping("/veiculo")
public class VeiculoController {

    private final VeiculoRepository veiculoRepository;
    private final CategoriaRepository categoriaRepository;
    private final FileStorageService fileStorageService;

    @Autowired
    public VeiculoController(VeiculoRepository veiculoRepository,
                              CategoriaRepository categoriaRepository,
                              FileStorageService fileStorageService) {
        this.veiculoRepository = veiculoRepository;
        this.categoriaRepository = categoriaRepository;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/novo")
    public String mostrarFormularioCriacao(Model model) {
        model.addAttribute("veiculo", new Veiculo());
        model.addAttribute("categorias", categoriaRepository.findAll());
        return "add_veiculo";
    }

    @PostMapping("/salvar")
    public String salvarVeiculo(
            @Valid @ModelAttribute("veiculo") Veiculo veiculo,
            BindingResult bindingResult,
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam(value = "categoriaId", required = false) Long categoriaId,
            @RequestParam(value = "specs", required = false) String specs,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("categorias", categoriaRepository.findAll());
            return "add_veiculo";
        }

        if (imageFile.isEmpty()) {
            model.addAttribute("errorMessage", "É obrigatório adicionar uma imagem para o veículo.");
            model.addAttribute("categorias", categoriaRepository.findAll());
            return "add_veiculo";
        }

        try {
            String imagePath = fileStorageService.storeFile(imageFile);
            veiculo.setImagem(imagePath);

            if (categoriaId != null) {
                categoriaRepository.findById(categoriaId).ifPresent(veiculo::setCategoria);
            }

            if (specs != null && !specs.isBlank()) {
                veiculo.setSpecs(specs);
            }

            veiculoRepository.save(veiculo);
            redirectAttributes.addFlashAttribute("successMessage", "Veículo adicionado com sucesso!");
        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "Ocorreu um erro ao gravar a imagem: " + e.getMessage());
            model.addAttribute("categorias", categoriaRepository.findAll());
            return "add_veiculo";
        }

        return "redirect:/admin";
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEdicao(@PathVariable Long id, Model model,
                                          RedirectAttributes redirectAttributes) {
        Optional<Veiculo> veiculoOpt = veiculoRepository.findById(id);
        if (veiculoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Veículo não encontrado.");
            return "redirect:/admin";
        }

        model.addAttribute("veiculo", veiculoOpt.get());
        model.addAttribute("categorias", categoriaRepository.findAll());
        return "editar_veiculo";
    }

    @PostMapping("/atualizar/{id}")
    public String atualizarVeiculo(
            @PathVariable Long id,
            @RequestParam String nome,
            @RequestParam(required = false) String marca,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) String descricao,
            @RequestParam double preco,
            @RequestParam int quantidade,
            @RequestParam(value = "categoriaId", required = false) Long categoriaId,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "specs", required = false) String specs,
            RedirectAttributes redirectAttributes) {

        Optional<Veiculo> veiculoOpt = veiculoRepository.findById(id);
        if (veiculoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Veículo não encontrado.");
            return "redirect:/admin";
        }

        Veiculo veiculo = veiculoOpt.get();
        veiculo.setNome(nome);
        veiculo.setMarca(marca);
        veiculo.setAno(ano);
        veiculo.setDescricao(descricao);
        veiculo.setPreco(preco);
        veiculo.setQuantidade(quantidade);

        if (categoriaId != null) {
            categoriaRepository.findById(categoriaId).ifPresent(veiculo::setCategoria);
        } else {
            veiculo.setCategoria(null);
        }

        if (specs != null) {
            veiculo.setSpecs(specs.isBlank() ? null : specs);
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String imagePath = fileStorageService.storeFile(imageFile);
                veiculo.setImagem(imagePath);
            } catch (IOException e) {
                e.printStackTrace();
                redirectAttributes.addFlashAttribute("errorMessage", "Erro ao atualizar a imagem: " + e.getMessage());
                return "redirect:/veiculo/editar/" + id;
            }
        }

        veiculoRepository.save(veiculo);
        redirectAttributes.addFlashAttribute("successMessage", "Veículo atualizado com sucesso!");
        return "redirect:/admin";
    }

    @PostMapping("/deletar/{id}")
    public String deletarVeiculo(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            veiculoRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Veículo removido com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao remover veículo: " + e.getMessage());
        }
        return "redirect:/admin";
    }
}
