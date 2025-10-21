#!/bin/bash
# Teste robusto de match cross-server usando conexões stdin/stdout

set -e

T=$(date +%s)
PA="ROBUST_A_${T}"
PB="ROBUST_B_${T}"

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║         TESTE ROBUSTO DE MATCH CROSS-SERVER                  ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "Player A: $PA (Server 1:8080)"
echo "Player B: $PB (Server 2:8083)"
echo ""

TMP="/tmp/robust_${T}"
mkdir -p "$TMP"

# Create named pipes for bidirectional communication
mkfifo "$TMP/in_a" "$TMP/in_b"

echo "[1/4] Starting Player A connection..."
# Start websocat with input from pipe
websocat ws://localhost:8080/ws < "$TMP/in_a" > "$TMP/out_a.log" 2>&1 &
PID_A=$!
exec 3>"$TMP/in_a"  # Open pipe for writing
sleep 2

echo "GAME:${PA}:CHARACTER_SETUP:A:Human:Warrior" >&3
sleep 5

echo "[2/4] Starting Player B connection..."
websocat ws://localhost:8083/ws < "$TMP/in_b" > "$TMP/out_b.log" 2>&1 &
PID_B=$!
exec 4>"$TMP/in_b"
sleep 2

echo "GAME:${PB}:CHARACTER_SETUP:B:Elf:Mage" >&4
sleep 5

echo "[3/4] Entering matchmaking..."
echo "GAME:${PA}:MATCHMAKING:ENTER" >&3
sleep 3

echo "GAME:${PB}:MATCHMAKING:ENTER" >&4
sleep 15

echo "[4/4] Checking results..."
echo ""

# Close pipes
exec 3>&-
exec 4>&-

# Check server logs
echo "=== Server Logs ==="
if grep -q "Found remote partner" /tmp/DEBUG1.log /tmp/DEBUG2.log 2>/dev/null; then
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║    ✅ CROSS-SERVER MATCH WORKING! ✅                       ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    grep "Found remote partner" /tmp/DEBUG1.log /tmp/DEBUG2.log | head -3
    RESULT=0
else
    echo "Checking detailed logs..."
    tail -30 /tmp/DEBUG1.log | grep -i "matchmaking\|queue" | tail -10
    RESULT=1
fi

kill $PID_A $PID_B 2>/dev/null || true
rm -f "$TMP/in_a" "$TMP/in_b"

exit $RESULT
