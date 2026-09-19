#!/usr/bin/env bash
# Builds the tool and runs it against the local Gitea instance (scripts/gitea-start.sh +
# scripts/gitea-setup.sh) and its seeded pull request, for manual smoke-testing. Extra arguments
# are passed straight through to the tool, e.g.:
#   ./scripts/gitea-run-cli.sh -v CHECKSTYLE ./some/folder ".*checkstyle\.xml$" Checkstyle
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
source scripts/lib.sh
load_env_file

./gradlew -q shadowJar
JAR=$(ls build/libs/*.jar | head -1)

java -jar "$JAR" \
  -url "$GITEA_URL" \
  -owner "$GITEA_OWNER" \
  -rs "$GITEA_REPO" \
  -prid "$GITEA_PR_INDEX" \
  -pat "$GITEA_TOKEN" \
  "$@"
