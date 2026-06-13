#!/bin/bash
set -e

# Builds the server locally, pulls the latest code into the existing clone
# on the Ubuntu server (the "sky" host from ~/.ssh/config), and uploads only
# the built jar. The server only needs Java (and pm2) installed, no build
# tools required there.

REMOTE_DIR="~/harbourfront-digital"
JAR_NAME="harbourfront-server-1.0.jar"

echo "Building server..."
(cd server && mvn package -q -DskipTests)

echo "Pulling latest code on server..."
ssh sky "cd $REMOTE_DIR && git pull"

echo "Uploading jar..."
ssh sky "mkdir -p $REMOTE_DIR/server/target"
scp -q "server/target/$JAR_NAME" "sky:$REMOTE_DIR/server/target/"

echo "Restarting app..."
ssh sky "cd $REMOTE_DIR && pm2 startOrReload ecosystem.config.js --update-env"

echo "Deploy complete."
