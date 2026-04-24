#!/bin/bash

BINARY_NAME="harbourfront-server"
CMD=$1

# If no command is given, show usage
if [ -z "$CMD" ]; then
  echo "Usage: $0 [build | run | seed]"
  exit 1
fi

if [ "$CMD" == "build" ]; then
  echo "Building server..."
  (cd server && mvn package -q -DskipTests)
  echo "Build complete. Run with: ./do.sh run"
fi

if [ "$CMD" == "run" ]; then
  echo "Building server..."
  (cd server && mvn package -q -DskipTests)
  echo "Starting server..."
  ./env.sh java -jar server/target/harbourfront-server-1.0.jar
fi

if [ "$CMD" == "seed" ]; then
  echo "Building server..."
  (cd server && mvn package -q -DskipTests)
  echo "Running seed..."
  ./env.sh java -jar server/target/harbourfront-server-1.0.jar seed
fi