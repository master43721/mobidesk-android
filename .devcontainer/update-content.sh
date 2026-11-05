#!/usr/bin/env bash
set -euo pipefail

cd "${CONTAINER_WORKSPACE_FOLDER:-/workspaces/mobidesk-android}"

if [[ -f .gitmodules ]]; then
  git submodule sync --recursive
  git submodule update --init --recursive
fi

# Resolve the checked-in Gradle wrapper and verify Java. A full build is an
# explicit developer/CI action, not an automatic container creation side effect.
java -version
./gradlew --version
