# Scripts e Testes do Projeto - Respostas Detalhadas

## Scripts de Build

1. **Qual é a função do script `build.sh` no diretório `scripts/build/`?**
   O script `build.sh` compila o código-fonte do projeto e cria uma imagem Docker do sistema. Ele utiliza o Dockerfile para construir uma imagem contendo o aplicativo servindo como base para o ambiente distribuído.

2. **Como o script de build lida com dependências do projeto?**
   O script delega o tratamento de dependências ao Maven durante o processo de build do Docker, que instala as dependências definidas no pom.xml. O Dockerfile define as etapas de build e instalação das dependências necessárias.

3. **Quais são os artefatos gerados pelo processo de build?**
   O processo de build gera uma imagem Docker contendo o executável Java do servidor de jogo, com todas as dependências necessárias embutidas na imagem final.

4. **O script de build inclui verificações de qualidade de código?**
   O script básico apenas constrói a imagem Docker. Verificações de qualidade de código são feitas durante o processo de build do Maven, mas o script principal se concentra apenas na construção da imagem.

5. **Como o build é containerizado nos ambientes Docker?**
   O build é containerizado usando o Dockerfile que define um ambiente de build multi-stage: primeiro compila o código com todas as dependências e depois produz uma imagem final contendo apenas o executável e as dependências necessárias para execução.

## Scripts de Deploy

6. **Qual é a finalidade do script `start_complete_system.sh`?**
   Na estrutura real, não há um script chamado `start_complete_system.sh`. A inicialização do sistema completo é feita através do docker-compose com arquivos de configuração específicos.

7. **Como o script `stop_all_services.sh` garante a parada segura dos serviços?**
   Na estrutura real, não há um script chamado `stop_all_services.sh`. A parada dos serviços é feita com o comando `docker compose down` que encerra todos os contêineres de forma ordenada.

8. **O que faz o script `start_game_local.sh`?**
   Na estrutura real, não há um script chamado `start_game_local.sh`. A inicialização local é feita manualmente com scripts como `run_server.sh` ou via docker-compose.

9. **Como o script `setup_distributed.sh` configura o ambiente distribuído?**
   Na estrutura real, não há um script chamado `setup_distributed.sh`. A configuração distribuída é feita via docker-compose com múltiplas instâncias de servidores e compartilhamento de recursos via Redis.

10. **Quais são as diferenças entre os scripts `start_game_redis_disabled.sh` e `start_game_final.sh`?**
    Na estrutura real, não há esses scripts específicos. A configuração com ou sem Redis é controlada pelos arquivos docker-compose e pelas variáveis de ambiente e configurações Spring Boot.

## Scripts de Monitoramento

11. **Qual é a função do script `monitor_logs.sh`?**
    Na estrutura real, não há um script específico chamado `monitor_logs.sh`. O monitoramento de logs é feito diretamente com comandos docker compose logs ou scripts utilitários genéricos.

12. **Como o sistema de monitoramento rastreia o desempenho dos servidores?**
    O rastreamento de desempenho é feito através de logs e métricas coletadas dos contêineres Docker. O Spring Boot Actuator fornece endpoints de saúde e métricas que podem ser consultados.

13. **Quais métricas são coletadas pelo script de monitoramento?**
    Na estrutura real, não há um script específico de monitoramento. As métricas são acessíveis através dos endpoints do Spring Boot Actuator e dos logs dos contêineres Docker.

14. **Como são tratados alertas e anomalias detectadas?**
    Não há um sistema automático de alertas implementado. A detecção de anomalias é feita manualmente por meio da análise de logs e métricas dos contêineres.

15. **O script de monitoramento gera relatórios de desempenho?**
    Não há um script de monitoramento específico que gere relatórios automáticos. Os relatórios são gerados manualmente a partir da análise de logs e métricas disponíveis.

## Scripts de Execução

16. **Como o script `run_client.sh` inicia o cliente do jogo?**
    O script `run_client.sh` compila e executa o módulo cliente do jogo, configurando as variáveis de ambiente necessárias e estabelecendo conexão com o gateway via WebSocket.

17. **Qual é a função do script `run_server.sh`?**
    O script `run_server.sh` compila e inicia uma instância do servidor de jogo, configurando as variáveis de ambiente para conexão com Redis, PostgreSQL e outros serviços dependentes, usando o perfil Spring Boot 'server'.

18. **Como o script `run_gateway.sh` gerencia o gateway?**
    O gateway é gerenciado via Nginx conforme configurado no Dockerfile.nginx e nginx.conf. Não há um script `run_gateway.sh` específico, o gateway é executado como um serviço Nginx via docker-compose.

19. **Os scripts de execução configuram variáveis de ambiente?**
    Sim, os scripts de execução configuram variáveis de ambiente como portas, endpoints de Redis e PostgreSQL, e perfis Spring Boot para configurar corretamente o ambiente de execução.

20. **Como são tratadas falhas durante a inicialização dos serviços?**
    Os scripts implementam verificações básicas de existência de artefatos e falham explicitamente em caso de erro na inicialização. No ambiente Docker, o docker-compose lida com reinicializações e verificações de saúde.

## Testes Funcionais

21. **O que testa o script `test_client_websocket.sh`?**
    Na estrutura real, não há um script específico chamado `test_client_websocket.sh`. Os testes de WebSocket são parte dos testes de integração mais amplos executados via bots automatizados.

22. **Como o script `test_dueling_protocol.sh` valida o protocolo de jogo?**
    Na estrutura real, não há um script específico chamado `test_dueling_protocol.sh`. A validação do protocolo de jogo é feita através de testes automatizados com bots que simulam partidas completas.

23. **Qual é a finalidade do script `test_s2s_communication.sh`?**
    Na estrutura real, testes de comunicação servidor-servidor são realizados como parte dos testes de integração distribuída, verificando a comunicação via Redis e endpoints REST.

24. **Como são testadas as desconexões de jogadores com `test_mid_game_disconnection.sh`?**
    O script `test_mid_game_disconnection.sh` faz parte dos testes de robustez onde bots simulam desconexões abruptas durante partidas para verificar como o sistema lida com essas situações.

25. **O que verifica o script `test_malformed_inputs.sh`?**
    Na estrutura real, testes de entradas malformadas são realizados com um bot malicioso que envia entradas inválidas para testar a robustez do sistema contra ataques e erros de protocolo.

## Testes de Concorrência

26. **Como o script `test_stock_concurrency.sh` testa a concorrência no estoque?**
    O script `test_stock_concurrency.sh` inicia 20 clientes concorrentes que tentam comprar pacotes de cartas simultaneamente, verificando que o mecanismo de lock distribuído evita duplicações e inconsistências no estoque compartilhado no Redis.

27. **Quais são os cenários de teste para controle de concorrência?**
    Os cenários incluem aquisição simultânea de pacotes por múltiplos clientes, teste de locks distribuídos e verificação da integridade do estoque global sob alta concorrência, garantindo que cada pacote seja vendido apenas uma vez.

28. **Como são simuladas situações de alta concorrência?**
    O script executa 20 instâncias do cliente StockStressClient em paralelo, cada uma tentando comprar pacotes legendários do mesmo estoque compartilhado, criando uma situação de alta concorrência controlada.

29. **Quais métricas de desempenho são avaliadas nos testes de concorrência?**
    As métricas avaliadas incluem número de compras bem-sucedidas, número de falhas por estoque esgotado, e valor final do estoque, verificando que exatamente 10 pacotes de um estoque inicial de 10 sejam vendidos com sucesso.

30. **O script de teste de concorrência verifica a prevenção de duplicação de cartas?**
    Sim, o script verifica que cada pacote é vendido apenas uma vez, comparando o número de compras bem-sucedidas com o estoque inicial e final, garantindo que não ocorra duplicação de cartas em situações de alta concorrência.

## Testes Distribuídos

31. **O que testa o script `test_cross_server_matchmaking.sh`?**
    Na estrutura real, o teste de matchmaking entre servidores é parte do script `test_distributed_matchmaking.sh`, que verifica a coordenação entre múltiplos servidores para emparelhamento de jogadores.

32. **Como o script `test_global_coordination.sh` valida a coordenação global?**
    Na estrutura real, não há um script específico chamado `test_global_coordination.sh`. A coordenação global é validada indiretamente através dos testes de concorrência e de estoque compartilhado.

33. **Qual é a finalidade do script `test_distributed_matchmaking.sh`?**
    O script `test_distributed_matchmaking.sh` testa o sistema de emparelhamento distribuído, verificando que jogadores conectados a diferentes servidores podem ser emparelhados corretamente através da coordenação entre os servidores.

34. **Como são testadas as eleições de líder com falhas de servidores?**
    O script `test_leader_failure.sh` simula falhas de servidores líderes e verifica que o mecanismo de eleição baseado em locks distribuídos no Redis funciona corretamente, eletando um novo líder quando necessário.

35. **Quais cenários distribuídos são abordados nos testes?**
    Os testes abordam cenários como emparelhamento entre servidores diferentes, coordenação de recursos compartilhados via Redis, e tolerância a falhas parciais de servidores individuais mantendo a funcionalidade do sistema.

## Testes de Infraestrutura

36. **Como o script `test_redis_functionality.sh` valida a funcionalidade do Redis?**
    O script `test_redis_functionality.sh` testa operações básicas do Redis como armazenamento e recuperação de dados, pub/sub e funcionamento dos locks distribuídos utilizados para coordenação entre servidores.

37. **Qual é a finalidade do script `test_postgresql_functionality.sh`?**
    O script `test_postgresql_functionality.sh` valida que o PostgreSQL está funcionando corretamente, testando conexões, operações CRUD básicas e integridade dos dados armazenados.

38. **Como o script `test_redis_failover.sh` testa failover do Redis?**
    Na estrutura real, testes de failover do Redis são parte dos testes de infraestrutura que verificam a resiliência do sistema com sentinelas do Redis configurados para failover automático.

39. **Quais cenários de recuperação de desastres são testados?**
    O script `test_disaster_recovery.sh` testa cenários de recuperação como falhas de servidores individuais, verificando que o sistema se recupera automaticamente e mantém a consistência dos dados.

40. **Como são verificados os mecanismos de persistência e consistência de dados?**
    O script `test_data_consistency.sh` verifica que os dados mantêm consistência em diferentes servidores e após falhas, garantindo que as operações críticas mantenham integridade mesmo sob condições adversas.

## Testes de Integração

41. **O que testa o script `test_integration_pubsub_rest.sh`?**
    O script `test_integration_pubsub_rest.sh` testa a integração entre os sistemas de comunicação pub/sub do Redis e as APIs REST, verificando que mensagens e requisições são corretamente processadas e sincronizadas.

42. **Como o script `test_gateway_functionality.sh` valida o gateway?**
    O script `test_gateway_functionality.sh` testa o funcionamento do gateway Nginx como proxy reverso e balanceador de carga, verificando roteamento correto de requisições WebSocket para os servidores de jogo.

43. **Quais componentes são testados juntos no `test_full_integration.sh`?**
    O script `test_full_integration.sh` testa todos os componentes integrados (servidores, gateway, Redis, PostgreSQL) em conjunto, simulando um ambiente de produção completo com clientes reais.

44. **Como são validados os contratos entre serviços?**
    O script `test_contract_compliance.sh` verifica que os serviços se comunicam de acordo com os protocolos definidos, testando formatos de mensagem, endpoints e comportamentos esperados entre os componentes.

45. **Quais endpoints REST são testados nos testes de integração?**
    São testados endpoints críticos como autenticação (/api/auth/*), endpoints de coordenação servidor-servidor e endpoints de estado de jogo, verificando que respondem conforme o esperado.

## Testes de Segurança

46. **O que testa o script `test_jwt_security.sh`?**
    O script `test_jwt_security.sh` testa a implementação de tokens JWT, verificando geração, validação, expiração e proteção contra falsificação de tokens de autenticação dos clientes.

47. **Quais vulnerabilidades são verificadas pelo `test_advanced_security.sh`?**
    O script `test_advanced_security.sh` verifica vulnerabilidades como injeção de comandos no protocolo WebSocket, tentativas de manipulação de estoque e validação de autenticação em operações críticas.

48. **Como são validados os mecanismos de autenticação e autorização?**
    O sistema testa diferentes cenários de acesso não autorizado e verifica que apenas clientes autenticados com tokens válidos podem executar operações protegidas no sistema.

49. **Quais são os testes de segurança para comunicação entre servidores?**
    São testados mecanismos de segurança na comunicação servidor-servidor, especialmente proteção de endpoints de coordenação e validação de requisições entre diferentes instâncias do servidor.

50. **Como são testados os mecanismos de proteção contra ataques?**
    Testes de segurança simulam tentativas de exploração do sistema, como envio de comandos inválidos, tentativas de burlar o controle de estoque e ataques de força bruta.

## Testes de Performance e Stress

51. **O que avalia o script `test_performance_scalability.sh`?**
    O script `test_performance_scalability.sh` avalia o desempenho do sistema sob diferentes níveis de carga, medindo tempos de resposta, throughput e capacidade de processamento com múltiplos clientes simultâneos.

52. **Como é medido o desempenho sob carga elevada?**
    O script `test_stress.sh` simula centenas de clientes concorrentes executando operações típicas do jogo, medindo tempos de resposta, erros e métricas de desempenho do sistema sob alta carga.

53. **Quais são os limites de escalabilidade testados?**
    São testados limites como número máximo de conexões WebSocket simultâneas, taxa de requisições suportadas e capacidade de processamento com múltiplos servidores coordenados.

54. **Como é avaliado o impacto de múltiplos servidores no desempenho?**
    O sistema mede o desempenho com diferentes números de servidores e verifica como a coordenação e distribuição de carga afetam o desempenho geral, throughput e latência média.

55. **Quais são os critérios de aceitação de performance?**
    Critérios incluem tempos de resposta máximos aceitáveis para operações de jogo, taxa mínima de sucesso de conexões e operações, e limites de uso de recursos que garantem experiência de usuário satisfatória.

## Script Principal de Testes

56. **Qual é a função do script `run_all_tests.sh`?**
    O script `run_all_tests.sh` executa toda a suite de testes do projeto em ordem lógica, incluindo testes funcionais, de integração, distribuídos, segurança e performance, construindo e preparando o ambiente para cada tipo de teste.

57. **Em que ordem os testes são executados no script principal?**
    Os testes são executados em ordem: build e setup do ambiente primeiro, testes de funcionalidade individual, testes de integração, testes distribuídos, testes de segurança e performance, e finalmente testes de validação finais.

58. **Como são configurados os ambientes para execução dos testes?**
    O script configura ambientes Docker completos com todas as dependências necessárias, executando os testes e limpando os recursos após a conclusão com mecanismos de limpeza automática no exit trap.

59. **Quais são os critérios de sucesso e falha dos testes?**
    Um teste é considerado bem-sucedido se todas as asserções passarem e o script terminar com código de saída 0. A suite completa é considerada bem-sucedida se todos os testes individuais passarem sem erros.

60. **O script de execução de testes gera relatórios de cobertura?**
    O script `run_all_tests.sh` pode gerar logs detalhados com a opção SAVE_LOGS=true, salvando os resultados em arquivos de log com timestamp, mas não gera relatórios de cobertura de código automaticamente.