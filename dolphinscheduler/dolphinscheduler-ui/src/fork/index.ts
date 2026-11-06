/**
 * Fork Overlay Index
 *
 * This directory contains fork implementations that override upstream modules
 * via vite.config.ts resolve.alias. The goal is to keep upstream files
 * byte-for-byte unchanged (L0 intrusion).
 *
 * When adding a new fork override:
 * 1. Create the implementation file here
 * 2. Add an alias entry in vite.config.ts resolve.alias
 * 3. Register in the alias manifest below for CI validation
 */

// Fork module manifest - used by CI to validate alias consistency
export const FORK_MODULES = {
  'plugin-descriptor': './plugin-descriptor/index.ts',
  'task-type-registry': './task-type-registry/index.ts',
} as const

export type ForkModule = keyof typeof FORK_MODULES
