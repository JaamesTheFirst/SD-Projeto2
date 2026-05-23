# AutoUBI — Loja Online de Veículos

Aplicação web desenvolvida com **Spring Boot** para a unidade curricular de Sistemas Distribuídos (UBI).

Trata-se de uma loja online de veículos motorizados que permite a gestão de produtos, compras online, faturação e estatísticas.

## Requisitos

- **Java 21** (JDK 21 ou superior)
- **MariaDB / MySQL 8+**
- **Gradle** (incluído via Gradle Wrapper — não é necessário instalar separadamente)

### Instalar dependências (Linux — Arch/EndeavourOS)

```bash
sudo pacman -S jdk21-openjdk mariadb
sudo mariadb-install-db --user=mysql --basedir=/usr --datadir=/var/lib/mysql
sudo systemctl enable --now mariadb
```

### Instalar dependências (Linux — Ubuntu/Debian)

```bash
sudo apt update
sudo apt install openjdk-21-jdk mariadb-server
sudo systemctl enable --now mariadb
```

### Instalar dependências (macOS — Homebrew)

```bash
brew install openjdk@21 mariadb
brew services start mariadb
```

### Instalar dependências (Windows)

1. Instale o [JDK 21](https://adoptium.net/) e adicione `JAVA_HOME` às variáveis de ambiente.
2. Instale o [MariaDB](https://mariadb.org/download/) com o instalador oficial.

---

## Configuração da Base de Dados

1. Aceda ao cliente MariaDB/MySQL como root:

   ```bash
   sudo mariadb -u root
   # ou: mysql -u root -p
   ```

2. Crie a base de dados e configure o utilizador:

   ```sql
   CREATE DATABASE IF NOT EXISTS autoubi;
   ALTER USER 'root'@'localhost' IDENTIFIED BY '12345';
   FLUSH PRIVILEGES;
   EXIT;
   ```

3. Execute o script de inicialização:

   ```bash
   mariadb -u root -p12345 autoubi < db/init.sql
   # ou: mysql -u root -p12345 autoubi < db/init.sql
   ```

   Este script cria todas as tabelas, insere as categorias de veículos, a ficha técnica dos veículos e o utilizador administrador.

4. (Opcional) Para usar credenciais ou porta diferentes, edite:
   ```
   src/main/resources/application.properties
   ```
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/autoubi
   spring.datasource.username=root
   spring.datasource.password=12345
   ```

> **Nota:** As tabelas são também criadas automaticamente pelo Hibernate na primeira execução (`spring.jpa.hibernate.ddl-auto=update`), pelo que o passo 3 é opcional — mas é necessário para os dados de exemplo (veículos, categorias, admin).

---

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

   Se tiver várias versões de Java instaladas, defina a correta:
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

---

## Credenciais de Acesso

### Administrador (pré-configurado)
- **Email:** `admin@autoubi.pt`
- **Password:** `admin123`

### Cliente
- Registe-se através da página de registo em `/register`
- Ou navegue como visitante sem conta em `/cliente`

---

## Funcionalidades

### Administrador
- Gestão de veículos (adicionar, editar, remover) com campos: nome, marca, ano, descrição, preço, stock, categoria, imagem e ficha técnica (specs JSON)
- Pesquisa e ordenação do inventário por preço, potência, tamanho, 0–100 km/h e consumo
- Painel de estatísticas: total de vendas, receita total, ticket médio, clientes únicos
- Produto mais e menos vendido
- Gráficos de vendas por mês, receita por período (dia/semana/mês) e produtos mais vendidos
- Gestão de clientes: lista paginada, pesquisa, suspender/reativar conta, remover conta
- Top clientes por valor gasto
- Histórico global de compras com pesquisa por email e detalhe expandível por fatura (com download PDF)

### Cliente / Visitante
- Navegar pelo catálogo como visitante (sem conta) ou autenticado
- Filtrar veículos por nome, preço (min/max), categoria e disponibilidade
- Ordenar catálogo por preço, potência, tamanho, 0–100 km/h e consumo
- Adicionar veículos ao carrinho (animação de voo para o ícone do carrinho)
- Finalizar compra com emissão de fatura em PDF
- Consultar perfil com histórico de compras e estatísticas pessoais
- Carrinho de visitante persistido em sessão, transferido para conta no registo/login

---

## Estrutura do Projeto

```
src/main/java/com/example/projeto_sd/
├── ProjetoSdApplication.java       # Classe principal
├── SecurityConfig.java             # Configuração de segurança (Spring Security + CSRF)
├── WebConfig.java                  # Configuração web (serve uploads de imagens)
├── DataInitializer.java            # Dados iniciais (admin + categorias)
│
├── Categoria.java                  # Entidade Categoria
├── CategoriaRepository.java        # Repositório Categoria
├── Cliente.java                    # Entidade Cliente (com campo suspended)
├── ClienteRepository.java          # Repositório Cliente (pesquisa por email/nome)
├── ClienteForm.java                # DTO de registo
├── ClienteDetailsService.java      # Serviço de autenticação (bloqueia contas suspensas)
│
├── Veiculo.java                    # Entidade Veículo (com specs JSON)
├── VeiculoRepository.java          # Repositório Veículo (filtros dinâmicos)
├── VeiculoController.java          # Controller de gestão de veículos (admin)
│
├── CarrinhoItem.java               # Entidade Item do Carrinho
├── CarrinhoRepository.java         # Repositório Carrinho
├── CarrinhoController.java         # Controller do Carrinho (clientes autenticados)
├── GuestCarrinhoController.java    # Controller do Carrinho (visitantes)
│
├── Fatura.java                     # Entidade Fatura
├── ItemFatura.java                 # Entidade Item da Fatura
├── FaturaRepository.java           # Repositório Fatura
├── ItemFaturaRepository.java       # Repositório Item Fatura
├── FaturaController.java           # Geração e download de faturas PDF
│
├── AdminController.java            # Controller do painel admin
├── ClienteController.java          # Controller do catálogo / perfil / detalhe
├── LoginController.java            # Controller de login
└── RegisterController.java         # Controller de registo
```

---

## Tecnologias Utilizadas

- **Backend:** Spring Boot 3.4.5 (Spring MVC, Spring Security, Spring Data JPA)
- **Frontend:** Thymeleaf, HTML5, CSS3, JavaScript (vanilla)
- **Base de Dados:** MariaDB / MySQL 8+
- **PDF:** OpenPDF 1.3.43
- **Gráficos:** Chart.js
- **Build:** Gradle (Wrapper incluído)


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

   Se tiver várias versões de Java instaladas, defina a correta:
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
