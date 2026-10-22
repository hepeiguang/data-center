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
import { Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { IJsonItem } from '../types'

export function useCustomRange({
    model,
    field,
    name = 'custom_range',
    span = 24,
    Ftype
}: {
    model: { [field: string]: any }
    field: string
    name?: string
    span?: Ref | number
    Ftype?: string
}): IJsonItem[] {
    const { t } = useI18n()
    if (!model[field]?.length) {
        model[field] = [{
            range_start: '',
            range_end: '',
            range_value: '',
            _id: Date.now().toString(),
        }]
    }
    return [
      {
        type: 'custom-parameters',
        field: field,
        name: t(`映射配置`),
        tooltip:t(`示例：\n 最小值：0 \n 最大值：18 \n 值：child`),
        class: 'btn-custom-parameters',
        span,
        children: [
        {
            type: 'input-number',
            field: 'range_start',
            span: 7,
            props: {
                step: 1,      
                precision: 2 ,
                placeholder: t('初始值'),

            },
            validate: {
              trigger: ['input', 'blur'],
              required: true,
              validator(validate: any, value: number) {
                if (!value && value !== 0) {
                  return new Error(t('该字段为必填项'))
                }
                const comItems = model[field].filter(
                  (item: { range_start: number ,range_end:number}) =>item.range_start ==  value && item.range_end ?item.range_end< value:false
                )
                if (comItems.length > 0) {
                  return new Error(t('不得大于最大值'))
                }
                const sameItems = model[field].filter(
                  (item: { range_start: number ,range_end:number}) =>item.range_start < value && item.range_end > value
                )
                if (sameItems.length > 0) {
                  return new Error(t('范围值已存在'))
                }
              }
            }
        },
        {
            type: 'input-number',
            field: 'range_end',
            span: 7,
            props: {
                step: 1,     
                placeholder: t('最大值'), 
                precision: 2     
            },
            validate: {
              trigger: ['input', 'blur'],
              required: true,
              validator(validate: any, value: number) {
                if (!value && value !== 0) {
                  return new Error(t('该字段为必填项'))
                }
                const comItems = model[field].filter(
                  (item: { range_start: number ,range_end:number}) =>item.range_end == value && item.range_start? item.range_start > value : false  
                )
                if (comItems.length > 0) {
                  return new Error(t('不得小于最小值'))
                }
                const sameItems = model[field].filter(
                  (item: { range_start: number ,range_end:number}) =>item.range_start <  value && item.range_end > value
                )
                if (sameItems.length > 0) {
                  return new Error(t('范围值已存在'))
                }
              }
            }
        },
        {
            type: 'input',
            field: 'range_value',
            value: '',
            span: 7,
            props: {
                placeholder: t('值'),
            },
            validate: {
                trigger: ['input', 'blur'],
                required: true,
                validator(validate: any, value: string) {
                  if (!value) {
                    return new Error(t('该字段为必填项'))
                  }
                  if (['LONG', 'DOUBLE'].includes(Ftype)) {
                    if (isNaN(Number(value))) {
                      return new Error(t('数字类型必须输入有效数值'))
                    }
                  }
                  if (Ftype === 'BOOLEAN') {
                    if (!['true', 'false'].includes(value.toLowerCase())) {
                      return new Error(t('布尔类型必须为 true 或 false'))
                    }
                  }
                }
              }
        },]}
    ]
}

