-- Script para inserir dados de teste no banco de dados
-- Este script deve ser executado após a inicialização completa do banco de dados e da aplicação

-- Inserir players de teste
INSERT INTO players (id, nickname, coins, health_points, upgrade_points, base_attack, base_defense, base_mana, player_race, player_class) VALUES
('player1', 'Player 1', 1000, 100, 0, 10, 5, 50, 'Human', 'Warrior'),
('player2', 'Player 2', 1000, 100, 0, 10, 5, 50, 'Elf', 'Mage'),
('player3', 'Player 3', 1000, 100, 0, 10, 5, 50, 'Dwarf', 'Paladin'),
('player4', 'Player 4', 1000, 100, 0, 10, 5, 50, 'Orc', 'Berserker')
ON CONFLICT (id) DO NOTHING;

-- Inserir usuários de teste (senhas serão criptografadas pela aplicação)
-- Este comando é apenas para referência, pois as senhas devem ser criptografadas
-- INSERT INTO users (username, password, player_id) VALUES
-- ('client1', '$2a$10$hashedPassword1', 'player1'),
-- ('client2', '$2a$10$hashedPassword2', 'player2'),
-- ('client3', '$2a$10$hashedPassword3', 'player3'),
-- ('client4', '$2a$10$hashedPassword4', 'player4')
-- ON CONFLICT (username) DO NOTHING;