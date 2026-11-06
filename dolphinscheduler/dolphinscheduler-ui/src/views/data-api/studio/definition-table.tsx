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

import { defineComponent, PropType } from 'vue'
import { NButton, NIcon, NInput, NSelect, NSwitch, NTable } from 'naive-ui'
import { DeleteOutlined, PlusOutlined } from '@vicons/antd'
import styles from './index.module.scss'

export interface DefColumn {
  key: string
  label: string
  kind?: 'text' | 'area' | 'select' | 'switch'
  width?: string
  options?: { label: string; value: string }[]
}

export type DefRow = Record<string, any>

const props = {
  columns: {
    type: Array as PropType<DefColumn[]>,
    required: true as const
  },
  value: { type: Array as PropType<DefRow[]>, default: () => [] },
  disabled: { type: Boolean, default: false },
  addLabel: { type: String, default: '' },
  emptyText: { type: String, default: '' }
}

export default defineComponent({
  name: 'DefinitionTable',
  props,
  emits: ['update:value'],
  setup(props, ctx) {
    const emitRows = (rows: DefRow[]) => ctx.emit('update:value', rows)
    const patch = (index: number, key: string, value: any) => {
      emitRows(
        (props.value || []).map((row, i) =>
          i === index ? { ...row, [key]: value } : row
        )
      )
    }
    const remove = (index: number) =>
      emitRows((props.value || []).filter((unusedRow, i) => i !== index))
    const add = () =>
      emitRows([
        ...(props.value || []),
        {
          name: '',
          value: '',
          description: '',
          required: false,
          dataType: 'String'
        }
      ])
    return { patch, remove, add }
  },
  render() {
    const {
      columns,
      value,
      disabled,
      addLabel,
      emptyText,
      patch,
      remove,
      add
    } = this
    const rows = value || []
    const renderCell = (row: DefRow, col: DefColumn, index: number) => {
      const cell = row[col.key]
      if (col.kind === 'switch') {
        return (
          <NSwitch
            size='small'
            value={!!cell}
            disabled={disabled}
            onUpdate:value={(v: boolean) => patch(index, col.key, v)}
          />
        )
      }
      if (col.kind === 'select') {
        return (
          <NSelect
            value={cell === undefined || cell === null ? null : String(cell)}
            options={col.options || []}
            disabled={disabled}
            clearable
            filterable
            tag
            onUpdate:value={(v: string) => patch(index, col.key, v)}
          />
        )
      }
      if (col.kind === 'area') {
        return (
          <NInput
            type='textarea'
            autosize={{ minRows: 1, maxRows: 4 }}
            value={cell == null ? '' : String(cell)}
            disabled={disabled}
            onUpdate:value={(v: string) => patch(index, col.key, v)}
          />
        )
      }
      return (
        <NInput
          value={cell == null ? '' : String(cell)}
          disabled={disabled}
          onUpdate:value={(v: string) => patch(index, col.key, v)}
        />
      )
    }
    return (
      <>
        <NTable size='small' singleLine class={styles['def-table']}>
          <thead>
            <tr>
              {columns.map((col) => (
                <th key={col.key} style={{ width: col.width }}>
                  {col.label}
                </th>
              ))}
              {!disabled && <th style={{ width: '48px' }}></th>}
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 && (
              <tr>
                <td colspan={disabled ? columns.length : columns.length + 1}>
                  <span class={styles['section-hint']}>{emptyText}</span>
                </td>
              </tr>
            )}
            {rows.map((row, index) => (
              <tr key={index}>
                {columns.map((col) => (
                  <td key={col.key}>{renderCell(row, col, index)}</td>
                ))}
                {!disabled && (
                  <td>
                    <NButton
                      size='small'
                      quaternary
                      type='error'
                      class='btn-delete-row'
                      onClick={() => remove(index)}
                    >
                      {{
                        icon: () => (
                          <NIcon>
                            <DeleteOutlined />
                          </NIcon>
                        )
                      }}
                    </NButton>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </NTable>
        {!disabled && (
          <NButton
            size='small'
            dashed
            class='btn-add-row'
            style={{ marginTop: '8px' }}
            onClick={add}
          >
            {{
              icon: () => (
                <NIcon>
                  <PlusOutlined />
                </NIcon>
              ),
              default: () => addLabel
            }}
          </NButton>
        )}
      </>
    )
  }
})
