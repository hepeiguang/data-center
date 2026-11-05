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

import { computed } from 'vue'
import { useThemeStore } from '@/store/theme/theme'
import themeList from '@/themes'

type Tone = { background: string; borderColor: string; color: string }

const surface = (light: boolean, tokens: any) => ({
  primary: light ? tokens.common.primaryColor : tokens.common.primaryColor,
  card: light ? '#ffffff' : '#1f1f1f',
  cardBorder: light ? '#e5e7eb' : '#333639',
  headerBg: light ? '#fafbfc' : '#262626',
  tint: light ? '#f0f7ff' : '#111a2c',
  tintBorder: light ? '#d6e8ff' : '#1b3b63',
  mutedBg: light ? '#f5f6f8' : '#262626',
  divider: light ? '#f0f2f5' : '#303030',
  text: light ? '#111827' : '#e5e6eb',
  textStrong: light ? '#4b5563' : '#c3c6cc',
  textSecondary: light ? '#6b7280' : '#a3a6ad',
  textMuted: light ? '#9ca3af' : '#7d8085',
  textLabel: light ? '#8a94a6' : '#87909c',
  borderStrong: light ? '#d9d9d9' : '#4a4a4a',
  borderSoft: light ? '#e1e5eb' : '#3a3d41',
  borderChip: light ? '#d7dde6' : '#41454b',
  borderNode: light ? '#dfe3e8' : '#3d444c',
  onPrimary: light ? '#ffffff' : 'rgba(255,255,255,.92)',
  edgeLine: light ? '#b7c2cf' : '#5a6673',
  bandAlt: light ? '#fbfcfe' : '#1d1d1f'
})

const tones = (light: boolean, p: any): Record<string, Tone> => ({
  info: light
    ? { background: '#e6f4ff', borderColor: '#91caff', color: '#0958d9' }
    : { background: 'rgba(23,125,220,.22)', borderColor: 'rgba(105,183,255,.45)', color: '#69b7ff' },
  teal: light
    ? { background: '#e6fffb', borderColor: '#87e8de', color: '#13a8a8' }
    : { background: 'rgba(19,168,168,.18)', borderColor: 'rgba(92,214,214,.45)', color: '#5cd6d6' },
  success: light
    ? { background: '#f6ffed', borderColor: '#b7eb8f', color: '#389e0d' }
    : { background: 'rgba(73,170,25,.2)', borderColor: 'rgba(115,209,61,.45)', color: '#73d13d' },
  warning: light
    ? { background: '#fff7e6', borderColor: '#ffd591', color: '#d46b08' }
    : { background: 'rgba(216,150,20,.2)', borderColor: 'rgba(247,179,43,.45)', color: '#f7b32b' },
  error: light
    ? { background: '#fff1f0', borderColor: '#ffa39e', color: '#cf1322' }
    : { background: 'rgba(166,29,36,.25)', borderColor: 'rgba(255,120,117,.45)', color: '#ff7875' },
  purple: light
    ? { background: '#f9f0ff', borderColor: '#d3adf7', color: '#722ed1' }
    : { background: 'rgba(114,46,209,.22)', borderColor: 'rgba(146,84,222,.45)', color: '#b37feb' },
  neutral: light
    ? { background: '#f5f5f5', borderColor: '#d9d9d9', color: '#4b5563' }
    : { background: '#2a2a2a', borderColor: '#4a4a4a', color: '#a3a6ad' },
  plain: light
    ? { background: '#ffffff', borderColor: '#d7dde6', color: '#4b5563' }
    : { background: '#1f1f1f', borderColor: '#41454b', color: '#c3c6cc' }
})

const layerHues = (light: boolean): Record<string, string> => ({
  ODS: light ? '#fa8c16' : '#f7b32b',
  DWD: light ? '#1890ff' : '#177ddc',
  DWS: light ? '#13a8a8' : '#36cfc9',
  ADS: light ? '#52c41a' : '#73d13d',
  DIM: light ? '#722ed1' : '#9254de'
})

/** Shared colors for the data-asset pages, derived from the project theme. */
export function useAssetTheme() {
  const themeStore = useThemeStore()
  const dark = computed(() => themeStore.darkTheme)
  const tokens = computed(() => themeList[dark.value ? 'dark' : 'light'])
  const p = computed(() => surface(!dark.value, tokens.value))
  const t = computed(() => {
    const palette = p.value
    return {
      ...palette,
      tones: tones(!dark.value, tokens.value),
      layers: layerHues(!dark.value),
      hue: (name: string) => layerHues(!dark.value)[name] || (name ? '#8c8c8c' : palette.textMuted),
      tone: (name: string) =>
        tones(!dark.value, tokens.value)[name] ||
        tones(!dark.value, tokens.value).neutral,
      cardStyle: {
        background: palette.card,
        border: `1px solid ${palette.cardBorder}`,
        borderRadius: '8px'
      },
      banner: {
        borderRadius: '10px',
        border: `1px solid ${palette.tintBorder}`,
        background: dark.value
          ? 'linear-gradient(135deg, #16222f 0%, #1b2430 60%, #1a2a2a 100%)'
          : 'linear-gradient(135deg, #f2f8ff 0%, #eaf3ff 55%, #e8f6f6 100%)',
        color: palette.text
      },
      tile: {
        background: dark.value ? 'rgba(255,255,255,.06)' : 'rgba(255,255,255,.78)',
        border: `1px solid ${dark.value ? 'rgba(255,255,255,.12)' : 'rgba(255,255,255,.9)'}`,
        borderRadius: '8px'
      },
      sectionTitle: {
        fontWeight: 600,
        borderLeft: `3px solid ${palette.primary}`,
        paddingLeft: '8px'
      },
      chip: (name: string) => {
        const tone = tones(!dark.value, tokens.value)[name]
        return {
          background: tone.background,
          borderColor: tone.borderColor,
          color: tone.color
        }
      }
    }
  })
  return { dark, t, tokens }
}
