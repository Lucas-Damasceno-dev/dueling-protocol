#!/bin/bash
echo ">>> Starting GameClient with Docker..."
docker run -it --network=host dueling-protocol java -cp app.jar GameClient