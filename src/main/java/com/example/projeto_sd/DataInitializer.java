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
            String adminEmail = "admin@autoubi.pt";
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

            // Categorias dos carros/veículos
            String[][] categorias = {
                {"Citadinos", "Veículos compactos e económicos ideais para uso urbano"},
                {"Sedã / Berlina", "Automóveis de 4 portas com porta-bagagens separado"},
                {"SUV / Todo-o-Terreno", "Veículos desportivos utilitários e para todo-o-terreno"},
                {"Elétricos / Híbridos", "Veículos de propulsão elétrica ou híbrida"},
                {"Desportivos", "Automóveis de alto desempenho e condução desportiva"},
                {"Comerciais", "Carrinhas, furgões e veículos de trabalho"},
                {"Motos", "Motociclos, scooters e ciclomotores"}
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