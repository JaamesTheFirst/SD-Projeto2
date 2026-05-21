-- =============================================================
-- Script de criação da base de dados para a aplicação AutoUBI
-- SGBD: MySQL 8+
-- =============================================================

-- Criar a base de dados (executar separadamente se necessário)
-- CREATE DATABASE autoubi;

-- =============================================================
-- Tabela: categoria
-- =============================================================
CREATE TABLE IF NOT EXISTS categoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL UNIQUE,
    descricao VARCHAR(255)
);

-- =============================================================
-- Tabela: cliente (utilizadores do sistema)
-- =============================================================
CREATE TABLE IF NOT EXISTS cliente (
    email VARCHAR(255) NOT NULL PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

-- =============================================================
-- Tabela: veiculo (produtos)
-- =============================================================
CREATE TABLE IF NOT EXISTS veiculo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    marca VARCHAR(255),
    ano INTEGER,
    descricao VARCHAR(500),
    preco DOUBLE PRECISION NOT NULL,
    quantidade INTEGER NOT NULL,
    image_path VARCHAR(255),
    categoria_id BIGINT,
    CONSTRAINT fk_veiculo_categoria
        FOREIGN KEY (categoria_id) REFERENCES categoria(id)
        ON DELETE SET NULL
);

-- =============================================================
-- Tabela: carrinho_item (items no carrinho de compras)
-- =============================================================
CREATE TABLE IF NOT EXISTS carrinho_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_email VARCHAR(255) NOT NULL,
    veiculo_id BIGINT NOT NULL,
    quantidade INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_carrinho_veiculo
        FOREIGN KEY (veiculo_id) REFERENCES veiculo(id)
        ON DELETE CASCADE
);

-- =============================================================
-- Tabela: fatura (faturas / vendas)
-- =============================================================
CREATE TABLE IF NOT EXISTS fatura (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_email VARCHAR(255) NOT NULL,
    data_compra TIMESTAMP NOT NULL,
    total_pago DOUBLE PRECISION NOT NULL
);

-- =============================================================
-- Tabela: item_fatura (itens de cada fatura)
-- =============================================================
CREATE TABLE IF NOT EXISTS item_fatura (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fatura_id BIGINT NOT NULL,
    nome_veiculo VARCHAR(255) NOT NULL,
    preco_unitario DOUBLE PRECISION NOT NULL,
    quantidade INTEGER NOT NULL,
    CONSTRAINT fk_item_fatura
        FOREIGN KEY (fatura_id) REFERENCES fatura(id)
        ON DELETE CASCADE
);

-- =============================================================
-- Dados iniciais
-- =============================================================

-- Categorias de veículos
INSERT INTO categoria (nome, descricao)
VALUES
    ('Citadinos', 'Veículos compactos e económicos ideais para uso urbano'),
    ('Sedã / Berlina', 'Automóveis de 4 portas com porta-bagagens separado'),
    ('SUV / Todo-o-Terreno', 'Veículos desportivos utilitários e para todo-o-terreno'),
    ('Elétricos / Híbridos', 'Veículos de propulsão elétrica ou híbrida'),
    ('Desportivos', 'Automóveis de alto desempenho e condução desportiva'),
    ('Comerciais', 'Carrinhas, furgões e veículos de trabalho'),
    ('Motos', 'Motociclos, scooters e ciclomotores')
ON DUPLICATE KEY UPDATE nome=nome;

-- Utilizador administrador criado automaticamente pelo DataInitializer ao iniciar a aplicação.
-- Não inserir aqui para evitar conflito de hash. Credenciais: admin@autoubi.pt / admin123
