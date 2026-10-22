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
import { reactive, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { IJsonItem } from '../../../types'
import { useUdfs } from '../../use-udfs'
export function useSqlcolumntorowTransform(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()

  return [
      {
        type: 'custom-parameters',
        field: 'columns',
        span: 24,
        children: [
            {
              type: 'input',
              field: 'groupKey',
              span: 11,
              props: {
                placeholder: t('分组后的字段名')
              },
              validate: {
                trigger: ['input', 'trigger'],
                required: true,
                message: t('该字段为必填')
              },
            },
            {
              type: 'input',
              field: 'groupValue',
              span: 11,
              props: {
                placeholder: t('分组后的值')
              },
              validate: {
                trigger: ['input', 'trigger'],
                required: true,
                message: t('该字段为必填')
              },
            },
            {
              type: 'custom-parameters',
              field: 'fields',
              span: 22,
              children: [
                {
                  type: 'input',
                  field: 'fieldName',
                  span: 10,
                  props: {
                    placeholder: t('转换后行的字段名')
                  },
                },
                {
                  type: 'input',
                  field: 'fieldValue',
                  span: 10,
                  props: {
                    placeholder: t('转换后行的字段值取值的字段名')
                  },
                },
              ]
            },
        ]
      },
  
    // //////////////////////////////////
    useUdfs(model)
  ]
}
