# Relatório Técnico - Dueling Protocol

## 1. Introdução

O **Dueling Protocol** é um sistema distribuído de jogo de cartas multiplayer desenvolvido para TEC502 - MI Concorrência e Conectividade. Implementa arquitetura distribuída com alta disponibilidade, tolerância a falhas e escalabilidade horizontal.

## 2. Arquitetura

Sistema composto por múltiplos servidores (peer-to-peer) coordenados via Redis:
- 4+ servidores simultâneos
- NGINX Gateway (load balancer)
- PostgreSQL (persistência)
- Redis Sentinel (alta disponibilidade)

## 3. Funcionalidades

- ✓ Autenticação JWT multi-servidor
- ✓ Matchmaking cross-server (3 estratégias)
- ✓ Sistema de duelos em turnos
- ✓ Loja com controle de estoque distribuído
- ✓ Sistema completo de trocas de cartas
- ✓ Comunicação S2S (Server-to-Server)

## 4. Concorrência

Soluções implementadas:
- Locks distribuídos (Redisson)
- Sistema de cooldown (5s)
- Sincronização via Redis Pub/Sub
- Transações ACID (PostgreSQL)

## 5. Testes

31 scripts automatizados:
- 18 testes funcionais
- 4 testes de infraestrutura
- 2 testes de concorrência
- 1 teste de segurança

**Resultado**: 100% de sucesso

## 6. Estatísticas

| Métrica | Valor |
|---------|-------|
| Linhas Java | 13.171 |
| Classes | 45 |
| Scripts | 43 |
| Commits | 62+ |

## 7. Uso do Menu

**Single PC**: 
```
./menu.sh → opção 2 → opção 10
```

**Multi-PC**:
```
PC1: ./menu.sh → opção 1 (Docker)
PC2: ./menu.sh → opção 5 (Cliente)
```

## 8. Conclusão

Sistema **100% funcional** com todos objetivos atingidos.

---
*UEFS - 2025*
