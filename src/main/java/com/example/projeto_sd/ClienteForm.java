package com.example.projeto_sd;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.crypto.password.PasswordEncoder;

public class ClienteForm {

    @NotBlank(message = "Email é obrigatório")
    private String email;

    @NotBlank(message = "Password é obrigatória")
    @Size(min = 6, message = "A password deve ter pelo menos 6 caracteres")
    private String password;

    @NotBlank(message = "Confirmação de password é obrigatória")
    private String confirmPassword;

    public ClienteForm() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    //Utility method to check if both passwords batem.
    public boolean isPasswordConfirmed() {
        return confirmPassword != null && confirmPassword.equals(password);
    }

    public Cliente toCliente(PasswordEncoder encoder) {
        Cliente c = new Cliente();
        c.setEmail(this.email);
        c.setPassword(encoder.encode(this.password));
        c.setRole("ROLE_USER");
        return c;
    }
}