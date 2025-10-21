#!/bin/bash
echo ">>> Starting GameServer..."

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Build the server module if not already built
cd "$PROJECT_ROOT/dueling-server"
if [ ! -f "target/dueling-server-1.0-SNAPSHOT.jar" ]; then
    echo "Building server module..."
    mvn clean package -DskipTests
fi

# Run the server with correct configuration for local development
echo "Starting server on port 8083 with local-dev profile..."
java -DSERVER_PORT=8083 \
     -DPOSTGRES_HOST=localhost \
     -DPOSTGRES_PORT=5432 \
     -DPOSTGRES_DB=dueling_db \
     -DPOSTGRES_USER=dueling_user \
     -DPOSTGRES_PASSWORD=dueling_password \
     -Dspring.redis.host=localhost \
     -Dspring.redis.port=6379 \
     -Dspring.profiles.active=server,local-dev \
     -jar "$PROJECT_ROOT/dueling-server/target/dueling-server-1.0-SNAPSHOT.jar"
