#!/usr/bin/env bash
set -euo pipefail

echo "==> [backend] Compiling..."
mvn -B clean compile

echo "==> [backend] Running tests..."
mvn -B test

echo "==> [backend] All checks passed."
