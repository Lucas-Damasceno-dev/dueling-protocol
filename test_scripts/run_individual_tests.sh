#!/usr/bin/env bash

# Ensures the script stops if any command fails
set -e

echo "======================================================="
echo ">>> STARTING ALL INDIVIDUAL TESTS"
echo "======================================================="

# Run each test script
echo ">>> Running Queue Disconnection test..."
./test_queue_disconnection.sh

echo ">>> Running Mid-Game Disconnection test..."
./test_mid_game_disconnection.sh

echo ">>> Running Simultaneous Play test..."
./test_simultaneous_play.sh

echo ">>> Running Persistence Race Condition test..."
./test_persistence_race_condition.sh

echo ">>> Running Malformed Inputs (Malicious Bot) test..."
./test_malformed_inputs.sh

echo ">>> Running Stress test..."
./test_stress.sh

echo "======================================================="
echo ">>> ALL INDIVIDUAL TESTS FINISHED"
echo "======================================================="