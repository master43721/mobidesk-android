#!/usr/bin/env bash
set -euo pipefail

mkdir -p /workspaces/.cache/gradle /workspaces/.cache/android /workspaces/.cache/ccache
mkdir -p "$GRADLE_USER_HOME"
touch "$GRADLE_USER_HOME/gradle.properties"

# Keep repository build artifacts and caches outside the tracked source tree.
grep -q '^org.gradle.caching=true$' $GRADLE_USER_HOME/gradle.properties || \
  printf '\norg.gradle.caching=true\n' >> "$HOME/.gradle/gradle.properties"
grep -q '^org.gradle.parallel=true$' "$HOME/.gradle/gradle.properties" || \
  printf 'org.gradle.parallel=true\n' >> "$HOME/.gradle/gradle.properties"
grep -q '^org.gradle.workers.max=3$' "$HOME/.gradle/gradle.properties" || \
  printf 'org.gradle.workers.max=3\n' >> "$HOME/.gradle/gradle.properties"

sdkmanager --list_installed
adb version
git submodule status --recursive
printf '\nMobiDesk Android devcontainer is ready. Run tests/build explicitly.\n'
