-- =============================================================
-- Script de criação da base de dados para a aplicação Mobílubi
-- SGBD: MySQL 8.0+
-- =============================================================

-- Criar a base de dados
CREATE DATABASE IF NOT EXISTS mobiliubi
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE mobiliubi;

-- =============================================================
-- Tabela: categoria
-- =============================================================
CREATE TABLE IF NOT EXISTS categoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL UNIQUE,
    descricao VARCHAR(255)
) ENGINE=InnoDB;

-- =============================================================
-- Tabela: cliente (utilizadores do sistema)
-- =============================================================
CREATE TABLE IF NOT EXISTS cliente (
    email VARCHAR(255) NOT NULL PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
) ENGINE=InnoDB;

-- =============================================================
-- Tabela: mobilia (produtos)
-- =============================================================
CREATE TABLE IF NOT EXISTS mobilia (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    descricao VARCHAR(500),
    preco DOUBLE NOT NULL,
    quantidade INT NOT NULL,
    image_path VARCHAR(255),
    categoria_id BIGINT,
    CONSTRAINT fk_mobilia_categoria
        FOREIGN KEY (categoria_id) REFERENCES categoria(id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

-- =============================================================
-- Tabela: carrinho_item (items no carrinho de compras)
-- =============================================================
CREATE TABLE IF NOT EXISTS carrinho_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_email VARCHAR(255) NOT NULL,
    mobilia_id BIGINT NOT NULL,
    quantidade INT NOT NULL DEFAULT 1,
    CONSTRAINT fk_carrinho_mobilia
        FOREIGN KEY (mobilia_id) REFERENCES mobilia(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- =============================================================
-- Tabela: fatura (faturas / vendas)
-- =============================================================
CREATE TABLE IF NOT EXISTS fatura (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_email VARCHAR(255) NOT NULL,
    data_compra DATETIME NOT NULL,
    total_pago DOUBLE NOT NULL
) ENGINE=InnoDB;

-- =============================================================
-- Tabela: item_fatura (itens de cada fatura)
-- =============================================================
CREATE TABLE IF NOT EXISTS item_fatura (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fatura_id BIGINT NOT NULL,
    nome_mobilia VARCHAR(255) NOT NULL,
    preco_unitario DOUBLE NOT NULL,
    quantidade INT NOT NULL,
    CONSTRAINT fk_item_fatura
        FOREIGN KEY (fatura_id) REFERENCES fatura(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- =============================================================
-- Dados iniciais
-- =============================================================

-- Categorias
INSERT IGNORE INTO categoria (nome, descricao) VALUES
    ('Sala de Estar', 'Sofás, mesas de centro, estantes e móveis para a sala'),
    ('Quarto', 'Camas, cómodas, mesas de cabeceira e roupeiros'),
    ('Cozinha', 'Mesas de cozinha, cadeiras, armários e bancadas'),
    ('Casa de Banho', 'Móveis de casa de banho, espelhos e acessórios'),
    ('Escritório', 'Secretárias, cadeiras de escritório e estantes'),
    ('Jardim', 'Mesas de jardim, cadeiras de exterior, espreguiçadeiras e pérgulas'),
    ('Decoração', 'Candeeiros, quadros, tapetes e objetos decorativos');

-- Utilizador administrador
-- Password: admin123 (BCrypt encoded)
INSERT IGNORE INTO cliente (email, password, role) VALUES
    ('admin@mobiliubi.pt', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN');

-- NOTA: A password BCrypt acima é gerada em runtime pelo DataInitializer.
-- Se executar este script diretamente, o admin será criado com esta hash.
-- Alternativamente, deixe o DataInitializer criar o admin automaticamente.
