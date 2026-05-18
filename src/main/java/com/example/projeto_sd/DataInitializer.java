package com.example.projeto_sd;

import com.example.projeto_sd.Cliente;
import com.example.projeto_sd.ClienteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initAdmin(ClienteRepository clienteRepository,
                                PasswordEncoder passwordEncoder) {
        return args -> {
            // Definimos o e-mail e a senha iniciais do Admin
            String adminEmail = "admin@mobiliubi.pt";
            String adminRawPassword = "admin123";

            // 1) Verifica se já existe um cliente com este email
            boolean exists = clienteRepository.findByEmail(adminEmail).isPresent();

            if (!exists) {
                // 2) Se não existir, cria instância de Cliente
                Cliente admin = new Cliente();
                admin.setEmail(adminEmail);
                // Encripta a password usando o PasswordEncoder configurado no SecurityConfig
                admin.setPassword(passwordEncoder.encode(adminRawPassword));
                // Define o papel (role) como "ADMIN"
                admin.setRole("ADMIN");

                // 3) Persiste no repositório
                clienteRepository.save(admin);
                System.out.println("Usuário admin criado: " + adminEmail + " / " + adminRawPassword);
            } else {
                System.out.println("Usuário admin já existe. Nenhuma ação tomada.");
            }
        };
    }
}