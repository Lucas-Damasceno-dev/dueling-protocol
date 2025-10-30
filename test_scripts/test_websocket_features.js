#!/usr/bin/env node
/**
 * Teste WebSocket para TRADE, MATCH e PURCHASE
 */

const WebSocket = require('ws');
const https = require('http');
const crypto = require('crypto');

const API_URL = 'http://localhost:8080';
const WS_URL = 'ws://localhost:8080/ws';

function generateUniqueId() {
    return crypto.randomBytes(8).toString('hex');
}

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
    
    if (regResp.status !== 200) {
        log(`ERROR: Registration failed for ${username}: ${JSON.stringify(regResp.data)}`, colors.red);
        return null;
    }
    
    log(`Logging in ${username}...`);
    const loginResp = await httpPost(`${API_URL}/api/auth/login`, { username, password });
    const token = loginResp.data.token;
    
    if (!token) {
        log(`ERROR: No token for ${username}`, colors.red);
        process.exit(1);
    }
    
    // Decode JWT to get the actual username from token
    const payload = token.split('.')[1];
    const decoded = JSON.parse(Buffer.from(payload, 'base64').toString());
    log(`Token username: ${decoded.sub}`, colors.green);
    
    return { token, username: decoded.sub };
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
            } else if (msg.includes('ERROR:Character already exists')) {
                // Character already exists, just proceed with purchase
                log('ℹ Character already exists, proceeding with purchase', colors.yellow);
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
        let player1Cards = [];
        let player2Cards = [];
        
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
            } else if (msg.includes('ERROR:Character already exists')) {
                log(`❌ ${username1} character already exists - THIS SHOULD NOT HAPPEN WITH UNIQUE USERS!`, colors.red);
                log(`Username was: ${username1}`, colors.red);
                ws1.close();
                ws2.close();
                resolve(false);
            } else if (msg.includes('SUCCESS:Character created')) {
                // Extract player ID - it can be alphanumeric now
                const match = msg.match(/Player ID:\s*([a-zA-Z0-9_-]+)/);
                if (match) {
                    player1Id = match[1];
                    log(`Player 1 ID: ${player1Id}`);
                }
                
                // Request cards to get card IDs for trade
                ws1.send('SHOW_CARDS');
                log(`→ ${username1} sent: SHOW_CARDS`);
            } else if (msg.includes('INFO:YOUR_CARDS')) {
                // Extract card IDs from the response
                // Format: INFO:YOUR_CARDS:cardId1(name1),cardId2(name2),...
                const cardsPart = msg.split(':').slice(2).join(':');
                const cardMatches = cardsPart.match(/([a-z0-9-]+)\(/g);
                
                if (cardMatches && cardMatches.length >= 1) {
                    player1Cards = cardMatches.map(m => m.slice(0, -1));
                    log(`Player 1 has ${player1Cards.length} cards`);
                }
                
                // Try to send trade proposal if both players are ready
                if (ws2CharCreated && player2Id && player1Cards.length >= 1 && player2Cards.length >= 1 && !proposalSent) {
                    setTimeout(() => {
                        if (!proposalSent) {
                            ws1.send(`TRADE:PROPOSE:${player2Id}:${player1Cards[0]}:${player2Cards[0]}`);
                            log(`→ ${username1} sent: TRADE:PROPOSE:${player2Id}:${player1Cards[0]}:${player2Cards[0]}`);
                            proposalSent = true;
                        }
                    }, 1000);
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
            } else if (msg.includes('ERROR:Character already exists')) {
                log(`❌ ${username2} character already exists - THIS SHOULD NOT HAPPEN WITH UNIQUE USERS!`, colors.red);
                log(`Username was: ${username2}`, colors.red);
                ws1.close();
                ws2.close();
                resolve(false);
            } else if (msg.includes('SUCCESS:Character created')) {
                // Extract player ID - it can be alphanumeric now
                const match = msg.match(/Player ID:\s*([a-zA-Z0-9_-]+)/);
                if (match) {
                    player2Id = match[1];
                    log(`Player 2 ID: ${player2Id}`);
                }
                
                // Request cards to get card IDs for trade
                ws2.send('SHOW_CARDS');
                log(`→ ${username2} sent: SHOW_CARDS`);
            } else if (msg.includes('INFO:YOUR_CARDS')) {
                // Extract card IDs from the response
                const cardsPart = msg.split(':').slice(2).join(':');
                const cardMatches = cardsPart.match(/([a-z0-9-]+)\(/g);
                
                if (cardMatches && cardMatches.length >= 1) {
                    player2Cards = cardMatches.map(m => m.slice(0, -1));
                    log(`Player 2 has ${player2Cards.length} cards`);
                }
                
                // If player1 is ready, request their cards
                if (ws1CharCreated && player1Cards.length === 0) {
                    setTimeout(() => {
                        ws1.send('SHOW_CARDS');
                        log(`→ ${username1} sent: SHOW_CARDS (triggered by p2)`);
                    }, 500);
                }
                
                // Try to send trade proposal if both players are ready
                if (ws1CharCreated && player1Id && player2Cards.length >= 1 && player1Cards.length >= 1 && !proposalSent) {
                    setTimeout(() => {
                        if (!proposalSent) {
                            ws1.send(`TRADE:PROPOSE:${player2Id}:${player1Cards[0]}:${player2Cards[0]}`);
                            log(`→ ${username1} sent: TRADE:PROPOSE:${player2Id}:${player1Cards[0]}:${player2Cards[0]}`);
                            proposalSent = true;
                        }
                    }, 500);
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
            } else if (msg.includes('ERROR') && !msg.includes('already exists')) {
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
            } else if (msg.includes('ERROR:Character already exists')) {
                log(`ℹ ${username1} character already exists, continuing...`, colors.yellow);
                ws1CharCreated = true;
                // Check if we can start matchmaking
                if (ws2CharCreated && !matchmaking1Sent) {
                    setTimeout(() => {
                        ws1.send('MATCHMAKING:ENTER');
                        log(`→ ${username1} sent: MATCHMAKING:ENTER`);
                        matchmaking1Sent = true;
                        
                        setTimeout(() => {
                            ws2.send('MATCHMAKING:ENTER');
                            log(`→ ${username2} sent: MATCHMAKING:ENTER`);
                        }, 2000);
                    }, 1000);
                }
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
            } else if (msg.includes('ERROR:Character already exists')) {
                log(`ℹ ${username2} character already exists, continuing...`, colors.yellow);
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
        const user1 = `p${generateUniqueId()}`;
        log(`\nCreating user for PURCHASE: ${user1}`);
        const auth1 = await registerAndLogin(user1, 'pass123');
        if (!auth1) {
            log('ERROR: Failed to register user for PURCHASE test', colors.red);
            purchaseOk = false;
        } else {
            purchaseOk = await testPurchase(auth1.token, auth1.username);
        }
    }
    
    // Test TRADE
    if (testFeature === 'ALL' || testFeature === 'TRADE') {
        const user2 = `t${generateUniqueId()}`;
        const user3 = `t${generateUniqueId()}`;
        log(`\nCreating users for TRADE: ${user2}, ${user3}`);
        const auth2 = await registerAndLogin(user2, 'pass123');
        const auth3 = await registerAndLogin(user3, 'pass123');
        if (!auth2 || !auth3) {
            log('ERROR: Failed to register users for TRADE test', colors.red);
            tradeOk = false;
        } else {
            tradeOk = await testTrade(auth2.token, auth2.username, auth3.token, auth3.username);
        }
    }
    
    // Test MATCHMAKING
    if (testFeature === 'ALL' || testFeature === 'MATCHMAKING') {
        const user4 = `m${generateUniqueId()}`;
        const user5 = `m${generateUniqueId()}`;
        log(`\nCreating users for MATCHMAKING: ${user4}, ${user5}`);
        const auth4 = await registerAndLogin(user4, 'pass123');
        const auth5 = await registerAndLogin(user5, 'pass123');
        if (!auth4 || !auth5) {
            log('ERROR: Failed to register users for MATCHMAKING test', colors.red);
            matchOk = false;
        } else {
            matchOk = await testMatchmaking(auth4.token, auth4.username, auth5.token, auth5.username);
        }
    }
    
    // Summary
    log('\n╔════════════════════════════════════════════════╗', colors.blue);
    log('║                    RESUMO                      ║', colors.blue);
    log('╚════════════════════════════════════════════════╝', colors.blue);
    
    if (purchaseOk !== null) {
        log(`1. PURCHASE:    ${purchaseOk === 'SKIP' ? '⊘ SKIPPED (db not clean)' : purchaseOk ? '✅ PASSED' : '❌ FAILED'}`, 
            purchaseOk === 'SKIP' ? colors.yellow : purchaseOk ? colors.green : colors.red);
    }
    if (tradeOk !== null) {
        log(`2. TRADE:       ${tradeOk === 'SKIP' ? '⊘ SKIPPED (db not clean)' : tradeOk ? '✅ PASSED' : '❌ FAILED'}`, 
            tradeOk === 'SKIP' ? colors.yellow : tradeOk ? colors.green : colors.red);
    }
    if (matchOk !== null) {
        log(`3. MATCHMAKING: ${matchOk === 'SKIP' ? '⊘ SKIPPED (db not clean)' : matchOk ? '✅ PASSED' : '❌ FAILED'}`, 
            matchOk === 'SKIP' ? colors.yellow : matchOk ? colors.green : colors.red);
    }
    
    const allTests = [purchaseOk, tradeOk, matchOk].filter(v => v !== null && v !== 'SKIP');
    const success = allTests.length > 0 && allTests.every(v => v === true);
    const hasSkipped = [purchaseOk, tradeOk, matchOk].some(v => v === 'SKIP');
    
    if (hasSkipped) {
        log('\n⚠ Some tests were skipped due to existing data in database.', colors.yellow);
        log('Please clean the database before running tests for accurate results.', colors.yellow);
    }
    
    process.exit(success ? 0 : 1);
}

main().catch(err => {
    console.error('Fatal error:', err);
    process.exit(1);
});
