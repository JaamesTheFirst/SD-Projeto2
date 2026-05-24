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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(authenticationProvider(passwordEncoder()))

                .authorizeHttpRequests(auth -> auth
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

                        .requestMatchers("/admin/**", "/veiculo/**")
                        .hasAuthority("ADMIN")

                        .requestMatchers("/clientes/**", "/clientes")
                        .hasAuthority("USER")  // ou .hasRole("USER") se gravar “ROLE_USER”

                        .anyRequest().authenticated()
                )


                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .loginProcessingUrl("/login-process")
                        .successHandler(customAuthenticationSuccessHandler())
                        .failureUrl("/login?error=true")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                ;

        return http.build();
    }
}