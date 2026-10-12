#!/usr/bin/env bash
set -e

echo "=========================================================="
echo " Starting Student Management System - Live Web Container"
echo "=========================================================="

# Wait for MySQL to be ready if SMS_DB_HOST is set
if [ -n "$SMS_DB_HOST" ]; then
    echo "Waiting for database at $SMS_DB_HOST:$SMS_DB_PORT..."
    for i in {1..30}; do
        if nc -z "$SMS_DB_HOST" "${SMS_DB_PORT:-3306}" 2>/dev/null || (echo > /dev/tcp/"$SMS_DB_HOST"/"${SMS_DB_PORT:-3306}") 2>/dev/null; then
            echo "Database connection ready."
            break
        fi
        echo "Database not ready yet... waiting 2s ($i/30)"
        sleep 2
    done
fi

echo "Starting Xvfb virtual framebuffer on display :0..."
Xvfb :0 -screen 0 ${SCREEN_WIDTH:-1366}x${SCREEN_HEIGHT:-768}x${SCREEN_DEPTH:-24} &
sleep 2

echo "Starting Openbox window manager..."
openbox &
sleep 1

echo "Starting x11vnc server..."
x11vnc -display :0 -nopw -listen 0.0.0.0 -xkb -forever -shared &
sleep 1

echo "Starting noVNC web server on port 6080..."
websockify --web /usr/share/novnc/ 6080 localhost:5900 &
sleep 1

echo "Application is now LIVE and accessible via web browser at http://localhost:6080/vnc.html"
echo "Starting Java application..."

exec java -Duser.timezone=UTC -jar /app/dist/StudentManagementSystem.jar
