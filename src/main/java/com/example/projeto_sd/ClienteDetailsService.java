package com.example.projeto_sd;

import com.example.projeto_sd.Cliente;
import com.example.projeto_sd.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteDetailsService implements UserDetailsService {
    private final ClienteRepository repo;

    public ClienteDetailsService(ClienteRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Cliente c = (Cliente) repo.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilizador não encontrado"));
        return new User(
                c.getEmail(),
                c.getPassword(),
                /* enabled= */ !c.isSuspended(),
                /* accountNonExpired= */ true,
                /* credentialsNonExpired= */ true,
                /* accountNonLocked= */ !c.isSuspended(),
                List.of(new SimpleGrantedAuthority(c.getRole()))
        );
    }
}