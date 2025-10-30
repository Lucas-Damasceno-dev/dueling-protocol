# Requisitos de Avaliação - 30 Perguntas com Respostas sobre o Projeto

## Arquitetura Distribuída e Escalabilidade

1. **Como a solução migrou de uma arquitetura centralizada para distribuída?**
   A migração foi realizada através da implementação de múltiplos servidores de jogo independentes que compartilham recursos globalmente via Redis. Em vez de um único servidor central processando todas as operações, os novos servidores distribuídos utilizam locks distribuídos para coordenar operações críticas como compra de pacotes, mantendo o estoque global compartilhado no Redis.

2. **Quais são os múltiplos servidores de jogos implementados no sistema?**
   O sistema implementa múltiplos servidores de jogo (server-1, server-2, server-3, server-4) configurados via Docker Compose. Cada servidor opera independentemente, mantendo conexões WebSocket com clientes conectados localmente, mas coordena operações críticas como compra de pacotes através de locks distribuídos no Redis.

3. **Descreva os principais aspectos da comunicação entre os componentes do sistema.**
   A comunicação entre componentes se dá principalmente através de WebSocket para comunicação em tempo real cliente-servidor, REST API para autenticação e Nginx como gateway. Para coordenação entre servidores, utiliza-se Redis como armazenamento compartilhado e locks distribuídos via Redisson para sincronização de operações críticas.

4. **Como o sistema lida com a escalabilidade dos componentes distribuídos?**
   O sistema é projetado para escalar horizontalmente adicionando mais instâncias de servidores de jogo que compartilham o mesmo Redis e PostgreSQL. A comunicação é otimizada usando locks distribuídos para operações críticas e armazenamento em Redis para gerenciar recursos compartilhados, minimizando impacto na latência.

5. **Quais os benefícios da arquitetura distribuída em relação à arquitetura anterior?**
   A arquitetura distribuída oferece maior capacidade de processamento paralelo, melhor distribuição da carga entre servidores, e maior tolerância a falhas localizadas. A coordenação é feita via locks distribuídos, permitindo que o sistema continue operando mesmo com alguns servidores offline, mantendo consistência do estoque global.

## Comunicação Servidor-Servidor

6. **Quais endpoints REST foram implementados para comunicação entre servidores?**
   Foram implementados endpoints REST em ServerApiClient e ServerRegistry para coordenação de matchmaking entre servidores. Eles permitem encontrar parceiros de jogo em outros servidores e manter um registro de servidores disponíveis, facilitando o emparelhamento entre jogadores em servidores diferentes.

7. **Como é feita a gestão de recursos compartilhados entre servidores diferentes?**
   A gestão de recursos compartilhados, especialmente o estoque de pacotes de cartas, é feita através de locks distribuídos no Redis. Cada servidor tenta adquirir um lock global antes de executar operações que afetam recursos compartilhados, garantindo exclusividade de acesso e prevenindo inconsistências.

8. **Descreva a implementação de endpoints para estoque de pacotes compartilhado.**
   O estoque de pacotes é gerenciado diretamente no Redis via CardRepository, que fornece métodos atômicos para reduzir o estoque de forma segura. O método claimCard utiliza um script Lua para garantir que a operação de diminuir o estoque seja atômica, evitando race conditions entre servidores concorrentes.

9. **Como é garantida a segurança na comunicação entre servidores?**
   A comunicação entre servidores utiliza autenticação baseada em JWT para operações críticas e mecanismos de segurança implementados no Spring Boot. A comunicação ocorre principalmente através de operações atômicas no Redis e endpoints protegidos com autenticação.

10. **Quais protocolos de comunicação são usados entre servidores?**
    A comunicação entre servidores utiliza HTTP/HTTPS para API REST em operações de matchmaking, e comunicação direta com o Redis para coordenação de recursos compartilhados. Para comunicação em tempo real com os clientes, usa-se WebSocket.

## Comunicação Cliente-Servidor

11. **Qual biblioteca de terceiros foi utilizada para o modelo publisher-subscriber?**
    Foi utilizada a biblioteca Redisson com o modelo publisher-subscriber do Redis para comunicação entre servidores e notificação de eventos do jogo. O modelo pub/sub é implementado em RedisEventManager para enviar mensagens aos clientes conectados.

12. **Como é justificada a escolha da biblioteca publisher-subscriber?**
    A escolha do Redis para o modelo publisher-subscriber se justifica pela necessidade de comunicação eficiente e em tempo real entre múltiplos servidores e clientes. O Redis oferece alta performance, persistência opcional e mecanismos robustos de pub/sub que se integram bem com o sistema de locks distribuídos.

13. **Quais são as vantagens do modelo publisher-subscriber em relação a outros modelos?**
    As vantagens incluem comunicação assíncrona, desacoplamento entre produtores e consumidores de eventos, escalabilidade horizontal, e eficiência para broadcasting de mensagens para múltiplos clientes. Isso é ideal para notificação de eventos de jogo em tempo real.

14. **Como é estabelecida a conexão entre clientes e servidores?**
    A conexão é estabelecida através de handshake WebSocket via Nginx Gateway, seguido de autenticação JWT. Após autenticado, o cliente se comunica diretamente com o servidor de jogo ao qual foi roteado, recebendo mensagens e enviando comandos em tempo real.

15. **Como o sistema lida com a desconexão de clientes?**
    O sistema detecta desconexões através do fechamento da conexão WebSocket, atualiza o estado do jogador e limpa sessões de jogo em andamento. Jogadores em partidas são tratados como desistindo, e suas sessões são removidas do sistema de matchmaking.

## Gerenciamento Distribuído de Estoque

16. **Como é implementada a distribuição de cartas únicas entre servidores?**
    A distribuição é implementada através do CardRepository que utiliza o Redis para armazenar o estoque global de cartas. Cada tentativa de obtenção de carta utiliza um script Lua atômico para garantir que a carta seja reduzida em uma única operação, garantindo unicidade mesmo com servidores concorrentes.

17. **Qual mecanismo evita a duplicação de cartas durante o acesso concorrente?**
    O mecanismo utiliza scripts Lua executados no Redis para garantir operações atômicas de modificação de estoque. O método claimCard implementa uma operação de redução de estoque em uma única transação atômica, prevenindo race conditions que poderiam resultar em duplicação.

18. **Como é garantida a justiça na distribuição de pacotes de cartas?**
    A justiça é garantida pelo mecanismo de lock distribuído que processa as requisições de forma sequencial. Cada requisição de compra adquire um lock global, processa a operação e libera o lock, garantindo que todas as requisições sejam atendidas em ordem e de forma justa.

19. **Qual solução foi implementada para controle de concorrência distribuída?**
    A solução implementa locks distribuídos via Redisson que protegem operações críticas como compra de pacotes. Cada servidor tenta adquirir um lock global antes de modificar recursos compartilhados, garantindo exclusividade e prevenindo inconsistências.

20. **Como é evitada a perda de cartas em situações de alta concorrência?**
    O sistema utiliza operações atômicas no Redis para garantir que cada carta seja distribuída exatamente uma vez. O script Lua utilizado no CardRepository garante que a operação de redução do estoque seja atômica e que nenhuma carta seja "perdida" durante operações concorrentes.

## Consistência e Justiça do Estado do Jogo

21. **Como o sistema garante a consistência do estado do jogo entre servidores?**
    O sistema garante consistência através de locks distribuídos para operações críticas e armazenamento compartilhado no Redis. As operações que afetam recursos globais são protegidas por locks que garantem exclusividade, enquanto dados específicos de jogadores são mantidos localmente e sincronizados quando necessário.

22. **Como é tratada a troca de cartas entre jogadores em servidores diferentes?**
    Atualmente, o sistema tem suporte para troca de cartas, mas a implementação assume que ambos os jogadores estejam no mesmo servidor. Para trocas entre servidores diferentes, seria necessário um mecanismo intermediário de coordenação, mas isso não está completamente implementado na versão atual.

23. **Qual mecanismo evita inconsistências no saldo de cartas?**
    O mecanismo utiliza operações atômicas no Redis (scripts Lua) para garantir que modificações no estoque de cartas sejam feitas de forma consistente. Cada operação de obtenção de carta verifica disponibilidade e reduz o estoque em uma única transação atômica.

24. **Como o progresso da partida é mantido consistentemente em múltiplos servidores?**
    O estado da partida é mantido localmente em cada servidor onde a partida está ocorrendo, com sincronização via Redis para informações compartilhadas. Cada GameSession mantém seu estado localmente, e apenas dados globais (como estoque de cartas) são sincronizados via Redis.

25. **Quais estratégias são usadas para evitar disputas de estado?**
    São utilizados locks distribuídos para operações críticas, operações atômicas no Redis para modificações de estoque, e separação de responsabilidades entre dados locais (por servidor) e dados globais (compartilhados via Redis), minimizando possíveis disputas.

## Pareamento em Ambiente Distribuído

26. **Como o sistema permite pareamento entre jogadores em servidores diferentes?**
    O sistema permite isso através de uma abordagem de emparelhamento distribuído onde servidores diferentes se comunicam entre si para encontrar parceiros de jogo. Cada servidor tenta primeiro encontrar parceiros localmente, e se não encontrar, consulta outros servidores registrados via ServerRegistry.

27. **Que garantias existem para evitar pareamento múltiplo de um jogador?**
    Cada jogador é mantido em uma única fila de matchmaking por vez, e o sistema de lock distribuído garante que apenas um processo de emparelhamento por jogador esteja ativo simultaneamente, evitando emparelhamentos múltiplos.

28. **Como é implementado o sistema de filas para pareamento distribuído?**
    O sistema de filas é implementado localmente em cada servidor, com coordenação entre servidores. Quando um servidor não encontra parceiro local, ele tenta encontrar um parceiro em outros servidores registrados, criando uma rede distribuída de filas de emparelhamento.

29. **Quais são os critérios para o pareamento entre jogadores distribuídos?**
    Os critérios incluem disponibilidade de jogadores em outros servidores e capacidade de coordenação entre os servidores envolvidos. O sistema prioriza emparelhamento local primeiro, e apenas busca parceiros em outros servidores quando necessário.

30. **Como é verificado que um jogador não está emparelhado com múltiplos oponentes?**
    O sistema mantém o estado de emparelhamento em uma fila local e utiliza locks distribuídos para garantir que um jogador só possa estar em um processo de emparelhamento por vez, prevenindo emparelhamentos múltiplos.