package com.example.projeto_sd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Configuração de segurança que:
 * 1. Utiliza apenas autenticação via base de dados (ClienteDetailsService).
 * 2. Registra o DAO provider para ligar ClienteDetailsService + BCryptPasswordEncoder.
 * 3. Redireciona ADMIN → /admin e USER → /clientes após autenticação bem‐sucedida.
 */
@Configuration
public class SecurityConfig {

    private final ClienteDetailsService clienteDetailsService;

    @Autowired
    private CarrinhoRepository carrinhoRepository;

    @Autowired
    private VeiculoRepository veiculoRepository;

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
     *    e usa o BCrypt do bean acima para comparar palavras-passe.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        // “clienteDetailsService” deve implementar UserDetailsService e buscar Cliente por email
        provider.setUserDetailsService(clienteDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    Authentication authentication
            ) throws IOException, ServletException {

                // Merge guest session cart into the DB for the authenticated user
                HttpSession session = request.getSession(false);
                boolean hadGuestCart = false;
                if (session != null) {
                    Map<Long, Integer> guestCart = GuestCarrinhoController.getGuestCart(session);
                    if (!guestCart.isEmpty()) {
                        String email = authentication.getName();
                        for (Map.Entry<Long, Integer> entry : guestCart.entrySet()) {
                            Optional<Veiculo> veiculoOpt = veiculoRepository.findById(entry.getKey());
                            if (veiculoOpt.isEmpty()) continue;
                            Veiculo veiculo = veiculoOpt.get();
                            int qty = Math.min(entry.getValue(), veiculo.getQuantidade());
                            if (qty <= 0) continue;
                            Optional<CarrinhoItem> existing =
                                carrinhoRepository.findByClienteEmailAndVeiculoId(email, veiculo.getId());
                            if (existing.isPresent()) {
                                CarrinhoItem item = existing.get();
                                item.setQuantidade(Math.min(item.getQuantidade() + qty, veiculo.getQuantidade()));
                                carrinhoRepository.save(item);
                            } else {
                                CarrinhoItem item = new CarrinhoItem();
                                item.setClienteEmail(email);
                                item.setVeiculo(veiculo);
                                item.setQuantidade(qty);
                                carrinhoRepository.save(item);
                            }
                        }
                        session.removeAttribute(GuestCarrinhoController.GUEST_CART);
                        hadGuestCart = true;
                    }
                }

                for (GrantedAuthority authority : authentication.getAuthorities()) {
                    if (authority.getAuthority().equals("ADMIN")) {
                        response.sendRedirect("/admin");
                        return;
                    }
                }
                // If items were merged, send user to cart; otherwise default redirect
                response.sendRedirect(hadGuestCart ? "/cliente/carrinho" : "/cliente");
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
                                "/error",
                                "/cliente",
                                "/cliente/veiculo/**",
                                "/guest/**"
                        ).permitAll()

                        // 4.2.2) somente ROLE_ADMIN acessa /admin/** e /veiculo/**
                        .requestMatchers("/admin/**", "/veiculo/**")
                        .hasAuthority("ADMIN") // ou .hasRole("ADMIN") se a base de dados guardar "ROLE_ADMIN"

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

                // 4.5) CSRF protection enabled (Thymeleaf injects token in forms automatically)
                // AJAX calls must include the X-CSRF-TOKEN header from the _csrf meta tag
                ;

        return http.build();
    }
}