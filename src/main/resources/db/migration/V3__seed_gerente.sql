-- Gerente inicial (bootstrap). Email e hash BCrypt vêm de placeholders Flyway
-- resolvidos em tempo de execução a partir de app.seed.gerente.* (FlywaySeedConfig).
INSERT INTO users (nome, email, senha, role, ativo)
VALUES ('Gerente Padrão', '${gerente_email}', '${gerente_password_hash}', 'GERENTE', TRUE);
