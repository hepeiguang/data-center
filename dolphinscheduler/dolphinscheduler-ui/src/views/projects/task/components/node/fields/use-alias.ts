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

import { useI18n } from 'vue-i18n'
import type { IJsonItem, INodeData } from '../types'
import { useDagStore } from '@/store/project/dynamic/dag'
import { toRaw } from 'vue'

export function useAlias(from?: number, model: INodeData): IJsonItem {
  const { t } = useI18n()
  const oldModel = toRaw(model)
  return {
    type: 'input',
    field: 'alias',
    class: 'input-node-alias',
    name: from === 1 ? t('project.node.task_name') : t('project.node.alias'),
    props: {
      placeholder: t('project.node.alias_tips'),
      maxLength: 100
    },
    // validate: {
    //   trigger: ['input', 'blur'],
    //   required: true,
    //   message: t('project.node.alias_tips')
    // }
    validate: {
      trigger: ['input', 'blur'],
      required: true,
      // message: t('project.node.alias_tips'),
      validator(validate: any, value: string) {
        const graph = useDagStore()
        const nodes = graph?.taskDefinitionList || []

        let list = toRaw(nodes).map(i => {
          return toRaw(i)
        })

        if (list.filter(i => i.id !== oldModel.id).filter(i => i.taskParams.alias === value).length > 0) {
          return new Error('存在相同别名请重新设置')
        }

        if (!value) {
          return new Error(t('project.node.alias_tips'))
        }
      }
    }
  }
}
