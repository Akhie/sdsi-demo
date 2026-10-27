#!/bin/bash
# Expense Splitter — Quick Start
echo "=== Backend ==="
cd "$(dirname "$0")/backend" || exit 1
echo "Starting backend on :8080 ... (requires Maven + JDK 17)"
mvn spring-boot:run &
BACKEND_PID=$!
sleep 6

echo "=== Frontend ==="
cd "$(dirname "$0")/frontend" || exit 1
echo "Starting frontend on :3000 ... (requires Node 16+)"
npm install --silent 2>/dev/null
npm run dev &
FRONTEND_PID=$!

echo ""
echo "Backend:  http://localhost:8080"
echo "Frontend: http://localhost:3000"
echo "Press Ctrl+C to stop both"
trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null" EXIT
wait