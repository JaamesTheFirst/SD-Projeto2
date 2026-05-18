package com.example.projeto_sd;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(ClienteRepository clienteRepository,
                               CategoriaRepository categoriaRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            // ---- Criar Admin ----
            String adminEmail = "admin@mobiliubi.pt";
            String adminRawPassword = "admin123";

            boolean adminExists = clienteRepository.findByEmail(adminEmail).isPresent();
            if (!adminExists) {
                Cliente admin = new Cliente();
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode(adminRawPassword));
                admin.setRole("ADMIN");
                clienteRepository.save(admin);
                System.out.println("Utilizador admin criado: " + adminEmail + " / " + adminRawPassword);
            } else {
                System.out.println("Utilizador admin já existe.");
            }

            // ---- Criar Categorias ----
            String[][] categorias = {
                {"Sala de Estar", "Sofás, mesas de centro, estantes e móveis para a sala"},
                {"Quarto", "Camas, cómodas, mesas de cabeceira e roupeiros"},
                {"Cozinha", "Mesas de cozinha, cadeiras, armários e bancadas"},
                {"Casa de Banho", "Móveis de casa de banho, espelhos e acessórios"},
                {"Escritório", "Secretárias, cadeiras de escritório e estantes"},
                {"Jardim", "Mesas de jardim, cadeiras de exterior, espreguiçadeiras e pérgulas"},
                {"Decoração", "Candeeiros, quadros, tapetes e objetos decorativos"}
            };

            for (String[] cat : categorias) {
                if (categoriaRepository.findByNome(cat[0]).isEmpty()) {
                    categoriaRepository.save(new Categoria(cat[0], cat[1]));
                    System.out.println("Categoria criada: " + cat[0]);
                }
            }
        };
    }
}