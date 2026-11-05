FROM mcr.microsoft.com/devcontainers/base:ubuntu-24.04

ARG ANDROID_CMDLINE_TOOLS_REV=15859902
ARG ANDROID_CMDLINE_TOOLS_SHA256=4e4c464f145a7512b57d088ac6c278c03c9eea610886b35a5e0804e74eedf583

ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV PATH=${PATH}:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools

RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    openjdk-21-jdk-headless build-essential nasm ccache cmake ninja-build \
    unzip zip curl wget jq git pkg-config ca-certificates file python3 \
    && rm -rf /var/lib/apt/lists/*

RUN set -eux; \
    mkdir -p "${ANDROID_HOME}/cmdline-tools"; \
    curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_CMDLINE_TOOLS_REV}_latest.zip" -o /tmp/android-tools.zip; \
    echo "${ANDROID_CMDLINE_TOOLS_SHA256}  /tmp/android-tools.zip" | sha256sum -c -; \
    mkdir -p /tmp/android-tools; \
    unzip -q /tmp/android-tools.zip -d /tmp/android-tools; \
    mv /tmp/android-tools/cmdline-tools "${ANDROID_HOME}/cmdline-tools/latest"; \
    rm -rf /tmp/android-tools /tmp/android-tools.zip; \
    yes | "${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" --sdk_root="${ANDROID_HOME}" --licenses >/dev/null; \
    "${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" --sdk_root="${ANDROID_HOME}" --install \
      "platform-tools" \
      "platforms;android-37" \
      "build-tools;37.0.0" \
      "ndk;29.0.13113456" \
      "cmake;4.1.2"; \
    chown -R vscode:vscode "${ANDROID_HOME}"

USER vscode
RUN mkdir -p /home/vscode/.gradle /home/vscode/.android
USER root
