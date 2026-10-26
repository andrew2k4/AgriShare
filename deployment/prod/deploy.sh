#!/usr/bin/env bash
# Run from /opt/agrishare/repo on the VPS: ./deployment/prod/deploy.sh
set -euo pipefail

cd "$(dirname "$0")/../.."
git pull origin main

cd deployment/prod
docker compose pull --ignore-buildable 2>/dev/null || true
docker compose build
docker compose up -d
docker compose ps
