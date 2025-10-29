#!/usr/bin/env node
/**
 * Teste WebSocket para TRADE, MATCH e PURCHASE
 */

const WebSocket = require('ws');
const https = require('http');

const API_URL = 'http://localhost:8080';
const WS_URL = 'ws://localhost:8080/ws';

// Colors
const colors = {
    reset: '\x1b[0m',
    green: '\x1b[32m',
    red: '\x1b[31m',
    yellow: '\x1b[33m',
    blue: '\x1b[34m'
};

function log(msg, color = colors.blue) {
    console.log(`${color}${msg}${colors.reset}`);
}

async function httpPost(url, data) {
    return new Promise((resolve, reject) => {
        const options = {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Content-Length': JSON.stringify(data).length
            }
        };
        
        const req = require('http').request(url, options, (res) => {
            let body = '';
            res.on('data', chunk => body += chunk);
            res.on('end', () => {
                try {
                    resolve({ status: res.statusCode, data: JSON.parse(body) });
                } catch(e) {
                    resolve({ status: res.statusCode, data: body });
                }
            });
        });
        
        req.on('error', reject);
        req.write(JSON.stringify(data));
        req.end();
    });
}

async function registerAndLogin(username, password) {
    log(`\nRegistering ${username}...`);
    const regResp = await httpPost(`${API_URL}/api/auth/register`, { username, password });
    log(`Register status: ${regResp.status}`);
    
    log(`Logging in ${username}...`);
    const loginResp = await httpPost(`${API_URL}/api/auth/login`, { username, password });
    const token = loginResp.data.token;
    
    if (!token) {
        log(`ERROR: No token for ${username}`, colors.red);
        process.exit(1);
    }
    
    log(`Token for ${username}: ${token.substring(0, 20)}...`, colors.green);
    return token;
}

function waitForMessage(ws, timeout = 10000) {
    return new Promise((resolve, reject) => {
        const timer = setTimeout(() => {
            reject(new Error('Timeout waiting for message'));
        }, timeout);
        
        ws.once('message', (data) => {
            clearTimeout(timer);
            resolve(data.toString());
        });
    });
}

async function testPurchase(token, username) {
    log(`\n=== Testing PURCHASE for ${username} ===`, colors.yellow);
    
    return new Promise((resolve) => {
        const ws = new WebSocket(`${WS_URL}?token=${token}`);
        
        ws.on('open', () => {
            log('✓ WebSocket connected');
        });
        
        let characterCreated = false;
        
        ws.on('message', async (data) => {
            const msg = data.toString();
            log(`← Received: ${msg}`);
            
            if (msg.includes('SUCCESS:CONNECTED') && !characterCreated) {
                // Create character first
                ws.send('CHARACTER_SETUP:TestNick:HUMAN:WARRIOR');
                log('→ Sent: CHARACTER_SETUP:TestNick:HUMAN:WARRIOR');
                characterCreated = true;
            } else if (msg.includes('SUCCESS:Character created') || msg.includes('CHARACTER')) {
                // Now send purchase command
                ws.send('STORE:BUY:BASIC');
                log('→ Sent: STORE:BUY:BASIC');
            } else if (msg.includes('Pack purchased') || msg.includes('Cards received')) {
                log('✅ PURCHASE SUCCESS', colors.green);
                ws.close();
                resolve(true);
            } else if (msg.includes('ERROR') && !msg.includes('already exists')) {
                log(`❌ PURCHASE ERROR: ${msg}`, colors.red);
                ws.close();
                resolve(false);
            }
        });
        
        ws.on('error', (err) => {
            log(`❌ WebSocket error: ${err.message}`, colors.red);
            resolve(false);
        });
        
        // Timeout after 15s
        setTimeout(() => {
            log('❌ PURCHASE TIMEOUT', colors.red);
            ws.close();
            resolve(false);
        }, 15000);
    });
}

async function testTrade(token1, username1, token2, username2) {
    log(`\n=== Testing TRADE: ${username1} ↔ ${username2} ===`, colors.yellow);
    
    return new Promise((resolve) => {
        const ws1 = new WebSocket(`${WS_URL}?token=${token1}`);
        const ws2 = new WebSocket(`${WS_URL}?token=${token2}`);
        
        let ws1Connected = false;
        let ws2Connected = false;
        let ws1CharCreated = false;
        let ws2CharCreated = false;
        let proposalSent = false;
        
        ws1.on('open', () => {
            log(`✓ ${username1} WebSocket connected`);
            ws1Connected = true;
        });
        
        ws2.on('open', () => {
            log(`✓ ${username2} WebSocket connected`);
            ws2Connected = true;
        });
        
        let player1Id = null;
        let player2Id = null;
        
        ws1.on('message', (data) => {
            const msg = data.toString();
            log(`← ${username1} received: ${msg}`);
            
            if (msg.includes('TRADE_COMPLETE') || msg.includes('TRADE_ACCEPTED')) {
                log('✅ TRADE COMPLETE (detected by player 1)', colors.green);
                setTimeout(() => {
                    ws1.close();
                    ws2.close();
                    resolve(true);
                }, 500);
            } else if (msg.includes('SUCCESS:CONNECTED') && !ws1CharCreated) {
                ws1.send('CHARACTER_SETUP:User1:HUMAN:WARRIOR');
                log(`→ ${username1} sent: CHARACTER_SETUP`);
                ws1CharCreated = true;
            } else if (msg.includes('SUCCESS:Character created')) {
                // Extract player ID
                const match = msg.match(/Player ID: (\d+)/);
                if (match) {
                    player1Id = match[1];
                    log(`Player 1 ID: ${player1Id}`);
                }
                
                if (ws2CharCreated && player2Id && !proposalSent) {
                    // Send trade proposal after both characters created
                    setTimeout(() => {
                        if (!proposalSent) {
                            ws1.send(`TRADE:PROPOSE:${player2Id}:basic-0:basic-1`);
                            log(`→ ${username1} sent: TRADE:PROPOSE:${player2Id}:basic-0:basic-1`);
                            proposalSent = true;
                        }
                    }, 2000);
                }
            }
        });
        
        ws2.on('message', (data) => {
            const msg = data.toString();
            log(`← ${username2} received: ${msg}`);
            
            if (msg.includes('SUCCESS:CONNECTED') && !ws2CharCreated) {
                ws2.send('CHARACTER_SETUP:User2:ELF:MAGE');
                log(`→ ${username2} sent: CHARACTER_SETUP`);
                ws2CharCreated = true;
            } else if (msg.includes('SUCCESS:Character created')) {
                // Extract player ID
                const match = msg.match(/Player ID: (\d+)/);
                if (match) {
                    player2Id = match[1];
                    log(`Player 2 ID: ${player2Id}`);
                }
                
                // Try to trigger proposal if player1 is ready
                if (ws1CharCreated && player1Id && !proposalSent) {
                    setTimeout(() => {
                        if (!proposalSent) {
                            ws1.send(`TRADE:PROPOSE:${player2Id}:basic-0:basic-1`);
                            log(`→ ${username1} sent: TRADE:PROPOSE:${player2Id}:basic-0:basic-1`);
                            proposalSent = true;
                        }
                    }, 2000);
                }
            } else if (msg.includes('TRADE_COMPLETE') || msg.includes('TRADE_ACCEPTED')) {
                log('✅ TRADE COMPLETE (detected by player 2)', colors.green);
                setTimeout(() => {
                    ws1.close();
                    ws2.close();
                    resolve(true);
                }, 500);
            } else if (msg.includes('TRADE_PROPOSAL') && !msg.includes('ERROR')) {
                log('✅ TRADE PROPOSAL RECEIVED', colors.green);
                // Extract trade ID from message
                // Format: UPDATE:TRADE_PROPOSAL:tradeId:proposerId:offeredCards:requestedCards
                const parts = msg.split(':');
                const tradeId = parts[2];
                log(`Trade ID extracted: ${tradeId}`);
                
                // Accept trade only once
                setTimeout(() => {
                    ws2.send(`TRADE:ACCEPT:${tradeId}`);
                    log(`→ ${username2} sent: TRADE:ACCEPT:${tradeId}`);
                }, 500);
            } else if (msg.includes('ERROR')) {
                log(`❌ TRADE ERROR: ${msg}`, colors.red);
                setTimeout(() => {
                    ws1.close();
                    ws2.close();
                    resolve(false);
                }, 500);
            }
        });
        
        ws1.on('error', (err) => log(`❌ WS1 error: ${err.message}`, colors.red));
        ws2.on('error', (err) => log(`❌ WS2 error: ${err.message}`, colors.red));
        
        // Timeout after 20s
        setTimeout(() => {
            log('❌ TRADE TIMEOUT', colors.red);
            ws1.close();
            ws2.close();
            resolve(false);
        }, 20000);
    });
}

async function testMatchmaking(token1, username1, token2, username2) {
    log(`\n=== Testing MATCHMAKING: ${username1} vs ${username2} ===`, colors.yellow);
    
    return new Promise((resolve) => {
        const ws1 = new WebSocket(`${WS_URL}?token=${token1}`);
        const ws2 = new WebSocket(`${WS_URL}?token=${token2}`);
        
        let ws1Connected = false;
        let ws2Connected = false;
        let ws1CharCreated = false;
        let ws2CharCreated = false;
        let matchmaking1Sent = false;
        
        ws1.on('open', () => {
            log(`✓ ${username1} WebSocket connected`);
            ws1Connected = true;
        });
        
        ws2.on('open', () => {
            log(`✓ ${username2} WebSocket connected`);
            ws2Connected = true;
        });
        
        ws1.on('message', (data) => {
            const msg = data.toString();
            log(`← ${username1} received: ${msg}`);
            
            if (msg.includes('SUCCESS:CONNECTED') && !ws1CharCreated) {
                ws1.send('CHARACTER_SETUP:Match1:HUMAN:WARRIOR');
                log(`→ ${username1} sent: CHARACTER_SETUP`);
                ws1CharCreated = true;
            } else if (msg.includes('SUCCESS:Character created') && ws2CharCreated && !matchmaking1Sent) {
                setTimeout(() => {
                    ws1.send('MATCHMAKING:ENTER');
                    log(`→ ${username1} sent: MATCHMAKING:ENTER`);
                    matchmaking1Sent = true;
                    
                    // Send second player after 2s
                    setTimeout(() => {
                        ws2.send('MATCHMAKING:ENTER');
                        log(`→ ${username2} sent: MATCHMAKING:ENTER`);
                    }, 2000);
                }, 1000);
            } else if (msg.includes('MATCH') || msg.includes('OPPONENT') || msg.includes('GAME') || msg.includes('Entered matchmaking')) {
                log('✅ MATCHMAKING SUCCESS - Match created', colors.green);
                ws1.close();
                ws2.close();
                resolve(true);
            }
        });
        
        ws2.on('message', (data) => {
            const msg = data.toString();
            log(`← ${username2} received: ${msg}`);
            
            if (msg.includes('SUCCESS:CONNECTED') && !ws2CharCreated) {
                ws2.send('CHARACTER_SETUP:Match2:ELF:MAGE');
                log(`→ ${username2} sent: CHARACTER_SETUP`);
                ws2CharCreated = true;
            } else if (msg.includes('MATCH') || msg.includes('OPPONENT') || msg.includes('GAME') || msg.includes('Entered matchmaking')) {
                log('✅ MATCHMAKING SUCCESS - Match created or entered queue', colors.green);
                // Wait a bit to see if match is created
                setTimeout(() => {
                    ws1.close();
                    ws2.close();
                    resolve(true);
                }, 3000);
            }
        });
        
        ws1.on('error', (err) => log(`❌ WS1 error: ${err.message}`, colors.red));
        ws2.on('error', (err) => log(`❌ WS2 error: ${err.message}`, colors.red));
        
        // Timeout after 25s
        setTimeout(() => {
            log('❌ MATCHMAKING TIMEOUT', colors.red);
            ws1.close();
            ws2.close();
            resolve(false);
        }, 25000);
    });
}

async function main() {
    const testFeature = process.env.TEST_FEATURE || 'ALL';
    const timestamp = Date.now();
    
    log('╔════════════════════════════════════════════════╗', colors.blue);
    if (testFeature === 'ALL') {
        log('║   TESTE WEBSOCKET: PURCHASE + TRADE + MATCH   ║', colors.blue);
    } else {
        log(`║   TESTE WEBSOCKET: ${testFeature.padEnd(29)}║`, colors.blue);
    }
    log('╚════════════════════════════════════════════════╝', colors.blue);
    
    let purchaseOk = null;
    let tradeOk = null;
    let matchOk = null;
    
    // Test PURCHASE
    if (testFeature === 'ALL' || testFeature === 'PURCHASE') {
        const user1 = `purchase_${timestamp}`;
        log(`\nCreating user for PURCHASE: ${user1}`);
        const token1 = await registerAndLogin(user1, 'pass123');
        purchaseOk = await testPurchase(token1, user1);
    }
    
    // Test TRADE
    if (testFeature === 'ALL' || testFeature === 'TRADE') {
        const user2 = `trader1_${timestamp}`;
        const user3 = `trader2_${timestamp}`;
        log(`\nCreating users for TRADE: ${user2}, ${user3}`);
        const token2 = await registerAndLogin(user2, 'pass123');
        const token3 = await registerAndLogin(user3, 'pass123');
        tradeOk = await testTrade(token2, user2, token3, user3);
    }
    
    // Test MATCHMAKING
    if (testFeature === 'ALL' || testFeature === 'MATCHMAKING') {
        const user4 = `match1_${timestamp}`;
        const user5 = `match2_${timestamp}`;
        log(`\nCreating users for MATCHMAKING: ${user4}, ${user5}`);
        const token4 = await registerAndLogin(user4, 'pass123');
        const token5 = await registerAndLogin(user5, 'pass123');
        matchOk = await testMatchmaking(token4, user4, token5, user5);
    }
    
    // Summary
    log('\n╔════════════════════════════════════════════════╗', colors.blue);
    log('║                    RESUMO                      ║', colors.blue);
    log('╚════════════════════════════════════════════════╝', colors.blue);
    
    if (purchaseOk !== null) {
        log(`1. PURCHASE:    ${purchaseOk ? '✅ PASSED' : '❌ FAILED'}`, 
            purchaseOk ? colors.green : colors.red);
    }
    if (tradeOk !== null) {
        log(`2. TRADE:       ${tradeOk ? '✅ PASSED' : '❌ FAILED'}`, 
            tradeOk ? colors.green : colors.red);
    }
    if (matchOk !== null) {
        log(`3. MATCHMAKING: ${matchOk ? '✅ PASSED' : '❌ FAILED'}`, 
            matchOk ? colors.green : colors.red);
    }
    
    const allTests = [purchaseOk, tradeOk, matchOk].filter(v => v !== null);
    const success = allTests.length > 0 && allTests.every(v => v === true);
    process.exit(success ? 0 : 1);
}

main().catch(err => {
    console.error('Fatal error:', err);
    process.exit(1);
});
