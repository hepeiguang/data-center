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
import { useCustomParams } from '.'
import type { IJsonItem } from '../types'
import { watch, computed } from 'vue'
export function useFlexibleApi(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()

  const HTTP_CHECK_CONDITIONS = [
    {
      label: t('project.node.status_code_default'),
      value: 'STATUS_CODE_DEFAULT'
    },
    {
      label: t('project.node.status_code_custom'),
      value: 'STATUS_CODE_CUSTOM'
    },
    {
      label: t('project.node.body_contains'),
      value: 'BODY_CONTAINS'
    },
    {
      label: t('project.node.body_not_contains'),
      value: 'BODY_NOT_CONTAINS'
    }
  ]
  // 监听值
  watch(
    () => model.flexibleHttpTaskType,
    (newValue, oldValue) => {
      // switch(newValue) {
      //   case 'LOOP_REQUEST':
      //     model.loopConditionField = ''
      //     model.loopConditionValue = ''
      //     model.loopInterval = ''
      //     model.maxLoopNum = ''
      //   break;
      //   case 'ITERATE_REQUEST':
      //     model.iterateParam = ''
      //     model.iterateParamPath = ''
      //   break;
      // }
      model.bodyExpression = newValue.bodyExpression
      model.headersExpression = newValue.headersExpression
      model.paramsExpression = newValue.paramsExpression
      model.pathExpression = newValue.pathExpression
    }
  )
  const simpleRequestSpan = computed(() =>
    model.flexibleHttpTaskType === 'LOOP_REQUEST' ? 12 : 0
  )
  const IterateRequestSpan = computed(() =>
    model.flexibleHttpTaskType === 'ITERATE_REQUEST' ? 12 : 0
  )
  const isOpenSpan = computed(() => (model.isOpen ? 24 : 0))
  console.log(isOpenSpan, model.isOpen ? 24 : 0, 'isOpenSpan')
  return [
    {
      type: 'input',
      class: 'input-url-name',
      field: 'url',
      name: t('project.node.http_url'),
      props: {
        placeholder: t('project.node.http_url_tips')
      },
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('project.node.http_url_tips'))
          }
          if (value.search(new RegExp(/http[s]{0,1}:\/\/\S*/, 'i'))) {
            return new Error(t('project.node.http_url_validator'))
          }
        }
      }
    },
    {
      type: 'select',
      field: 'httpMethod',
      span: 12,
      name: t('project.node.http_method'),
      options: HTTP_METHODS
    },
    // 上面三个关联
    ...useCustomParams({
      model,
      field: 'localParams',
      isSimple: false
    }),
    {
      type: 'input',
      field: 'alias',
      name: t('project.node.alias'),
      hidden: true //TODO 隐藏别名，使用总体别名参数代替输入，实际存储位置是taskparams
      // span: 12,
    }
  ]
}

const HTTP_METHODS = [
  {
    value: 'GET',
    label: 'GET'
  },
  {
    value: 'POST',
    label: 'POST'
  },
  {
    value: 'HEAD',
    label: 'HEAD'
  },
  {
    value: 'PUT',
    label: 'PUT'
  },
  {
    value: 'DELETE',
    label: 'DELETE'
  },
  {
    value: 'PATCH',
    label: 'PATCH'
  }
]

const POSITIONS = [
  {
    value: 'PARAMETER',
    label: 'Parameter'
  },
  {
    value: 'BODY',
    label: 'Body'
  },
  {
    value: 'HEADERS',
    label: 'Headers'
  }
]

const SUB_TASK_HTTP_TASK_TYPE = [
  {
    value: 'SIMPLE_REQUEST',
    label: '普通请求'
  },
  {
    value: 'LOOP_REQUEST',
    label: '循环请求'
  },
  {
    value: 'ITERATE_REQUEST',
    label: '批量请求'
  }
]
