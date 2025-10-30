#!/bin/bash

# Script para iniciar o sistema completo do Protocolo de Duelo com NGINX como gateway
echo "=== Protocolo de Duelo - Iniciar Sistema Completo (com NGINX) ==="

# Diretório base do projeto
PROJECT_DIR="/home/lucas/Documentos/dev/projects/dueling-protocol"

# Verificar se o diretório do projeto existe
if [ ! -d "$PROJECT_DIR" ]; then
    echo "Erro: Diretório do projeto não encontrado em $PROJECT_DIR"
    exit 1
fi

# Navegar para o diretório do projeto
cd "$PROJECT_DIR"

# Parar quaisquer serviços em execução
echo "Parando serviços antigos..."
./scripts/deploy/stop_all_services.sh 2>/dev/null || true
sleep 5

# Compilar o projeto
echo "Compilando o projeto..."
mvn clean package -DskipTests

# Navegar para o diretório docker e iniciar o sistema com docker-compose (usando NGINX como gateway)
echo "Iniciando sistema com NGINX como gateway..."
cd docker

# Iniciar o sistema com docker-compose
docker compose up --build -d

# Aguardar os serviços iniciarem com tempo aumentado para acomodar inicialização completa
echo "Aguardando serviços iniciarem (isso pode levar até 2 minutos)..."
sleep 120

# Verificar se os serviços estão rodando
if docker compose ps | grep -q "nginx-gateway.*Up.*healthy"; then
    echo ""
    echo "=== Sistema iniciado com sucesso ==="
    echo "NGINX Gateway: http://localhost:8080"
    echo "WebSocket: ws://localhost:8080/ws"
    echo ""
    echo "Servidores disponíveis:"
    echo "  server-1: http://localhost:8081"
    echo "  server-2: http://localhost:8082"
    echo "  server-3: http://localhost:8083"
    echo "  server-4: http://localhost:8084"
    echo ""
    echo "Clientes Docker já estão rodando e conectados automaticamente."
    echo ""
    echo "Para visualizar logs: docker compose logs -f"
    echo "Para parar todos os serviços, execute: ./stop_all_services.sh"
    echo ""
    echo "Para uso em ambiente multi-PC (distribuído):"
    echo "1. Exponha as portas necessárias (8080, 8081-8084) no firewall"
    echo "2. Use o IP externo desta máquina para conexão de clientes remotos"
    echo "3. Configure os clientes remotos para apontar para este IP"
else
    echo ""
    echo "=== Erro ao iniciar o sistema ==="
    echo "Verificando status dos serviços:"
    docker compose ps
    echo ""
    echo "Verifique os logs com: docker compose logs"
    echo "Se o NGINX Gateway não estiver saudável, pode estar aguardando servidores adicionais."
    echo "Os servidores podem levar mais tempo para iniciar completamente."
    echo "Tente verificar o status novamente com: docker compose ps"
fi