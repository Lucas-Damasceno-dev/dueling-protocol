-- Script de inicialização do PostgreSQL
-- Criação do usuário user se não existir
DO
$do$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE rolname = 'user') THEN

      CREATE ROLE "user" LOGIN PASSWORD 'password';
   END IF;
END
$do$;

-- Criação do banco de dados dueling_db se não existir
SELECT 'CREATE DATABASE dueling_db OWNER user'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'dueling_db');

-- Precisamos executar o comando acima separadamente
-- Vamos usar um bloco DO para criar o banco de dados se não existir
DO
$do$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'dueling_db') THEN
      CREATE DATABASE dueling_db OWNER "user";
   END IF;
END
$do$;

-- Após a criação do banco de dados, podemos inserir dados iniciais
-- Esta parte será executada após a inicialização do banco de dados pelo Spring Boot
-- Para isso, precisamos usar um script de migração Flyway ou Liquibase
-- Ou inserir os dados diretamente no banco de dados após a inicialização

-- Vamos criar um script separado para inserir dados de teste
-- Este script será executado pelo docker-compose após a inicialização do banco de dados