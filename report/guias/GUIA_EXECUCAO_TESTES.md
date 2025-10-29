# Guia de Execução de Testes - Dueling Protocol

Este guia explica como executar cada teste do projeto usando o menu interativo `menu.sh`.

## 📋 Pré-requisitos

1. Docker e Docker Compose instalados
2. Maven instalado
3. Java 21 ou superior
4. Acesso ao terminal Linux/MacOS (ou WSL no Windows)

## 🚀 Acesso Rápido via Menu

### Menu Principal

Execute o menu principal:
```bash
./menu.sh
```

O menu principal oferece as seguintes opções de teste:

### Opção 9: Executar Testes
Esta opção abre um submenu com todas as categorias de testes disponíveis.

```bash
./menu.sh
# Selecione: 9) Executar Testes
```

## 🧪 Categorias de Testes

### 1. Testes Funcionais (Opção 1 do submenu de testes)

#### Testes Disponíveis:
- **Matchmaking**: Testa a formação de partidas entre jogadores
- **Trade (Troca)**: Testa o sistema de troca de cartas entre jogadores
- **Purchase (Compra)**: Testa a compra de pacotes de cartas
- **Cross-Server Match**: Testa partidas entre servidores diferentes
- **Cross-Server Trade**: Testa trocas entre servidores diferentes
- **Game State Consistency**: Verifica consistência do estado do jogo
- **Desconexão na Fila**: Testa comportamento quando jogador desconecta na fila
- **Desconexão Durante Partida**: Testa comportamento quando jogador desconecta durante jogo

**Como executar via menu:**
```bash
./menu.sh
# Selecione: 9) Executar Testes
# Selecione: 1) Testes Funcionais
# Escolha o teste específico
```

**Como executar diretamente:**
```bash
# Teste de Matchmaking
bash test_scripts/functional/test_matchmaking.sh

# Teste de Trade
bash test_scripts/functional/test_trade.sh

# Teste de Purchase
bash test_scripts/functional/test_purchase.sh
```

### 2. Testes de Integração (Opção 2 do submenu de testes)

#### Testes Disponíveis:
- **PubSub e REST API**: Testa integração entre sistema de mensagens e API REST
- **Gateway Functionality**: Testa funcionalidade do gateway NGINX
- **Full Integration**: Teste de integração completo do sistema

**Como executar via menu:**
```bash
./menu.sh
# Selecione: 9) Executar Testes
# Selecione: 2) Testes de Integração
# Escolha o teste específico
```

**Como executar diretamente:**
```bash
# Teste PubSub e REST
bash test_scripts/integration/test_integration_pubsub_rest.sh

# Teste Gateway
bash test_scripts/integration/test_gateway_functionality.sh

# Teste Full Integration
bash test_scripts/integration/test_full_integration.sh
```

### 3. Testes Distribuídos (Opção 3 do submenu de testes)

#### Testes Disponíveis:
- **Distributed Matchmaking**: Testa matchmaking em ambiente distribuído
- **Cross-Server Matchmaking**: Testa matchmaking entre servidores
- **Leader Failure**: Testa recuperação de falha do líder
- **Global Coordination**: Testa coordenação global entre servidores
- **Purchase Global**: Testa compras em ambiente distribuído

**Como executar via menu:**
```bash
./menu.sh
# Selecione: 9) Executar Testes
# Selecione: 3) Testes Distribuídos
# Escolha o teste específico
```

**Como executar diretamente:**
```bash
# Teste Distributed Matchmaking
bash test_scripts/distributed/test_distributed_matchmaking.sh

# Teste Cross-Server
bash test_scripts/distributed/test_cross_server_matchmaking.sh
```

### 4. Testes de Concorrência (Opção 4 do submenu de testes)

#### Testes Disponíveis:
- **Stock Concurrency**: Testa acesso concorrente ao estoque de cartas

**Como executar via menu:**
```bash
./menu.sh
# Selecione: 9) Executar Testes
# Selecione: 4) Testes de Concorrência
```

**Como executar diretamente:**
```bash
bash test_scripts/concurrency/test_stock_concurrency.sh
```

### 5. Testes de Performance (Opção 5 do submenu de testes)

#### Testes Disponíveis:
- **Stress Test**: Teste de estresse do sistema

**Como executar via menu:**
```bash
./menu.sh
# Selecione: 9) Executar Testes
# Selecione: 5) Testes de Performance
```

### 6. Testes de Segurança (Opção 6 do submenu de testes)

#### Testes Disponíveis:
- **Malformed Inputs**: Testa resistência a entradas malformadas
- **Malicious Bot**: Testa resistência a bots maliciosos

**Como executar via menu:**
```bash
./menu.sh
# Selecione: 9) Executar Testes
# Selecione: 6) Testes de Segurança
```

## 🎯 Testes Críticos (TRADE + MATCH + PURCHASE)

Para executar especificamente os três testes críticos mencionados:

### Via Menu Rápido:
```bash
./menu.sh
# Opção 34: Executar Testes Críticos (Trade + Match + Purchase)
```

### Via Scripts Diretos:
```bash
# 1. Teste de Trade (Troca)
bash test_scripts/functional/test_trade.sh

# 2. Teste de Match (Partida)
bash test_scripts/functional/test_matchmaking.sh

# 3. Teste de Purchase (Compra)
bash test_scripts/functional/test_purchase.sh
```

## 🏗️ Compilação e Build

Antes de executar testes, você pode precisar compilar o projeto:

**Via Menu:**
```bash
./menu.sh
# Selecione: 33) Build e Deploy
# Selecione: 1) Build Completo (Clean + Package)
```

**Via Comando Direto:**
```bash
mvn clean package -DskipTests
```

## 🐳 Gerenciamento de Containers Docker

### Iniciar Sistema Completo
**Via Menu:**
```bash
./menu.sh
# Selecione: 1) Iniciar Sistema Completo
```

### Parar Sistema
**Via Menu:**
```bash
./menu.sh
# Selecione: 2) Parar Sistema
```

### Ver Logs
**Via Menu:**
```bash
./menu.sh
# Selecione: 7) Ver Logs
```

## 📊 Interpretando Resultados

### Testes Bem-Sucedidos
- Mensagens com ✓ ou "SUCCESS"
- Código de saída 0
- Logs sem erros críticos

### Testes com Falha
- Mensagens com ✗ ou "FAILURE"
- Código de saída diferente de 0
- Stack traces ou mensagens de erro nos logs

### Logs de Teste
Os logs dos testes são salvos em:
```
test_logs/
├── functional/
├── integration/
├── distributed/
└── ...
```

## 🔍 Troubleshooting

### Problema: Containers não iniciam
**Solução:**
```bash
./menu.sh
# Selecione: 2) Parar Sistema
# Depois: 1) Iniciar Sistema Completo
```

### Problema: Portas em uso
**Solução:**
```bash
# Verificar portas em uso
sudo lsof -i :8080
sudo lsof -i :5432
sudo lsof -i :6379

# Parar processos se necessário
./menu.sh
# Selecione: 2) Parar Sistema
```

### Problema: Erros de compilação
**Solução:**
```bash
./menu.sh
# Selecione: 33) Build e Deploy
# Selecione: 1) Build Completo
```

### Problema: Banco de dados com dados antigos
**Solução:**
```bash
./menu.sh
# Selecione: 2) Parar Sistema
# Selecione: 3) Limpar Dados
# Selecione: 1) Iniciar Sistema Completo
```

## 📝 Notas Importantes

1. **Ordem de Execução**: Sempre compile o projeto antes de executar testes
2. **Isolamento**: Cada teste inicia e para seus próprios containers
3. **Tempo**: Alguns testes podem levar vários minutos para completar
4. **Limpeza**: Os testes fazem limpeza automática dos containers ao finalizar
5. **Logs**: Sempre verifique os logs em caso de falha

## 🔄 Execução em Sequência

Para executar todos os testes em sequência:

```bash
./menu.sh
# Selecione: 9) Executar Testes
# Selecione: 0) Executar Todos os Testes
```

**ATENÇÃO**: Executar todos os testes pode levar mais de 1 hora!

## 📞 Suporte

Em caso de dúvidas ou problemas:
1. Verifique os logs em `test_logs/`
2. Verifique logs dos containers: `docker compose logs`
3. Consulte a documentação principal no README.md
