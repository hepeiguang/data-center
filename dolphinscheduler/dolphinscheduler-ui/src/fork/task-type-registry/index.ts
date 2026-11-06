/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Unified task type registry that merges:
 * 1. Plugin descriptors (from JAR files, fetched via API)
 * 2. Override YAML configs (dynamic-task-type-config.yaml, static JSON files)
 * 3. Upstream hardcoded mappings (TASK_TYPES_MAP_BASE)
 *
 * Priority: descriptor > override YAML > upstream hardcoded > fallback
 *
 * This module is part of the fork overlay (L0 intrusion).
 * Upstream files (constants/task-type.ts, store/project/task-type.ts) are NOT modified.
 */

import { computed, ref, type Ref } from 'vue'
import { descriptorCache, type PluginDescriptorSummary } from '../plugin-descriptor/descriptor-cache'
import { TASK_TYPES_MAP } from '@/store/project/task-type'

export interface UnifiedTaskType {
  type: string
  alias: string
  category: string
  name: string
  iconUrl: string | null
  helpLink: string | null
  source: 'descriptor' | 'override' | 'upstream' | 'unknown'
  descriptorStatus: string | null
  helperLinkDisable: boolean
  taskExecuteType: string
}

/**
 * Build a unified view of all task types from all sources.
 */
export function buildUnifiedRegistry(): Map<string, UnifiedTaskType> {
  const registry = new Map<string, UnifiedTaskType>()

  // Layer 1: Upstream hardcoded mappings (lowest priority)
  for (const [type, info] of Object.entries(TASK_TYPES_MAP)) {
    registry.set(type, {
      type,
      alias: info.alias,
      category: 'universal', // Default category for upstream types
      name: info.alias,
      iconUrl: null,
      helpLink: null,
      source: 'upstream',
      descriptorStatus: null,
      helperLinkDisable: info.helperLinkDisable || false,
      taskExecuteType: info.taskExecuteType || 'BATCH'
    })
  }

  // Layer 2: Descriptor-based types (highest priority, overrides upstream)
  for (const summary of descriptorCache.getAllSummaries()) {
    const existing = registry.get(summary.type)
    registry.set(summary.type, {
      type: summary.type,
      alias: summary.name,
      category: summary.category,
      name: summary.name,
      iconUrl: summary.iconUrl || null,
      helpLink: null,
      source: 'descriptor',
      descriptorStatus: summary.descriptorStatus,
      helperLinkDisable: existing?.helperLinkDisable || false,
      taskExecuteType: existing?.taskExecuteType || 'BATCH'
    })
  }

  return registry
}

/**
 * Reactive unified registry that updates when descriptors change.
 */
export const unifiedTaskTypeRegistry: Ref<Map<string, UnifiedTaskType>> = ref(new Map())

/**
 * Refresh the unified registry from current descriptor cache state.
 */
export function refreshUnifiedRegistry(): void {
  unifiedTaskTypeRegistry.value = buildUnifiedRegistry()
}

/**
 * Look up a task type in the unified registry.
 */
export function lookupTaskType(type: string): UnifiedTaskType | undefined {
  return unifiedTaskTypeRegistry.value.get(type)
}

/**
 * Check if a task type has a descriptor (from JAR).
 */
export function hasDescriptor(type: string): boolean {
  return descriptorCache.hasDescriptor(type)
}

/**
 * Get all task types grouped by category.
 */
export function getTaskTypesByCategory(): Map<string, UnifiedTaskType[]> {
  const categories = new Map<string, UnifiedTaskType[]>()

  for (const taskType of unifiedTaskTypeRegistry.value.values()) {
    const cat = taskType.category || 'other'
    if (!categories.has(cat)) {
      categories.set(cat, [])
    }
    categories.get(cat)!.push(taskType)
  }

  return categories
}
