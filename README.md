# Mobílubi — Loja Online de Mobílias

Aplicação web desenvolvida com **Spring Boot** para a unidade curricular de Sistemas Distribuídos (UBI).

Trata-se de uma loja online de mobílias (casa e jardim) que permite a gestão de produtos, compras online, faturação e estatísticas.

## Requisitos

- **Java 21** (JDK 21 ou superior)
- **MySQL 8.0** (ou superior)
- **Gradle** (incluído via Gradle Wrapper — não é necessário instalar separadamente)

## Configuração da Base de Dados

1. Inicie o servidor MySQL.

2. Crie a base de dados:
   ```sql
   CREATE DATABASE mobiliubi CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

3. (Opcional) Caso deseje alterar as credenciais de acesso à base de dados, edite o ficheiro:
   ```
   src/main/resources/application.properties
   ```
   E altere as seguintes propriedades:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/mobiliubi
   spring.datasource.username=root
   spring.datasource.password=12345
   ```

> **Nota:** As tabelas são criadas automaticamente pelo Hibernate na primeira execução (`spring.jpa.hibernate.ddl-auto=update`).

## Como Executar

1. Clone o repositório:
   ```bash
   git clone https://github.com/JaamesTheFirst/SD-Projeto2.git
   cd SD-Projeto2
   ```

2. Execute a aplicação com o Gradle Wrapper:

   **Windows:**
   ```bash
   .\gradlew.bat bootRun
   ```

   **Linux/macOS:**
   ```bash
   ./gradlew bootRun
   ```

3. Aceda à aplicação no browser:
   ```
   http://localhost:8080
   ```

## Credenciais de Acesso

### Administrador (pré-configurado)
- **Email:** `admin@mobiliubi.pt`
- **Password:** `admin123`

### Cliente
- Registe-se através da página de registo em `/register`

## Funcionalidades

### Administrador
- Gestão de produtos (adicionar, editar, remover)
- Gestão de stock
- Estatísticas de vendas (total de vendas, receita, ticket médio, clientes únicos)
- Gráficos de vendas por mês e produtos mais vendidos

### Cliente
- Navegar pelo catálogo de mobílias organizado por categorias
- Pesquisar produtos por nome, preço e disponibilidade
- Adicionar produtos ao carrinho de compras
- Finalizar compras com emissão de fatura
- Consultar perfil com histórico de compras e estatísticas pessoais

## Estrutura do Projeto

```
src/main/java/com/example/projeto_sd/
├── ProjetoSdApplication.java     # Classe principal
├── SecurityConfig.java           # Configuração de segurança
├── WebConfig.java                # Configuração web
├── DataInitializer.java          # Dados iniciais (admin + categorias)
│
├── Categoria.java                # Entidade Categoria
├── CategoriaRepository.java      # Repositório Categoria
├── Cliente.java                  # Entidade Cliente
├── ClienteRepository.java        # Repositório Cliente
├── ClienteForm.java              # DTO de registo
├── ClienteDetailsService.java    # Serviço de autenticação
│
├── Mobilia.java                  # Entidade Produto (Mobília)
├── MobiliaRepository.java        # Repositório Produto
├── MobiliaController.java        # Controller de gestão de produtos
│
├── CarrinhoItem.java             # Entidade Item do Carrinho
├── CarrinhoRepository.java       # Repositório Carrinho
├── CarrinhoController.java       # Controller do Carrinho
│
├── Fatura.java                   # Entidade Fatura
├── ItemFatura.java               # Entidade Item da Fatura
├── FaturaRepository.java         # Repositório Fatura
├── ItemFaturaRepository.java     # Repositório Item Fatura
├── FaturaController.java         # API REST de faturas
│
├── AdminController.java          # Controller do painel admin
├── ClienteController.java        # Controller do catálogo/perfil
├── LoginController.java          # Controller de login
├── RegisterController.java       # Controller de registo
└── FileStorageService.java       # Serviço de upload de imagens
```

## Tecnologias Utilizadas

- **Backend:** Spring Boot 3.4.5 (Spring MVC, Spring Security, Spring Data JPA)
- **Frontend:** Thymeleaf, HTML5, CSS3, JavaScript
- **Base de Dados:** MySQL
- **Gráficos:** Chart.js
- **Build:** Gradle