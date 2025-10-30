# Componentes da Arquitetura Distribuída

## Eleição de Líder

A eleição de líder é um componente fundamental em sistemas distribuídos para garantir que haja um coordenador responsável por decisões críticas. No contexto do jogo de cartas Dueling Protocol:

- **Mecanismo de Eleição**: O sistema implementa um mecanismo simplificado de eleição de líder baseado em locks distribuídos do Redisson. Cada servidor compete por um lock distribuído no Redis, e o servidor que adquirir o lock se torna o líder. Esta abordagem é mais simples que o algoritmo Raft completo, mas eficaz para o contexto do jogo.

- **Coordenação**: O líder eleito coordena operações críticas como o gerenciamento distribuído de estoque de pacotes, controle de pareamento global e limpeza de locks órfãos. Ele atua como o ponto de coordenação para operações que requerem exclusividade, como compras de pacotes.

- **Falhas e Nova Eleição**: Quando o líder falha (por timeout ou falha de sistema), outro servidor pode adquirir o lock distribuído e se tornar o novo líder. O sistema implementa verificação periódica para detecção de falhas e nova eleição automática, garantindo continuidade do serviço.

- **Controle de Concorrência**: O líder é responsável por gerenciar locks distribuídos para operações críticas, coordenando acesso concorrente a recursos compartilhados como estoque de cartas.

## Consenso

O mecanismo de consenso é essencial para garantir que todas as réplicas do sistema concordem sobre o estado global:

- **Locks Distribuídos**: O sistema utiliza locks distribuídos baseados no Redisson para garantir que decisões críticas sejam tomadas de forma serializada. Cada operação que afeta dados compartilhados (como compra de pacotes) adquire um lock distribuído antes de executar, garantindo consistência.

- **Operações Atômicas**: O sistema implementa operações atômicas para transações críticas, especialmente para operações de estoque. O uso de scripts Lua no Redis garante que operações de modificação de estoque sejam executadas de forma atômica.

- **Sincronização entre Servidores**: Servidores sincronizam operações críticas usando o lock distribuído para garantir que apenas uma operação de compra seja processada por vez, evitando problemas de concorrência.

- **Tolerância a Concorrência**: O sistema tolera operações concorrentes graças ao uso de locks distribuídos, que serializam operações críticas mantendo a disponibilidade do sistema para operações não críticas.

## Gateway

O gateway atua como ponto de entrada único para o sistema distribuído, gerenciando o tráfego e as conexões:

- **Balanceamento de Carga**: O gateway distribui as requisições dos clientes entre os diferentes servidores de jogo com base em critérios como carga atual e disponibilidade. O Nginx implementa balanceamento de carga para distribuir conexões WebSocket entre múltiplos servidores.

- **Roteamento de Conexões WebSocket**: O gateway encaminha conexões WebSocket para os servidores de jogo apropriados, mantendo persistência de conexão para que cada jogador permaneça conectado ao mesmo servidor durante sua sessão.

- **Autenticação e Autorização**: Todas as requisições passam pelo gateway, onde são verificados tokens JWT e permissões. O gateway valida credenciais antes de permitir o estabelecimento de conexões WebSocket.

- **Terminação de Conexões**: O gateway gerencia conexões WebSocket com clientes, lidando com handshake, manutenção de conexão e encerramento adequado. Ele atua como intermediário entre os clientes e os servidores de aplicação.

## Redis

Redis é utilizado como armazenamento em memória para dados críticos do sistema, fornecendo alta performance e funcionalidades avançadas:

- **Armazenamento de Estoque**: Redis armazena o estoque de pacotes de cartas em um mapa hash (HSET/HGET), permitindo acesso rápido e modificações atômicas. O estoque é compartilhado entre todos os servidores do sistema.

- **Locks Distribuídos**: Redis é utilizado para implementar locks distribuídos via Redisson que previnem operações concorrentes em recursos críticos, como a aquisição de pacotes de cartas por múltiplos jogadores simultaneamente.

- **Pub/Sub para Comunicação**: O recurso de publicação e subscrição do Redis é utilizado para comunicação entre servidores e notificação de eventos do sistema, permitindo troca de mensagens em tempo real entre componentes.

- **Scripts Lua para Operações Atômicas**: O sistema implementa scripts Lua no Redis para garantir operações atômicas de modificação de estoque, evitando race conditions quando múltiplos servidores tentam modificar o mesmo recurso.

## PostgreSQL

PostgreSQL fornece armazenamento persistente e confiável para dados do sistema, com suporte a transações ACID:

- **Dados de Usuário**: PostgreSQL armazena informações seguras de contas e perfis de jogadores, incluindo credenciais (criptografadas), estatísticas, progresso e configurações. O banco é utilizado para dados que requerem persistência durável.

- **Histórico de Partidas**: O banco registra persistentemente todas as partidas encerradas, permitindo análise de estatísticas, histórico de duelos e auditoria de ações. Esses dados são essenciais para sistemas de ranking e análise de desempenho.

- **Dados Persistentes**: PostgreSQL mantém dados que precisam de persistência durável, como informações de usuários, histórico de transações e dados de perfil que devem sobreviver a reinicializações do sistema.

- **Transações ACID**: O PostgreSQL garante que operações críticas sejam executadas com propriedades ACID (Atomicidade, Consistência, Isolamento e Durabilidade) para dados persistentes, prevenindo inconsistências de dados em operações que afetam o estado durável.

## Nginx

Nginx atua como servidor proxy reverso e balanceador de carga, otimizando o acesso ao sistema:

- **Proxy Reverso WebSocket**: Nginx encaminha requisições WebSocket para os servidores de aplicação apropriados, ocultando a complexidade da infraestrutura distribuída dos clientes e fornecendo um ponto de entrada único.

- **Balanceamento de Carga**: O Nginx distribui conexões WebSocket entre múltiplos servidores de jogo, garantindo distribuição equilibrada da carga e alta disponibilidade do sistema.

- **Saúde do Servidor**: Nginx implementa health checks para verificar a disponibilidade dos servidores de aplicação e não encaminha tráfego para servidores inativos ou com problemas.

- **Controle de Conexões**: Nginx gerencia conexões WebSocket, lidando com aspectos como timeout de conexão, limites de concorrência e encerramento adequado de conexões.