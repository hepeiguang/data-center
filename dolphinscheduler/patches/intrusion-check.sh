#!/usr/bin/env bash
# intrusion-check.sh - CI validation for upstream file intrusion budget
# Usage: ./intrusion-check.sh [base-ref]
# Exit 0 = within budget, Exit 1 = budget exceeded

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PATCHES_DIR="$SCRIPT_DIR/ds-3.4.3-plugin-descriptor"
BASE_REF="${1:-main}"

# Forbidden files - PR auto-reject if modified without L3 approval
FORBIDDEN_PATTERNS=(
  "dolphinscheduler-ui/src/views/projects/task/constants/task-type.ts"
  "dolphinscheduler-ui/src/store/project/task-type.ts"
  "dolphinscheduler-ui/src/views/projects/task/components/node/format-data.ts"
  "dolphinscheduler-ui/src/views/projects/workflow/components/dag/dag.module.scss"
  "dolphinscheduler-ui/src/locales/"
  "dolphinscheduler-task-plugin/dolphinscheduler-task-api/src/main/java/org/apache/dolphinscheduler/plugin/task/api/TaskChannelFactory.java"
  "dolphinscheduler-task-plugin/dolphinscheduler-task-api/src/main/java/org/apache/dolphinscheduler/plugin/task/api/TaskChannel.java"
  "dolphinscheduler-task-plugin/dolphinscheduler-task-api/src/main/java/org/apache/dolphinscheduler/plugin/task/api/TaskPluginManager.java"
)

# Budget: max 5 upstream files, max 20 lines total
MAX_FILES=5
MAX_LINES=20

echo "=== Plugin Descriptor Intrusion Check ==="
echo "Base ref: $BASE_REF"
echo ""

# Get list of changed files (excluding new files)
CHANGED_FILES=$(git diff --name-only --diff-filter=M "$BASE_REF"...HEAD 2>/dev/null || echo "")

if [ -z "$CHANGED_FILES" ]; then
  echo "No modified files detected."
  exit 0
fi

# Check forbidden patterns
FORBIDDEN_HIT=0
echo "Checking forbidden file list..."
for pattern in "${FORBIDDEN_PATTERNS[@]}"; do
  if echo "$CHANGED_FILES" | grep -q "^$pattern"; then
    echo "  FORBIDDEN: $pattern"
    FORBIDDEN_HIT=1
  fi
done

if [ "$FORBIDDEN_HIT" -eq 1 ]; then
  echo ""
  echo "FAIL: Forbidden files modified. See PRD section 5.0 for L3 approval process."
  exit 1
fi

# Count upstream file changes (files not in src/fork/ or new modules)
UPSTREAM_FILES=0
UPSTREAM_LINES=0
echo ""
echo "Counting upstream intrusion..."

while IFS= read -r file; do
  # Skip new files (L0)
  if ! git cat-file -e "$BASE_REF:$file" 2>/dev/null; then
    continue
  fi

  # Skip files in fork overlay directory (L0)
  if [[ "$file" == *"src/fork/"* ]]; then
    continue
  fi

  # Skip files in new modules (L0)
  if [[ "$file" == *"dolphinscheduler-plugin-descriptor/"* ]]; then
    continue
  fi

  # Count lines changed in this upstream file
  LINES=$(git diff --numstat "$BASE_REF"...HEAD -- "$file" 2>/dev/null | awk '{print $1 + $2}')
  if [ -n "$LINES" ] && [ "$LINES" -gt 0 ]; then
    UPSTREAM_FILES=$((UPSTREAM_FILES + 1))
    UPSTREAM_LINES=$((UPSTREAM_LINES + LINES))
    echo "  UPSTREAM: $file ($LINES lines)"

    # Check if registered in patches台账
    if ! grep -rq "$(basename "$file")" "$PATCHES_DIR/README.md" 2>/dev/null; then
      echo "    WARNING: Not registered in patches台账!"
    fi
  fi
done <<< "$CHANGED_FILES"

echo ""
echo "=== Summary ==="
echo "Upstream files changed: $UPSTREAM_FILES / $MAX_FILES"
echo "Upstream lines changed: $UPSTREAM_LINES / $MAX_LINES"

if [ "$UPSTREAM_FILES" -gt "$MAX_FILES" ]; then
  echo "FAIL: Too many upstream files modified."
  exit 1
fi

if [ "$UPSTREAM_LINES" -gt "$MAX_LINES" ]; then
  echo "FAIL: Too many upstream lines modified."
  exit 1
fi

echo "PASS: Within intrusion budget."
exit 0
