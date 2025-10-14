#!/bin/bash
echo ">>> Starting GameServer..."

# Build the server module if not already built
cd ../dueling-server
if [ ! -f "target/dueling-server-1.0-SNAPSHOT.jar" ]; then
    echo "Building server module..."
    mvn clean package -DskipTests
fi

# Run the server with correct configuration
echo "Starting server on port 8083..."
java -DSERVER_PORT=8083 \
     -DPOSTGRES_HOST=localhost \
     -DPOSTGRES_PORT=5432 \
     -DPOSTGRES_DB=dueling_db \
     -DPOSTGRES_USER=dueling_user \
     -DPOSTGRES_PASSWORD=dueling_pass \
     -DREDIS_SENTINEL_MASTER=mymaster \
     -DREDIS_SENTINEL_NODES=localhost:26379,localhost:26380,localhost:26381 \
     -Dspring.profiles.active=server,default \
     -jar target/dueling-server-1.0-SNAPSHOT.jar
