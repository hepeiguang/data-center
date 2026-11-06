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
 * Converts plugin descriptor form fields to IJsonItem[] format
 * compatible with the existing getElementByJson form engine.
 *
 * This is the bridge between the ds-plugin/v1 YAML contract and
 * the frontend's IJsonItem rendering pipeline.
 */

import type { IJsonItem, IJsonItemParams, IType } from '@/components/form/types'

export interface DescriptorFormField {
  field: string
  type: string
  labelKey?: string
  label?: string
  required?: boolean
  defaultValue?: any
  inputType?: string
  placeholder?: string
  tipKey?: string
  options?: Array<{ label: string; value: string } | string>
  optionsFrom?: string
  validate?: {
    required?: boolean
    trigger?: string
    type?: string
    message?: string
    min?: number
    max?: number
    pattern?: string
  }
  when?: Record<string, any>
  widget?: string
  widgetMinVersion?: number
  props?: Record<string, any>
  language?: string
  span?: number
  children?: DescriptorFormField[]
}

export interface DescriptorForm {
  form: DescriptorFormField[]
  locales?: Record<string, Record<string, string>>
  params?: {
    dtoClass?: string
    strict?: boolean
  }
}

/**
 * Map descriptor field type to IJsonItem IType.
 */
const TYPE_MAP: Record<string, IType> = {
  'input': 'input',
  'select': 'select',
  'editor': 'editor',
  'code-editor': 'editor',
  'json-editor': 'editor',
  'switch': 'switch',
  'input-number': 'input-number',
  'radio': 'radio',
  'checkbox': 'checkbox',
  'tree-select': 'tree-select',
  'custom': 'custom',
  'custom-parameters': 'custom-parameters',
  'custom-labels': 'custom-labels',
  'multi-input': 'multi-input',
  'multi-condition': 'multi-condition'
}

/**
 * Convert a single descriptor form field to IJsonItemParams.
 */
export function convertField(field: DescriptorFormField, model?: Record<string, any>): IJsonItemParams {
  const itemType = TYPE_MAP[field.type] || 'input'

  const item: IJsonItemParams = {
    field: field.field,
    name: field.labelKey || field.label || field.field,
    type: itemType,
    value: field.defaultValue,
    span: field.span
  }

  // Handle input type hints
  if (field.inputType === 'number' && itemType === 'input') {
    item.type = 'input-number'
  }

  // Handle editor language
  if ((itemType === 'editor') && field.language) {
    item.props = { ...item.props, language: field.language }
  }

  // Handle options (static)
  if (field.options) {
    item.options = field.options.map(opt => {
      if (typeof opt === 'string') {
        return { label: opt, value: opt }
      }
      return opt
    })
  }

  // Handle validation
  if (field.validate || field.required) {
    item.validate = {
      required: field.required || field.validate?.required || false,
      trigger: field.validate?.trigger || 'blur',
      message: field.validate?.message
    }
    if (field.validate?.type === 'non-empty') {
      item.validate.validator = (value: any) => {
        if (!value || (typeof value === 'string' && value.trim() === '')) {
          return new Error(field.validate?.message || `${field.label || field.field} is required`)
        }
        return true
      }
    }
  }

  // Handle custom widget
  if (itemType === 'custom' && field.widget) {
    item.props = {
      ...item.props,
      ...field.props,
      widgetName: field.widget,
      widgetMinVersion: field.widgetMinVersion
    }
  }

  // Handle children (nested fields)
  if (field.children && field.children.length > 0) {
    item.children = field.children.map(child => convertField(child, model))
  }

  return item
}

/**
 * Evaluate a 'when' condition against the current model values.
 * Returns true if the field should be visible.
 */
export function evaluateWhen(when: Record<string, any> | undefined, model: Record<string, any>): boolean {
  if (!when || Object.keys(when).length === 0) {
    return true
  }

  for (const [fieldName, expectedValue] of Object.entries(when)) {
    const actualValue = getNestedValue(model, fieldName)

    if (Array.isArray(expectedValue)) {
      if (!expectedValue.includes(actualValue)) {
        return false
      }
    } else {
      if (actualValue !== expectedValue) {
        return false
      }
    }
  }

  return true
}

/**
 * Get a nested value from an object using dot notation.
 */
function getNestedValue(obj: Record<string, any>, path: string): any {
  return path.split('.').reduce((current, key) => {
    return current && current[key] !== undefined ? current[key] : undefined
  }, obj as any)
}

/**
 * Convert a full descriptor form to IJsonItem[], applying 'when' conditions.
 */
export function convertDescriptorToJson(
  descriptor: DescriptorForm,
  model?: Record<string, any>
): IJsonItem[] {
  if (!descriptor.form || !Array.isArray(descriptor.form)) {
    return []
  }

  return descriptor.form
    .filter(field => {
      if (!model || !field.when) return true
      return evaluateWhen(field.when, model)
    })
    .map(field => convertField(field, model))
}

/**
 * Merge descriptor locales into Vue I18n at runtime.
 */
export function mergeDescriptorLocales(
  locales: Record<string, Record<string, string>> | undefined,
  mergeFn: (locale: string, messages: Record<string, any>) => void
): void {
  if (!locales) return

  for (const [locale, messages] of Object.entries(locales)) {
    mergeFn(locale, { task_components: messages })
  }
}

/**
 * Generic taskParams converter for descriptor-driven task types.
 * Replaces the per-type if/else branches in format-data.ts.
 */
export function convertModelToTaskParams(
  model: Record<string, any>,
  descriptor: DescriptorForm
): Record<string, any> {
  const taskParams: Record<string, any> = {}
  const strict = descriptor.params?.strict ?? false

  // Collect declared field names from descriptor
  const declaredFields = new Set<string>()
  if (descriptor.form) {
    for (const field of descriptor.form) {
      const rootField = field.field.split('.')[0]
      declaredFields.add(rootField)
    }
  }

  // Copy model fields to taskParams
  for (const [key, value] of Object.entries(model)) {
    if (key === 'taskParams') continue // Skip nested taskParams

    if (strict && !declaredFields.has(key)) {
      // In strict mode, skip undeclared fields
      console.warn(`[plugin-descriptor] Field "${key}" not declared in descriptor, skipping (strict mode)`)
      continue
    }

    if (value !== undefined && value !== null) {
      taskParams[key] = value
    }
  }

  return taskParams
}

/**
 * Generic taskParams reader for descriptor-driven task types.
 * Replaces the per-type if/else branches in formatModel direction.
 */
export function convertTaskParamsToModel(
  taskParams: Record<string, any> | string,
  descriptor: DescriptorForm
): Record<string, any> {
  let params: Record<string, any>

  if (typeof taskParams === 'string') {
    try {
      params = JSON.parse(taskParams)
    } catch {
      params = {}
    }
  } else {
    params = taskParams || {}
  }

  const model: Record<string, any> = {}

  // Set default values from descriptor
  if (descriptor.form) {
    for (const field of descriptor.form) {
      if (field.defaultValue !== undefined) {
        const key = field.field.split('.')[0]
        model[key] = field.defaultValue
      }
    }
  }

  // Override with actual values from taskParams
  for (const [key, value] of Object.entries(params)) {
    model[key] = value
  }

  return model
}
