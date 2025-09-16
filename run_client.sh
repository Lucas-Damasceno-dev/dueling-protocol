#!/bin/bash
echo ">>> Iniciando o GameClient com Docker..."
docker run -it --network=host dueling-protocol java -cp app.jar GameClient