#!/usr/bin/env bash
# Builds the tool and runs it against the local Gitea instance (scripts/gitea-start.sh +
# scripts/gitea-setup.sh) and its seeded pull request, in accumulated-comment mode only: one
# top-level comment summarizing every violation, rather than one inline comment per violation.
# Works with no arguments - defaults to scripts/example-checkstyle-realistic.xml. Extra arguments
# are passed straight through instead (e.g. to point at a different report):
#   ./scripts/gitea-run-cli-summary.sh -v CHECKSTYLE some/folder ".*report\.xml$" MyTool
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
source scripts/lib.sh
load_env_file

./gradlew -q shadowJar
JAR=$(ls build/libs/*.jar | head -1)

if [ "$#" -eq 0 ]; then
  set -- -v CHECKSTYLE scripts ".*example-checkstyle-realistic\.xml$" Checkstyle
fi

java -jar "$JAR" \
  -url "$GITEA_URL" \
  -owner "$GITEA_OWNER" \
  -rs "$GITEA_REPO" \
  -prid "$GITEA_PR_INDEX" \
  -pat "$GITEA_TOKEN" \
  -csfc false \
  -ccwasfc true \
  -cocf false \
  "$@"
