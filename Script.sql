-- Script SQL para MySQL Workbench
-- Criação do banco de dados SistemaGirias

-- Cria o banco de dados se ele não existir
CREATE DATABASE IF NOT EXISTS sistema_girias;

-- Usa o banco de dados criado
USE sistema_girias;

-- Desativa verificações de chaves estrangeiras durante a criação das tabelas
SET foreign_key_checks = 0;

-- Remove as tabelas caso já existam para garantir uma criação limpa
DROP TABLE IF EXISTS giria_regiao;
DROP TABLE IF EXISTS giria_categoria;
DROP TABLE IF EXISTS voto;
DROP TABLE IF EXISTS explicacao;
DROP TABLE IF EXISTS giria;
DROP TABLE IF EXISTS usuario;
DROP TABLE IF EXISTS categoria;
DROP TABLE IF EXISTS regiao;

-- Criação da tabela Usuario
CREATE TABLE usuario (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    senha VARCHAR(255) NOT NULL,
    reputacao INT DEFAULT 0,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Criação da tabela Giria
CREATE TABLE giria (
    id INT PRIMARY KEY AUTO_INCREMENT,
    termo VARCHAR(255) NOT NULL UNIQUE, -- Termo da gíria deve ser único
    data_cadastro TIMESTAMP NOT NULL, -- Alterado para TIMESTAMP para compatibilidade com LocalDateTime
    aprovada BOOLEAN DEFAULT FALSE,
    usuario_propositor_id INT NOT NULL,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- Herdado de EntidadeBase
    FOREIGN KEY (usuario_propositor_id) REFERENCES usuario(id) ON DELETE CASCADE
);

-- Criação da tabela Explicacao
CREATE TABLE explicacao (
    id INT PRIMARY KEY AUTO_INCREMENT,
    definicao TEXT NOT NULL,
    exemplo_uso TEXT,
    aprovada BOOLEAN DEFAULT FALSE,
    data_proposta DATE NOT NULL, -- Mantido como DATE para compatibilidade com LocalDate
    usuario_propositor_id INT NOT NULL,
    giria_associada_id INT NOT NULL,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- Herdado de EntidadeBase
    FOREIGN KEY (usuario_propositor_id) REFERENCES usuario(id) ON DELETE CASCADE,
    FOREIGN KEY (giria_associada_id) REFERENCES giria(id) ON DELETE CASCADE
);

-- Criação da tabela Voto
CREATE TABLE voto (
    id INT PRIMARY KEY AUTO_INCREMENT,
    tipo ENUM('POSITIVO', 'NEGATIVO') NOT NULL,
    data_voto TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_votante_id INT NOT NULL,
    objeto_avaliado_id INT NOT NULL,
    tipo_objeto_avaliado VARCHAR(50) NOT NULL, -- 'GIRIA' ou 'EXPLICACAO'
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- Herdado de EntidadeBase
    FOREIGN KEY (usuario_votante_id) REFERENCES usuario(id) ON DELETE CASCADE,
    INDEX idx_voto_objeto (objeto_avaliado_id, tipo_objeto_avaliado)
    -- Não há FK direta para giria_associada_id ou explicacao_associada_id aqui,
    -- pois 'objeto_avaliado_id' pode referenciar IDs de tabelas diferentes.
    -- A integridade referencial para 'objeto_avaliado_id' deve ser tratada na aplicação.
);

-- Tabela para Categorias (ex: "Tecnologia", "Jovem")
CREATE TABLE categoria (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL UNIQUE,
    INDEX idx_categoria_nome (nome)
);

-- Tabela para Regiões (ex: "Rio de Janeiro", "Sudeste")
CREATE TABLE regiao (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL UNIQUE,
    INDEX idx_regiao_nome (nome)
);

-- Tabela Associativa para Giria-Categoria (N:N)
CREATE TABLE giria_categoria (
    giria_id INT NOT NULL,
    categoria_id INT NOT NULL,
    PRIMARY KEY (giria_id, categoria_id),
    FOREIGN KEY (giria_id) REFERENCES giria(id) ON DELETE CASCADE,
    FOREIGN KEY (categoria_id) REFERENCES categoria(id) ON DELETE CASCADE
);

-- Tabela Associativa para Giria-Regiao (N:N)
CREATE TABLE giria_regiao (
    giria_id INT NOT NULL,
    regiao_id INT NOT NULL,
    PRIMARY KEY (giria_id, regiao_id),
    FOREIGN KEY (giria_id) REFERENCES giria(id) ON DELETE CASCADE,
    FOREIGN KEY (regiao_id) REFERENCES regiao(id) ON DELETE CASCADE
);

-- Adiciona índices para melhorar performance em consultas frequentes
CREATE INDEX idx_giria_termo ON giria(termo);
CREATE INDEX idx_giria_aprovada ON giria(aprovada);
CREATE INDEX idx_explicacao_giria ON explicacao(giria_associada_id);
CREATE INDEX idx_explicacao_aprovada ON explicacao(aprovada);

-- Reativa o modo de segurança
SET foreign_key_checks = 1;

-- Inserção de algumas categorias iniciais
INSERT INTO categoria (nome) VALUES 
('Internet'), 
('Jovem'), 
('Regional'), 
('Tecnologia'), 
('Acadêmico'),
('Música'),
('Cinema'),
('Games');

-- Inserção de algumas regiões iniciais
INSERT INTO regiao (nome) VALUES 
('Rio de Janeiro'), 
('São Paulo'), 
('Nordeste'), 
('Sul'),
('Norte'),
('Centro-Oeste'),
('Nacional'),
('Internacional');

-- Inserção de um usuário administrador inicial (senha: admin123)
INSERT INTO usuario (nome, email, senha, reputacao) VALUES 
('Administrador', 'admin@sistemagiriasteste.com', 'admin123', 100);

-- Inserção de alguns usuários de teste
INSERT INTO usuario (nome, email, senha, reputacao) VALUES 
('João Silva', 'joao@teste.com', 'senha123', 10),
('Maria Oliveira', 'maria@teste.com', 'senha456', 15),
('Pedro Santos', 'pedro@teste.com', 'senha789', 5);

-- Inserção de algumas gírias iniciais
INSERT INTO giria (termo, data_cadastro, aprovada, usuario_propositor_id) VALUES 
('Mano do céu', NOW(), TRUE, 1),
('Tá ligado', NOW(), TRUE, 2),
('Mó treta', NOW(), TRUE, 3),
('Partiu', NOW(), TRUE, 2),
('Dar um rolê', NOW(), FALSE, 3);

-- Associar gírias às categorias
INSERT INTO giria_categoria (giria_id, categoria_id) VALUES 
(1, 2), -- Mano do céu - Jovem
(1, 3), -- Mano do céu - Regional
(2, 2), -- Tá ligado - Jovem
(3, 2), -- Mó treta - Jovem
(4, 2), -- Partiu - Jovem
(5, 2); -- Dar um rolê - Jovem

-- Associar gírias às regiões
INSERT INTO giria_regiao (giria_id, regiao_id) VALUES 
(1, 1), -- Mano do céu - Rio de Janeiro
(2, 7), -- Tá ligado - Nacional
(3, 2), -- Mó treta - São Paulo
(4, 7), -- Partiu - Nacional
(5, 7); -- Dar um rolê - Nacional

-- Inserir algumas explicações para as gírias
INSERT INTO explicacao (definicao, exemplo_uso, aprovada, data_proposta, usuario_propositor_id, giria_associada_id) VALUES
('Expressão de espanto ou surpresa', 'Mano do céu, você viu aquilo?', TRUE, CURDATE(), 1, 1),
('Pergunta se a pessoa entendeu o que foi dito', 'Vai ter prova amanhã, tá ligado?', TRUE, CURDATE(), 2, 2),
('Significa um problema, confusão ou situação complicada', 'Foi mó treta o que aconteceu na festa ontem', TRUE, CURDATE(), 3, 3),
('Indica que vai sair para algum lugar ou fazer algo', 'Partiu cinema hoje à noite!', TRUE, CURDATE(), 1, 4),
('Sair para passear, se divertir', 'Vamos dar um rolê na praia esse fim de semana?', FALSE, CURDATE(), 2, 5);

-- Inserir alguns votos
INSERT INTO voto (tipo, usuario_votante_id, objeto_avaliado_id, tipo_objeto_avaliado) VALUES
('POSITIVO', 2, 1, 'GIRIA'),
('POSITIVO', 3, 1, 'GIRIA'),
('POSITIVO', 1, 2, 'GIRIA'),
('NEGATIVO', 3, 2, 'GIRIA'),
('POSITIVO', 2, 1, 'EXPLICACAO'),
('POSITIVO', 3, 2, 'EXPLICACAO'),
('NEGATIVO', 1, 3, 'EXPLICACAO');

-- Confirmar que as tabelas foram criadas e populadas
SELECT 'Banco de dados sistema_girias criado com sucesso!' as mensagem;
SELECT COUNT(*) as total_usuarios FROM usuario;
SELECT COUNT(*) as total_girias FROM giria;
SELECT COUNT(*) as total_explicacoes FROM explicacao;
SELECT COUNT(*) as total_votos FROM voto;
