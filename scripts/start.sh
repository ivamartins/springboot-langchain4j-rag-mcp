#!/bin/bash
# Starts the app in a detached process so it survives the parent shell exit.
cd /home/iva/workspace/study/java/langchain4j-rag-mcp
pkill -9 -f langchain4j-rag-mcp 2>/dev/null
pkill -9 -f mcp-server-everything 2>/dev/null
sleep 1
setsid java -jar target/langchain4j-rag-mcp-0.0.1-SNAPSHOT.jar > /tmp/app.log 2>&1 < /dev/null &
echo $! > /tmp/app.pid
echo "Started PID $(cat /tmp/app.pid)"
