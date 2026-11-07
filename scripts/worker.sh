#!/usr/bin/env bash
set -euo pipefail

# Launcher for TCMS Automation Worker using the Java 26 backend environment.
is_java_26() {
    local version
    version="$("$1" -version 2>&1)" || return 1
    [[ "$version" == *'version "26"'* || "$version" == *'version "26.'* || "$version" == *'version "26-'* ]]
}

backend_java_home="${JAVA26_HOME:-}"
if [[ -z "$backend_java_home" && -n "${JAVA_HOME:-}" ]] && is_java_26 "$JAVA_HOME/bin/java"; then
    backend_java_home="$JAVA_HOME"
fi
if [[ -z "$backend_java_home" && "$(uname -s)" == "Darwin" ]]; then
    backend_java_home="$(/usr/libexec/java_home -v 26 2>/dev/null || true)"
fi
if [[ -z "$backend_java_home" && -x "$HOME/Library/Java/JavaVirtualMachines/openjdk-26.0.1/Contents/Home/bin/java" ]]; then
    backend_java_home="$HOME/Library/Java/JavaVirtualMachines/openjdk-26.0.1/Contents/Home"
fi
if [[ -z "$backend_java_home" || ! -x "$backend_java_home/bin/java" ]] || ! is_java_26 "$backend_java_home/bin/java"; then
    echo 'JDK 26 is required. Install it or set JAVA26_HOME to its installation directory.' >&2
    exit 1
fi

export JAVA_HOME="$backend_java_home"
export PATH="$JAVA_HOME/bin:$PATH"

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

exec mvn compile exec:java -Dexec.mainClass="com.tcms.automation.worker.AutomationWorkerMain" -Dexec.args="$*"
