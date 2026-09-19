#!/usr/bin/env bash
# Starts a local Gitea instance (Docker) on http://localhost:3000, keeping any existing data.
# Run scripts/gitea-setup.sh afterwards to seed it with a user, repo and PR.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
source scripts/lib.sh

docker_compose up -d
wait_for_gitea
