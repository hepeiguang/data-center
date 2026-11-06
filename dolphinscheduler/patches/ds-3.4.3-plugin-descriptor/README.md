# DS 3.4.3 Plugin Descriptor Patches

This directory tracks all L2 (single-point patch) changes made to upstream
DolphinScheduler 3.4.3 files for the plugin descriptor feature.

## Principles (from PRD section 5.0)

- **L0**: New files/modules only - no upstream changes
- **L1**: Build/assembly config additions (pom.xml, spring.factories)
- **L2**: Minimal patches to upstream files, must be registered here
- **L3**: Structural changes - **FORBIDDEN** for this feature

## Quantitative DoD (AC-11)

- Phase 0-2 total upstream file changes: **<= 5 files, <= 20 lines**
- 100% of L2 changes must have entries below
- Forbidden files (PR review auto-reject if modified):
  - `constants/task-type.ts`
  - `store/project/task-type.ts`
  - `format-data.ts`
  - `dag.module.scss`
  - `locales/*`
  - `TaskChannelFactory.java`
  - `TaskChannel.java`
  - `TaskPluginManager.java`

## Registered Patches

### P1: Remove SHELL fallback for unknown task types (R4)

**File**: `dolphinscheduler-ui/src/views/projects/task/components/node/use-task.ts`
**Lines**: ~66-67
**Change**: Remove `|| nodes.SHELL` fallback; unknown types render read-only view
**Reason**: Security fix - prevents silent parameter overwrite
**Hash**: `TODO: compute after patch`

### P2: Stop filtering unknown types from store (R4)

**File**: `dolphinscheduler-ui/src/store/project/task-type.ts`
**Line**: ~212
**Change**: Remove `.filter((type) => !!TASK_TYPES_MAP[type])`
**Reason**: Unknown types should appear with "not descriptor-ized" marker
**Hash**: `TODO: compute after patch`

## Apply Script

```bash
#!/bin/bash
# apply-patches.sh - Re-apply registered patches after upstream merge
set -euo pipefail

PATCHES_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$PATCHES_DIR/../.." && pwd)"

echo "Plugin descriptor patches are tracked in this directory."
echo "Manual review required after upstream DS version upgrade."
echo ""
echo "Registered patches:"
grep "^### P" "$PATCHES_DIR/README.md" | sed 's/### /  - /'
```

## Upgrade Replay Procedure (AC-12)

1. Tag upstream version: `git tag ds-3.4.3`
2. Merge upstream upgrade: `git merge ds-3.4.4`
3. Run CI check: `.github/workflows/intrusion-check.yml`
4. Review conflicts - all must map to entries above
5. Re-apply patches if auto-merge failed
6. Run AC-1/AC-4 regression tests
