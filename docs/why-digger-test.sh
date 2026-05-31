#!/usr/bin/env bash
set -euo pipefail

# Test that why-digger.md contains required sections
file="docs/why-digger.md"

required_sections=(
  "Why Digger?"
  "Digger Principles"
  "Not For You If"
  "Scope Boundary"
  "Fast \"Should We Use It?\" Questions"
  "Important Tradeoffs"
  "Failure Modes"
)

echo "Testing $file for required sections..."

for section in "${required_sections[@]}"; do
  if ! grep -q "$section" "$file"; then
    echo "FAIL: Missing section: $section"
    exit 1
  fi
done

echo "PASS: All required sections present"
