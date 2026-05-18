package com.example.projeto_sd;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Configuração de segurança que:
 * 1. Utiliza apenas autenticação via base de dados (ClienteDetailsService).
 * 2. Registra o DAO provider para ligar ClienteDetailsService + BCryptPasswordEncoder.
 * 3. Redireciona ADMIN → /admin e USER → /clientes após autenticação bem‐sucedida.
 */
@Configuration
public class SecurityConfig {

    private final ClienteDetailsService clienteDetailsService;

    public SecurityConfig(ClienteDetailsService clienteDetailsService) {
        this.clienteDetailsService = clienteDetailsService;
    }

    /**
     * 1) Define o PasswordEncoder (BCrypt).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 2) Cria um DaoAuthenticationProvider que aponta para o ClienteDetailsService
     *    e usa o BCrypt do bean acima para comparar senhas.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        // “clienteDetailsService” deve implementar UserDetailsService e buscar Cliente por email
        provider.setUserDetailsService(clienteDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * 3) Cria um AuthenticationSuccessHandler que, após autenticar:
     *    - Se tiver “ROLE_ADMIN” (ou “ADMIN”), redireciona para /admin;
     *    - Caso contrário (usuário normal), redireciona para /clientes.
     */
    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    Authentication authentication
            ) throws IOException, ServletException {
                // DEBUG: para garantir que este método está sendo chamado,
                // você pode descomentar a linha abaixo temporariamente:
                // System.out.println(">> SUCCESS HANDLER: " + authentication.getName() + "  Roles: " + authentication.getAuthorities());

                for (GrantedAuthority authority : authentication.getAuthorities()) {
                    String papel = authority.getAuthority();
                    // Caso você salve no banco apenas “ADMIN” (sem prefixo):
                    if (papel.equals("ADMIN")) {
                        response.sendRedirect("/admin");
                        return;
                    }
                }
                // Se não encontrou ADMIN na lista de authorities, manda para /clientes
                response.sendRedirect("/cliente");
            }
        };
    }

    /**
     * 4) Configuração principal do Spring Security
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 4.1) Registra o provider JPA/BCrypt acima
                .authenticationProvider(authenticationProvider(passwordEncoder()))

                // 4.2) Configura as autorizações por URL
                .authorizeHttpRequests(auth -> auth
                        // 4.2.1) login, register e POST de login devem ficar liberados
                        .requestMatchers(
                                "/register",
                                "/login",
                                "/login-process",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/error"
                        ).permitAll()

                        // 4.2.2) somente ROLE_ADMIN acessa /admin/** e /mobilia/**
                        .requestMatchers("/admin/**", "/mobilia/**")
                        .hasAuthority("ADMIN") // ou .hasRole("ADMIN") se seu banco grava “ROLE_ADMIN”

                        // 4.2.3) somente ROLE_USER acessa /clientes/** e /clientes
                        .requestMatchers("/clientes/**", "/clientes")
                        .hasAuthority("USER")  // ou .hasRole("USER") se gravar “ROLE_USER”

                        // 4.2.4) qualquer outra rota exige, no mínimo, autenticação
                        .anyRequest().authenticated()
                )

                // 4.3) Configura o formulário de login
                .formLogin(form -> form
                        .loginPage("/login")                   // exibe login.html (GET /login)
                        .usernameParameter("email")            // o campo “username” agora é “email”
                        .passwordParameter("password")         // o campo “password” permanece
                        .loginProcessingUrl("/login-process")  // o POST que Spring interceptará
                        // 4.3.1) Ao autenticar com sucesso, chama nosso handler que redireciona conforme role
                        .successHandler(customAuthenticationSuccessHandler())
                        // 4.3.2) em caso de falha, volta para /login?error=true
                        .failureUrl("/login?error=true")
                        .permitAll()
                )

                // 4.4) Configuração de logout (pode ajustar a URL se desejar)
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                // 4.5) (Opcional) Desabilita CSRF apenas para facilitar testes locais
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}