#!/usr/bin/env bash
set -euo pipefail

mkdir -p "$GRADLE_USER_HOME" /workspaces/.cache/android /workspaces/.cache/ccache
properties="$GRADLE_USER_HOME/gradle.properties"
touch "$properties"

ensure_property() {
  local property="$1"
  grep -qxF "$property" "$properties" || printf '%s\n' "$property" >> "$properties"
}

# These settings enable Gradle's build cache and bounded parallelism on the
# selected 4-core Codespace; project-specific configuration remains in Git.
ensure_property "org.gradle.caching=true"
ensure_property "org.gradle.parallel=true"
ensure_property "org.gradle.workers.max=3"

sdkmanager --list_installed
test -s "$ANDROID_HOME/platforms/android-37.0/android.jar"
test -x "$ANDROID_HOME/build-tools/37.0.0/apksigner"
adb version
git submodule status --recursive
printf '\nMobiDesk Android devcontainer is ready. Run tests/build explicitly.\n'
