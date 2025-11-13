#!/usr/bin/env python3
import yaml
import sys

# Read docker-compose.yml
with open('docker/docker-compose.yml', 'r') as f:
    compose = yaml.safe_load(f)

# Add volumes to blockchain
compose['services']['dueling-blockchain']['volumes'] = [
    'blockchain-data:/usr/src/app/cache',
    'blockchain-deployments:/usr/src/app/deployments',
    'shared-blockchain:/usr/src/app/shared'
]

# Add deploy command
compose['services']['dueling-blockchain']['command'] = (
    'sh -c "npx hardhat node --hostname 0.0.0.0 & '
    'sleep 10 && '
    'npx hardhat run scripts/deploy.js --network localhost && '
    'cp deployment-info.json shared/ && '
    'wait"'
)

# Update healthcheck start_period
compose['services']['dueling-blockchain']['healthcheck']['start_period'] = '40s'

# Update all 4 servers
for server_name in ['server-1', 'server-2', 'server-3', 'server-4']:
    server = compose['services'][server_name]
    
    # Add volume
    if 'volumes' not in server:
        server['volumes'] = []
    server['volumes'].append('shared-blockchain:/shared:ro')
    
    # Remove old contract env vars and add new one
    env = server['environment']
    for key in list(env.keys()):
        if key in ['ASSET_CONTRACT', 'STORE_CONTRACT', 'TRADE_CONTRACT', 'MATCH_CONTRACT']:
            del env[key]
    
    env['BLOCKCHAIN_DEPLOYMENT_FILE'] = '/shared/deployment-info.json'

# Add new volumes
compose['volumes']['blockchain-deployments'] = None
compose['volumes']['shared-blockchain'] = None

# Write back
with open('docker/docker-compose.yml', 'w') as f:
    yaml.dump(compose, f, default_flow_style=False, sort_keys=False)

print("✅ docker-compose.yml atualizado com sucesso!")
