#!/usr/bin/env bash

# Define the log file name with current date and time to keep them organized
LOG_FILE="test_results_$(date +'%Y-%m-%d_%H-%M-%S').log"

echo "======================================================="
echo ">>> RUNNING ALL TESTS AND SAVING OUTPUT"
echo ">>> The output will be displayed here and also saved to file: $LOG_FILE"
echo "======================================================="

# Runs the test script and redirects the output
# The 'tee' command allows the output to be displayed in the terminal AND saved to a file at the same time.
# '2>&1' ensures that both standard output and errors are captured.
./run_all_tests.sh 2>&1 | tee "$LOG_FILE"

echo "======================================================="
echo ">>> EXECUTION COMPLETED"
echo ">>> The complete results have been saved to: $LOG_FILE"
echo "======================================================="
