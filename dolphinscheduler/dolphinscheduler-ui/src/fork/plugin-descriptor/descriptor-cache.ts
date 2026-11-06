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
 * Frontend cache for plugin descriptors fetched from the backend API.
 * Provides synchronous access to descriptor data for form rendering.
 */

import { ref, type Ref } from 'vue'
import type { DescriptorForm } from './descriptor-to-json'

export interface PluginDescriptorSummary {
  type: string
  name: string
  category: string
  descriptorStatus: 'VALID' | 'MISSING' | 'INVALID' | 'PENDING_ACTIVATION' | 'ACTIVE'
  iconUrl: string
  descriptorUrl: string
  errorMessage?: string
}

export interface PluginDescriptorDetail {
  descriptor: {
    apiVersion: string
    task: {
      type: string
      category: string
      name: string
      order?: number
      executeType?: string[]
      helpLink?: string
      descriptorVersion?: number
    }
    icon?: {
      ref?: string
      fallback?: string
    }
    locales?: Record<string, Record<string, string>>
    params?: {
      dtoClass?: string
      strict?: boolean
    }
    form: DescriptorForm['form']
  }
  descriptorStatus: string
  jarFileName?: string
  jarSha256?: string
  validationErrors?: Array<{ field: string; message: string }>
}

class DescriptorCacheService {
  private summaries: Map<string, PluginDescriptorSummary> = new Map()
  private details: Map<string, PluginDescriptorDetail> = new Map()
  private loading: Set<string> = new Set()
  private initialized = false
  private initPromise: Promise<void> | null = null

  /**
   * Initialize the cache by fetching the descriptor list from the backend.
   */
  async initialize(): Promise<void> {
    if (this.initialized) return
    if (this.initPromise) return this.initPromise

    this.initPromise = this.fetchSummaries()
    await this.initPromise
    this.initialized = true
  }

  /**
   * Fetch all descriptor summaries from the backend.
   */
  private async fetchSummaries(): Promise<void> {
    try {
      const response = await fetch('/dolphinscheduler/task/plugin-descriptors', {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' }
      })

      if (!response.ok) {
        console.warn('[plugin-descriptor] Failed to fetch summaries:', response.status)
        return
      }

      const summaries: PluginDescriptorSummary[] = await response.json()
      this.summaries.clear()
      for (const summary of summaries) {
        this.summaries.set(summary.type, summary)
      }
    } catch (err) {
      console.warn('[plugin-descriptor] Failed to fetch summaries:', err)
    }
  }

  /**
   * Fetch a single descriptor detail from the backend.
   */
  async fetchDetail(taskType: string): Promise<PluginDescriptorDetail | null> {
    if (this.details.has(taskType)) {
      return this.details.get(taskType)!
    }

    if (this.loading.has(taskType)) {
      return null
    }

    this.loading.add(taskType)

    try {
      const response = await fetch(`/dolphinscheduler/task/plugin-descriptors/${taskType}`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' }
      })

      if (!response.ok) {
        return null
      }

      const detail: PluginDescriptorDetail = await response.json()
      this.details.set(taskType, detail)
      return detail
    } catch (err) {
      console.warn(`[plugin-descriptor] Failed to fetch detail for ${taskType}:`, err)
      return null
    } finally {
      this.loading.delete(taskType)
    }
  }

  /**
   * Check if a task type has a valid descriptor.
   */
  hasDescriptor(taskType: string): boolean {
    const summary = this.summaries.get(taskType)
    return summary != null && (summary.descriptorStatus === 'VALID' || summary.descriptorStatus === 'ACTIVE')
  }

  /**
   * Get the cached summary for a task type.
   */
  getSummary(taskType: string): PluginDescriptorSummary | undefined {
    return this.summaries.get(taskType)
  }

  /**
   * Get the cached detail for a task type.
   */
  getDetail(taskType: string): PluginDescriptorDetail | undefined {
    return this.details.get(taskType)
  }

  /**
   * Get all registered task types.
   */
  getAllTaskTypes(): string[] {
    return Array.from(this.summaries.keys())
  }

  /**
   * Get all summaries.
   */
  getAllSummaries(): PluginDescriptorSummary[] {
    return Array.from(this.summaries.values())
  }

  /**
   * Force a rescan on the backend and refresh the cache.
   */
  async rescan(): Promise<void> {
    try {
      await fetch('/dolphinscheduler/task/plugin-descriptors/rescan', {
        method: 'POST'
      })
      this.details.clear()
      await this.fetchSummaries()
    } catch (err) {
      console.warn('[plugin-descriptor] Failed to rescan:', err)
    }
  }

  /**
   * Get the icon URL for a task type.
   */
  getIconUrl(taskType: string): string | null {
    const summary = this.summaries.get(taskType)
    return summary?.iconUrl || null
  }

  /**
   * Check if the cache has been initialized.
   */
  isInitialized(): boolean {
    return this.initialized
  }
}

// Singleton instance
export const descriptorCache = new DescriptorCacheService()

// Reactive reference for Vue components
export const descriptorCacheReady: Ref<boolean> = ref(false)

/**
 * Initialize the descriptor cache and update the reactive ready flag.
 */
export async function initDescriptorCache(): Promise<void> {
  await descriptorCache.initialize()
  descriptorCacheReady.value = true
}
