#!/bin/bash
echo ">>> Iniciando o GameServer com Docker..."
docker run -p 7777:7777/tcp -p 7778:7778/udp dueling-protocol
