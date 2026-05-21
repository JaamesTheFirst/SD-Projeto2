package com.example.projeto_sd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class FaturaController {

    @Autowired
    private FaturaRepository faturaRepository;

    @GetMapping("/fatura/{id}")
    public ResponseEntity<Map<String, Object>> getFaturaDetalhes(@PathVariable Long id, Authentication auth) {
        try {
            String clienteEmail = auth.getName();
            
            Optional<Fatura> faturaOpt = faturaRepository.findByIdWithItens(id);
            
            if (faturaOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Fatura fatura = faturaOpt.get();
            
            // Verificar se a fatura pertence ao cliente logado
            if (!fatura.getClienteEmail().equals(clienteEmail)) {
                return ResponseEntity.status(403).build(); // Forbidden
            }
            
            // Criar resposta manual para evitar problemas de serialização
            Map<String, Object> response = new HashMap<>();
            response.put("id", fatura.getId());
            response.put("dataCompra", fatura.getDataCompra());
            response.put("totalPago", fatura.getTotalPago());
            
            // Serializar os itens manualmente
            List<Map<String, Object>> itensResponse = fatura.getItens().stream()
                .map(item -> {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("id", item.getId());
                    itemMap.put("nomeVeiculo", item.getNomeVeiculo());
                    itemMap.put("precoUnitario", item.getPrecoUnitario());
                    itemMap.put("quantidade", item.getQuantidade());
                    itemMap.put("subtotal", item.getSubtotal());
                    return itemMap;
                })
                .collect(Collectors.toList());
                
            response.put("itens", itensResponse);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log do erro para debug
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erro interno do servidor");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}