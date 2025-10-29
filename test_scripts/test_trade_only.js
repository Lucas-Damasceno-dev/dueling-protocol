const WebSocket = require('ws');
const API_URL = 'http://localhost:8080';
const WS_URL = 'ws://localhost:8080/ws';

async function httpPost(url, data) {
    return new Promise((resolve) => {
        const req = require('http').request(url, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'}
        }, (res) => {
            let body = '';
            res.on('data', chunk => body += chunk);
            res.on('end', () => resolve({status: res.statusCode, data: JSON.parse(body)}));
        });
        req.write(JSON.stringify(data));
        req.end();
    });
}

async function registerAndLogin(username) {
    await httpPost(`${API_URL}/api/auth/register`, {username, password: 'pass'});
    const resp = await httpPost(`${API_URL}/api/auth/login`, {username, password: 'pass'});
    return resp.data.token;
}

async function main() {
    const ts = Date.now();
    const token1 = await registerAndLogin(`trader1_${ts}`);
    const token2 = await registerAndLogin(`trader2_${ts}`);
    
    const ws1 = new WebSocket(`${WS_URL}?token=${token1}`);
    const ws2 = new WebSocket(`${WS_URL}?token=${token2}`);
    
    let p1id, p2id;
    let tradeId;
    
    ws1.on('message', msg => {
        console.log('P1:', msg.toString());
        if (msg.includes('Player ID:')) {
            p1id = msg.toString().match(/Player ID: (\d+)/)[1];
            console.log(`>> Player 1 ID: ${p1id}`);
        }
        if (msg.includes('TRADE_COMPLETE')) {
            console.log('✅✅✅ TRADE COMPLETE!');
            process.exit(0);
        }
    });
    
    ws2.on('message', msg => {
        console.log('P2:', msg.toString());
        if (msg.includes('Player ID:')) {
            p2id = msg.toString().match(/Player ID: (\d+)/)[1];
            console.log(`>> Player 2 ID: ${p2id}`);
        }
        if (msg.includes('TRADE_PROPOSAL')) {
            tradeId = msg.toString().split(':')[2];
            console.log(`>> Trade ID: ${tradeId}`);
            console.log(`>> Accepting trade...`);
            ws2.send(`TRADE:ACCEPT:${tradeId}`);
        }
        if (msg.includes('TRADE_COMPLETE')) {
            console.log('✅✅✅ TRADE COMPLETE!');
            process.exit(0);
        }
    });
    
    setTimeout(() => {
        ws1.send('CHARACTER_SETUP:P1:HUMAN:WARRIOR');
        ws2.send('CHARACTER_SETUP:P2:ELF:MAGE');
    }, 1000);
    
    setTimeout(() => {
        if (p1id && p2id) {
            console.log(`>> Proposing trade from P1 to P2 (${p2id})...`);
            ws1.send(`TRADE:PROPOSE:${p2id}:basic-0:basic-1`);
        }
    }, 4000);
    
    setTimeout(() => {
        console.log('❌ Timeout - TRADE failed');
        process.exit(1);
    }, 15000);
}

main();
