#!/usr/bin/env bash
set -euo pipefail

# ─── Configuration ───────────────────────────────────────────────────
IMAGE_NAME="interview-me"
IMAGE_TAG="${1:-latest}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cd "${SCRIPT_DIR}"

# ─── Check if local JDK is available ────────────────────────────────
if command -v java &>/dev/null && java -version 2>&1 | grep -q "21"; then
    echo "▸ Local JDK 21 found — building JAR locally for faster Docker build..."
    ./gradlew :sboot:bootJar -x test --no-daemon

    JAR_FILE=$(find sboot/build/libs -name "sboot-*.jar" ! -name "*-plain.jar" 2>/dev/null | head -1)
    if [[ -z "${JAR_FILE}" ]]; then
        echo "✗ ERROR: JAR not found after build"
        exit 1
    fi
    echo "▸ JAR built: $(basename "${JAR_FILE}")"
else
    echo "▸ No local JDK 21 — Docker multi-stage build will compile everything"
fi

# ─── Build Docker image ─────────────────────────────────────────────
echo "▸ Building Docker image: ${IMAGE_NAME}:${IMAGE_TAG}"
docker build -t "${IMAGE_NAME}:${IMAGE_TAG}" .

echo "✓ Image built successfully: ${IMAGE_NAME}:${IMAGE_TAG}"
echo ""
echo "  Run with:  docker compose up -d"
