#!/usr/bin/env bash
# Stops the local Gitea container without deleting its data (scripts/.gitea-data).
# Use scripts/gitea-start.sh to bring it back up with the same state.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

docker compose stop
