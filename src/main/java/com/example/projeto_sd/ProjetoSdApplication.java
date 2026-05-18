package com.example.projeto_sd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
//@EnableJpaRepositories(basePackageClasses = DepartamentoRepository.class)
@EntityScan(basePackages = "com.example.projeto_sd")
public class ProjetoSdApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProjetoSdApplication.class, args);
    }
}