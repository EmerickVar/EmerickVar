#!/usr/bin/env bash
set -euxo pipefail
echo 'YITA_JITPACK_PROBE_BEGIN'
uname -a
java -version
command -v gradle || true
gradle --version || true
printf 'ANDROID_HOME=%s\n' "${ANDROID_HOME:-}"
printf 'ANDROID_SDK_ROOT=%s\n' "${ANDROID_SDK_ROOT:-}"
find "${ANDROID_HOME:-/opt/android-sdk}" -maxdepth 3 -type f -name sdkmanager 2>/dev/null | head -5 || true
echo 'YITA_JITPACK_PROBE_END'
