#!/bin/bash
# Common environment variable setup for test scripts

create_env_file() {
  local env_file=${1:-.env}
  local bot_mode=${2:-autobot}
  local bot_scenario=${3:-}
  
  cat > "$env_file" << ENVEOF
# Bot Configuration
BOT_MODE=$bot_mode
BOT_SCENARIO=$bot_scenario

# Database Configuration
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_DB=dueling_db
POSTGRES_USER=dueling_user
POSTGRES_PASSWORD=dueling_password

# Redis Configuration
REDIS_SENTINEL_MASTER=mymaster
REDIS_SENTINEL_NODES=redis-sentinel-1:26379,redis-sentinel-2:26379,redis-sentinel-3:26379

# Gateway Configuration
GATEWAY_HOST=nginx-gateway
GATEWAY_PORT=8080
ENVEOF
}

# Export the function
export -f create_env_file
