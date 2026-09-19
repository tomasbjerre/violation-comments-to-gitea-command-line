#!/usr/bin/env bash
# Builds the tool and runs it against the local Gitea instance (scripts/gitea-start.sh +
# scripts/gitea-setup.sh) and its seeded pull request, for manual smoke-testing. Extra arguments
# are passed straight through to the tool (and, being single-value options, override the
# -comment-template baked in below if repeated).
#
# Renders with a custom template (scripts/example-comment-template.mustach), to demonstrate
# -comment-template - drop that argument to see the tool's own default instead.
#
# scripts/example-checkstyle-realistic.xml is a real, multi-file Checkstyle report (from
# violations-lib's own test resources); its files aren't part of the demo repo's diff, so it needs
# the accumulated-comment mode, not inline comments (see also scripts/gitea-run-cli-summary.sh,
# which bakes exactly this in and needs no arguments):
#   ./scripts/gitea-run-cli.sh -csfc false -ccwasfc true -cocf false \
#     -v CHECKSTYLE scripts ".*example-checkstyle-realistic\.xml$" Checkstyle
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
source scripts/lib.sh
load_env_file

if [ "$#" -eq 0 ]; then
  log "No arguments given - this script always needs -v <PARSER> <FOLDER> <PATTERN> <NAME>"
  log "(with no arguments, the tool runs and finds 0 violations, so nothing gets posted - no error, just silently a no-op)."
  log "Either pass your own -v ..., or use scripts/gitea-run-cli-summary.sh, which works with no arguments."
  exit 1
fi

./gradlew -q shadowJar
JAR=$(ls build/libs/*.jar | head -1)

java -jar "$JAR" \
  -url "$GITEA_URL" \
  -owner "$GITEA_OWNER" \
  -rs "$GITEA_REPO" \
  -prid "$GITEA_PR_INDEX" \
  -pat "$GITEA_TOKEN" \
  -comment-template "$(cat scripts/example-comment-template.mustach)" \
  "$@"
