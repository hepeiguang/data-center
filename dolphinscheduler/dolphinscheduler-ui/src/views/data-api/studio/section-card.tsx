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
import { NIcon } from 'naive-ui'
import Card from '@/components/card'
import styles from './index.module.scss'

const props = {
  title: { type: String as PropType<string>, default: '' },
  icon: {
    type: [Object, Function] as PropType<any>,
    default: undefined
  },
  compact: { type: Boolean, default: false },
  contentStyle: { type: String, default: undefined }
}

/**
 * Card with an icon before the title; `extra` slot lands in the card header right side.
 */
export default defineComponent({
  name: 'SectionCard',
  props,
  render() {
    const { title, icon, compact, contentStyle, $slots } = this
    return (
      <Card
        contentStyle={
          contentStyle || `padding: ${compact ? '10px 14px' : '14px 16px'}`
        }
      >
        {{
          header: () => (
            <span class={styles['card-title']}>
              {icon ? (
                <NIcon class={styles['card-icon']} component={icon} />
              ) : null}
              {title}
            </span>
          ),
          'header-extra': () => $slots.extra?.(),
          default: () => $slots.default?.()
        }}
      </Card>
    )
  }
})
