#!/bin/sh

# Loads environment variables then executes the given command.
#
#   Dev:        reads from .env file
#   Docker:     skips (vars already injected by Docker Compose)
#   Production: pulls from AWS SSM at /harbourfront/prod/

if [ "${DOCKER:-false}" = "true" ]; then
    echo "Running in Docker: environment variables already injected..."
elif [ -f .env ]; then
    echo "Loading environment variables from .env..."
    set -a
    source .env
    set +a
else
    echo "Loading environment variables from AWS SSM..."
    export $(aws ssm get-parameters-by-path \
        --path "/harbourfront/prod/" \
        --with-decryption \
        --query "Parameters[*].[Name,Value]" \
        --output text | awk '{print $1"="$2}' | sed 's|/harbourfront/prod/||') || exit 1
fi

exec "$@"
