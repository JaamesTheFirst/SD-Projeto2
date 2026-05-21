# AutoUBI — Loja Online de Veículos

Aplicação web desenvolvida com **Spring Boot** para a unidade curricular de Sistemas Distribuídos (UBI).

Trata-se de uma loja online de veículos motorizados (T4) que permite a gestão de produtos, compras online, faturação e estatísticas.

## Requisitos

- **Java 21** (JDK 21 ou superior)
- **PostgreSQL 14** (ou superior)
- **Gradle** (incluído via Gradle Wrapper — não é necessário instalar separadamente)

### Instalar dependências (Linux — Arch/EndeavourOS)

```bash
sudo pacman -S jdk21-openjdk postgresql
```

### Instalar dependências (Linux — Ubuntu/Debian)

```bash
sudo apt update
sudo apt install openjdk-21-jdk postgresql postgresql-contrib
```

### Instalar dependências (macOS — Homebrew)

```bash
brew install openjdk@21 postgresql@14
```

### Instalar dependências (Windows)

1. Instale o [JDK 21](https://adoptium.net/) e adicione `JAVA_HOME` às variáveis de ambiente.
2. Instale o [PostgreSQL](https://www.postgresql.org/download/windows/) com o instalador oficial.

---

## Configuração da Base de Dados

1. Inicialize e inicie o serviço PostgreSQL:

   **Linux (systemd):**
   ```bash
   # Apenas na primeira vez (Arch/EndeavourOS) — inicializar o cluster de dados:
   sudo -u postgres initdb --locale=C.UTF-8 --encoding=UTF8 -D '/var/lib/postgres/data'

   sudo systemctl enable --now postgresql
   ```

   **macOS:**
   ```bash
   brew services start postgresql@14
   ```

   **Windows:** o serviço é iniciado automaticamente após a instalação.

2. Crie o utilizador e a base de dados:

   ```bash
   sudo -u postgres psql -c "ALTER USER postgres PASSWORD '12345';"
   sudo -u postgres psql -c "CREATE DATABASE autoubi;"
   ```

   > No Windows, abra o **psql** como administrador e execute apenas as duas instruções SQL acima.

3. Execute o script de inicialização:

   ```bash
   psql -U postgres -d autoubi -f db/init.sql
   ```

   Este script cria todas as tabelas, insere as categorias de veículos e o utilizador administrador.

4. (Opcional) Para usar credenciais ou porta diferentes, edite:
   ```
   src/main/resources/application.properties
   ```
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/autoubi
   spring.datasource.username=postgres
   spring.datasource.password=12345
   ```

> **Nota:** As tabelas são também criadas automaticamente pelo Hibernate na primeira execução (`spring.jpa.hibernate.ddl-auto=update`), pelo que o passo 3 é opcional se apenas quiser as categorias e o admin pré-carregados.

## Como Executar

1. Clone o repositório:
   ```bash
   git clone https://github.com/JaamesTheFirst/SD-Projeto2.git
   cd SD-Projeto2
   ```

2. Confirme que o Java 21 está activo:
   ```bash
   java -version
   # deve mostrar: openjdk version "21.x.x"
   ```

   Se tiver várias versões de Java instaladas, defina a correcta:
   ```bash
   # Linux (Arch/EndeavourOS):
   sudo archlinux-java set java-21-openjdk

   # Linux (Ubuntu):
   sudo update-alternatives --config java

   # macOS (Homebrew):
   export JAVA_HOME=$(brew --prefix openjdk@21)
   ```

3. Execute a aplicação com o Gradle Wrapper:

   **Linux/macOS:**
   ```bash
   ./gradlew bootRun
   ```

   **Windows:**
   ```bash
   .\gradlew.bat bootRun
   ```

   Na primeira execução o Gradle descarrega as dependências automaticamente — aguarde até ver `Started ProjetoSdApplication`.

4. Aceda à aplicação no browser:
   ```
   http://localhost:8080
   ```

## Credenciais de Acesso

### Administrador (pré-configurado)
- **Email:** `admin@autoubi.pt`
- **Password:** `admin123`

### Cliente
- Registe-se através da página de registo em `/register`

## Funcionalidades

### Administrador
- Gestão de veículos (adicionar, editar, remover) com campos: nome, marca, ano, descrição, preço, stock, categoria e imagem
- Gestão de stock
- Estatísticas de vendas (total de vendas, receita, ticket médio, clientes únicos)
- Produto mais e menos vendido
- Tabela dos melhores clientes
- Gráficos de vendas por mês, receita por mês e produtos mais vendidos

### Cliente
- Navegar pelo catálogo de veículos organizado por categorias
- Pesquisar veículos por nome, preço, categoria e disponibilidade
- Adicionar veículos ao carrinho de compras
- Finalizar compras com emissão de fatura
- Consultar perfil com histórico de compras e estatísticas pessoais

## Estrutura do Projeto

```
src/main/java/com/example/projeto_sd/
├── ProjetoSdApplication.java     # Classe principal
├── SecurityConfig.java           # Configuração de segurança
├── WebConfig.java                # Configuração web (serve uploads)
├── DataInitializer.java          # Dados iniciais (admin + categorias)
│
├── Categoria.java                # Entidade Categoria
├── CategoriaRepository.java      # Repositório Categoria
├── Cliente.java                  # Entidade Cliente
├── ClienteRepository.java        # Repositório Cliente
├── ClienteForm.java              # DTO de registo
├── ClienteDetailsService.java    # Serviço de autenticação
│
├── Veiculo.java                  # Entidade Produto (Veículo)
├── VeiculoRepository.java        # Repositório Produto
├── VeiculoController.java        # Controller de gestão de veículos
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
- **Base de Dados:** PostgreSQL
- **Gráficos:** Chart.js
- **Build:** Gradle
