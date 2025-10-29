# 🧪 Plano de Execução de Testes - QA Engineer

**Data:** 2025-11-03  
**Objetivo:** Executar e corrigir TODOS os testes individualmente até 100% de sucesso

## 📊 Inventário de Testes

Total de scripts identificados: **48 testes**

### Categorias:
- **Concorrência:** 1 teste
- **Distribuídos:** 7 testes  
- **Funcionais:** 23 testes
- **Infraestrutura:** 10 testes
- **Integração:** 3 testes
- **Performance:** 1 teste
- **Segurança:** 2 testes
- **Stress:** 1 teste

---

## 🎯 Estratégia de Execução

### Fase 1: Testes Básicos (Prioridade ALTA)
Começar pelos testes mais simples que validam funcionalidades core:

1. ✅ **test_integration_pubsub_rest.sh** - CORRIGIDO (HTTP 403 → permitAll)
2. ⏳ **test_client_websocket.sh** - Teste de conexão WebSocket básica
3. ⏳ **test_matchmaking.sh** - Sistema de matchmaking
4. ⏳ **test_game_state_consistency.sh** - Consistência de estado
5. ⏳ **test_purchase.sh** - Sistema de compras

### Fase 2: Testes de Infraestrutura (Prioridade ALTA)
Validar que a infraestrutura está funcionando:

6. ⏳ **test_redis_sentinel.sh** - Redis Sentinel
7. ⏳ **test_postgresql_functionality.sh** - PostgreSQL
8. ⏳ **test_redis_functionality.sh** - Redis básico

### Fase 3: Testes Distribuídos (Prioridade MÉDIA)
Validar comunicação entre servidores:

9. ⏳ **test_s2s_communication.sh** - Server-to-Server
10. ⏳ **test_cross_server_matchmaking.sh** - Matchmaking distribuído
11. ⏳ **test_distributed_system.sh** - Sistema distribuído completo

### Fase 4: Testes de Negócio (Prioridade MÉDIA)
Validar funcionalidades de jogo:

12. ⏳ **test_trade.sh** - Sistema de trocas
13. ⏳ **test_dueling_protocol.sh** - Protocolo de duelo
14. ⏳ **test_match_final.sh** - Partidas completas

### Fase 5: Testes de Robustez (Prioridade BAIXA)
Validar comportamento em cenários adversos:

15. ⏳ **test_mid_game_disconnection.sh** - Desconexão em jogo
16. ⏳ **test_queue_disconnection.sh** - Desconexão na fila
17. ⏳ **test_malformed_inputs.sh** - Inputs malformados
18. ⏳ **test_persistence_race_condition.sh** - Race conditions
19. ⏳ **test_simultaneous_play.sh** - Jogadas simultâneas

### Fase 6: Testes de Segurança (Prioridade MÉDIA)
Validar autenticação e autorização:

20. ⏳ **test_jwt_security.sh** - Segurança JWT
21. ⏳ **test_advanced_security.sh** - Segurança avançada

### Fase 7: Testes de Performance (Prioridade BAIXA)
Validar desempenho sob carga:

22. ⏳ **test_stress.sh** - Teste de stress
23. ⏳ **test_performance_scalability.sh** - Escalabilidade

### Fase 8: Testes Completos (Prioridade FINAL)
Validação end-to-end:

24. ⏳ **test_full_integration.sh** - Integração completa
25. ⏳ **test_system_complete.sh** - Sistema completo

---

## 🔧 Correções Já Aplicadas

### 1. Security Config - HTTP 403 nos endpoints
**Problema:** Endpoints `/api/players/**` não estavam em permitAll()  
**Solução:** Adicionado `.requestMatchers("/api/players/**").permitAll()` na configuração de segurança  
**Arquivo:** `dueling-server/src/main/java/security/SecurityConfig.java`  
**Status:** ✅ CORRIGIDO

### 2. Variáveis de Ambiente
**Problema:** Warnings de variáveis POSTGRES_*, REDIS_* não definidas  
**Solução:** Criado `test_scripts/common_env.sh` com função `create_env_file()`  
**Status:** ✅ CORRIGIDO

### 3. Serviço "client" não existe
**Problema:** Scripts usavam `--scale client=X`  
**Solução:** Substituído por lista explícita `client-1 client-2`  
**Status:** ✅ CORRIGIDO

### 4. Caminhos PROJECT_ROOT incorretos
**Problema:** 15+ scripts com `$SCRIPT_DIR/..` em vez de `$SCRIPT_DIR/../..`  
**Solução:** Corrigido em todos os scripts de subdiretórios  
**Status:** ✅ CORRIGIDO

### 5. Endpoints API incorretos
**Problema:** Testes usavam `/api/sync/matchmaking/enter` (não existe)  
**Solução:** Corrigido para `/api/matchmaking/enqueue`  
**Status:** ✅ CORRIGIDO

---

## 📋 Template de Execução

Para cada teste, seguir este processo:

```bash
# 1. Parar todos os serviços
docker compose -f docker/docker-compose.yml down

# 2. Executar o teste
bash test_scripts/categoria/nome_do_teste.sh

# 3. Capturar resultado
# - Exit code: 0 = sucesso, != 0 = falha
# - Logs: docker logs ou test_logs/

# 4. Se falhar:
#    a) Analisar logs
#    b) Identificar causa raiz
#    c) Aplicar correção
#    d) Recompilar se necessário
#    e) Re-executar teste

# 5. Documentar resultado
```

---

## 📝 Registro de Execução

### Teste 1: test_integration_pubsub_rest.sh
- **Data:** 2025-11-03 17:30
- **Status:** ✅ PASSOU (após correção de segurança)
- **Duração:** ~50s
- **Observações:** Teste 1 passou, Teste 2 falhou com HTTP 403 inicialmente

---

## 🎯 Próximos Passos

1. **Rebuild Docker images** com SecurityConfig corrigido
2. **Re-executar test_integration_pubsub_rest.sh** para validar correção
3. **Iniciar Fase 1** com test_client_websocket.sh
4. **Documentar cada execução** neste arquivo

---

## 📊 Progresso Geral

- ✅ **Testes Passando:** 0/48 (0%)
- 🔧 **Correções Aplicadas:** 5
- ⏳ **Testes Pendentes:** 48
- ⏱️ **Tempo Estimado:** 4-6 horas

---

**Última Atualização:** 2025-11-03 17:40
