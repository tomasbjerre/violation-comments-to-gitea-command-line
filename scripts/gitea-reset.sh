#!/usr/bin/env bash
# Tears the local Gitea instance down and deletes all of its data, for a clean slate.
# Follow up with scripts/gitea-start.sh && scripts/gitea-setup.sh to rebuild it.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

docker compose down -v
rm -rf .gitea-data
rm -f scripts/.env
