#!/usr/bin/env bash
# Seeds the local Gitea instance (see scripts/gitea-start.sh) with:
#  - an admin user
#  - an API token (written, with everything else needed to reach it, to scripts/.env)
#  - a test repo, with a "main" branch and a "feature/violations-demo" branch that changes
#    src/main/java/com/example/MyClass.java
#  - an open pull request from the feature branch into main
#
# Safe to re-run: skips any step whose result already exists.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
source scripts/lib.sh

FEATURE_BRANCH="feature/violations-demo"

docker_compose up -d
wait_for_gitea

api() {
  # api <method> <path> [jq-body]
  local method="$1" path="$2" body="${3:-}"
  if [ -n "$body" ]; then
    curl -sf -u "$GITEA_ADMIN_USER:$GITEA_ADMIN_PASSWORD" \
      -H "Content-Type: application/json" \
      -X "$method" "$GITEA_URL/api/v1$path" -d "$body"
  else
    curl -sf -u "$GITEA_ADMIN_USER:$GITEA_ADMIN_PASSWORD" \
      -X "$method" "$GITEA_URL/api/v1$path"
  fi
}

ensure_admin_user() {
  if docker_compose exec -T --user git gitea gitea admin user list 2>/dev/null | awk '{print $2}' | grep -qx "$GITEA_ADMIN_USER"; then
    log "Admin user '$GITEA_ADMIN_USER' already exists."
    return
  fi
  log "Creating admin user '$GITEA_ADMIN_USER'..."
  docker_compose exec -T --user git gitea gitea admin user create \
    --username "$GITEA_ADMIN_USER" \
    --password "$GITEA_ADMIN_PASSWORD" \
    --email "$GITEA_ADMIN_EMAIL" \
    --admin \
    --must-change-password=false
}

ensure_api_token() {
  log "Creating API token '$GITEA_TOKEN_NAME'..."
  local response
  if ! response=$(api POST "/users/$GITEA_ADMIN_USER/tokens" \
      "{\"name\":\"$GITEA_TOKEN_NAME\",\"scopes\":[\"write:repository\",\"write:issue\",\"write:user\"]}" 2>/dev/null); then
    log "Token '$GITEA_TOKEN_NAME' likely already exists, recreating it..."
    api DELETE "/users/$GITEA_ADMIN_USER/tokens/$GITEA_TOKEN_NAME" >/dev/null 2>&1 || true
    response=$(api POST "/users/$GITEA_ADMIN_USER/tokens" \
      "{\"name\":\"$GITEA_TOKEN_NAME\",\"scopes\":[\"write:repository\",\"write:issue\",\"write:user\"]}")
  fi
  GITEA_TOKEN=$(echo "$response" | jq -r .sha1)
  if [ -z "$GITEA_TOKEN" ] || [ "$GITEA_TOKEN" = "null" ]; then
    log "Failed to obtain an API token. Response was: $response"
    exit 1
  fi
}

ensure_repo() {
  if curl -sf -H "Authorization: token $GITEA_TOKEN" \
      "$GITEA_URL/api/v1/repos/$GITEA_OWNER/$GITEA_REPO" >/dev/null 2>&1; then
    log "Repo '$GITEA_OWNER/$GITEA_REPO' already exists."
    return
  fi
  log "Creating repo '$GITEA_OWNER/$GITEA_REPO'..."
  curl -sf -H "Authorization: token $GITEA_TOKEN" -H "Content-Type: application/json" \
    -X POST "$GITEA_URL/api/v1/user/repos" \
    -d "{\"name\":\"$GITEA_REPO\",\"auto_init\":true,\"default_branch\":\"main\",\"private\":false}" \
    >/dev/null
}

ensure_feature_branch() {
  if curl -sf -H "Authorization: token $GITEA_TOKEN" \
      "$GITEA_URL/api/v1/repos/$GITEA_OWNER/$GITEA_REPO/branches/$FEATURE_BRANCH" >/dev/null 2>&1; then
    log "Branch '$FEATURE_BRANCH' already exists."
    return
  fi

  log "Seeding repo content and pushing '$FEATURE_BRANCH'..."
  local scratch
  scratch=$(mktemp -d)
  trap 'rm -rf "$scratch"' RETURN

  local clone_url="http://$GITEA_ADMIN_USER:$GITEA_TOKEN@localhost:3000/$GITEA_OWNER/$GITEA_REPO.git"
  git clone --quiet "$clone_url" "$scratch/$GITEA_REPO"
  (
    cd "$scratch/$GITEA_REPO"
    git config user.email "$GITEA_ADMIN_EMAIL"
    git config user.name "$GITEA_ADMIN_USER"

    mkdir -p src/main/java/com/example
    cat > src/main/java/com/example/MyClass.java <<'JAVA'
package com.example;

public class MyClass {

  public void doSomething() {
    int result = compute(1, 2);
    System.out.println("result: " + result);
  }

  private int compute(final int a, final int b) {
    return a + b;
  }
}
JAVA
    git add -A
    git commit --quiet -m "chore: seed source file for violation comments demo"
    git push --quiet origin main

    git checkout -q -b "$FEATURE_BRANCH"
    cat > src/main/java/com/example/MyClass.java <<'JAVA'
package com.example;

public class MyClass {

  public void doSomething() {
    int result = compute(1, 2);
    System.out.println("result: " + result);
    System.out.println("done");
  }

  private int compute(final int a, final int b) {
    // intentionally sloppy line, for a violation comment to attach to below
    int unused = 0;
    return a + b;
  }
}
JAVA

    # Also touched here so scripts/example-checkstyle-realistic.xml's violations (which report
    # against these paths) land on files that are actually part of the PR diff - needed for
    # scripts/gitea-run-cli-single.sh's inline comments, which (unlike the accumulated/summary
    # mode) can only be posted on a file that's part of the diff.
    mkdir -p src/main/java/se/bjurr/violations/lib/example
    cat > src/main/java/se/bjurr/violations/lib/example/MyClass.java <<'JAVA'
package se.bjurr.violations.lib.example;

public class MyClass {

  public void doSomething() {
    int result = compute(1, 2);
    System.out.println("result: " + result);
    if (result > 0) {
    }
  }

  private int compute(final int a, final int b) {
    return a + b;
  }
}
JAVA
    cat > src/main/java/se/bjurr/violations/lib/example/OtherClass.java <<'JAVA'
package se.bjurr.violations.lib.example;

public class OtherClass {

  public void doSomethingElse() {
    int value = compute();
    if (value > 0) {
    }
  }

  private int compute() {
    return 42;
  }

  public boolean isComplex(
      final boolean a,
      final boolean b,
      final boolean c,
      final boolean d,
      final boolean e,
      final boolean f,
      final boolean g,
      final boolean h) {
    return a && b && c && d || e && f || g && h || a && !b || c && !d;
  }
}
JAVA

    git add -A
    git commit --quiet -m "feat: tweak MyClass for violation comments demo"
    git push --quiet origin "$FEATURE_BRANCH"
  )
}

ensure_pull_request() {
  local existing
  existing=$(curl -sf -H "Authorization: token $GITEA_TOKEN" \
      "$GITEA_URL/api/v1/repos/$GITEA_OWNER/$GITEA_REPO/pulls?state=open" \
      | jq -r "[.[] | select(.head.ref == \"$FEATURE_BRANCH\")][0].number // empty")
  if [ -n "$existing" ]; then
    log "Pull request #$existing already open for '$FEATURE_BRANCH'."
    GITEA_PR_INDEX="$existing"
    return
  fi

  log "Opening pull request from '$FEATURE_BRANCH' into 'main'..."
  local response
  response=$(curl -sf -H "Authorization: token $GITEA_TOKEN" -H "Content-Type: application/json" \
    -X POST "$GITEA_URL/api/v1/repos/$GITEA_OWNER/$GITEA_REPO/pulls" \
    -d "{\"head\":\"$FEATURE_BRANCH\",\"base\":\"main\",\"title\":\"Violations demo PR\",\"body\":\"Used to develop/test violation-comments-to-gitea-command-line.\"}")
  GITEA_PR_INDEX=$(echo "$response" | jq -r .number)
}

ensure_admin_user
ensure_api_token
ensure_repo
ensure_feature_branch
ensure_pull_request
write_env_file

log "Done. GITEA_OWNER=$GITEA_OWNER GITEA_REPO=$GITEA_REPO GITEA_PR_INDEX=$GITEA_PR_INDEX"
log "Web UI: $GITEA_URL/$GITEA_OWNER/$GITEA_REPO/pulls/$GITEA_PR_INDEX"
