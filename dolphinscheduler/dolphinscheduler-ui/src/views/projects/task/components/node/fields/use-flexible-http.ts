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
import { watch,computed } from 'vue'
import { useRoute } from 'vue-router'
export function useFlexibleHttp(model: { [field: string]: any }): IJsonItem[] {
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
  watch(() => [model.bodyExpression, model.headersExpression, model.paramsExpression, model.pathExpression], (newValue, oldValue) => {
    model.expressionConfig.bodyExpression = newValue[0]
    model.expressionConfig.headersExpression = newValue[1]
    model.expressionConfig.paramsExpression = newValue[2]
    model.expressionConfig.pathExpression = newValue[3]
  })
  watch(() => model.expressionConfig, (newValue, oldValue) => {
    model.bodyExpression = newValue.bodyExpression
    model.headersExpression = newValue.headersExpression
    model.paramsExpression = newValue.paramsExpression
    model.pathExpression = newValue.pathExpression
  })
  // 监听值
  watch(() => model.flexibleHttpTaskType, (newValue, oldValue) => {
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
  })
  const simpleRequestSpan = computed(() => (model.flexibleHttpTaskType === 'LOOP_REQUEST' ? 12 : 0))
  const IterateRequestSpan = computed(() => (model.flexibleHttpTaskType === 'ITERATE_REQUEST' ? 12 : 0))
  const isOpenSpan = computed(() => (model.isOpen ? 24 : 0))
  // 查看路由跳转是否有类型参数
  const route = useRoute()
  const queryParamsType = route.query.type


  // 是否显示模板的接口地址
  const isLogSwitchSpan = computed(() => (queryParamsType && model.isOpen ? 12 : 0))
  const isLogUrlSpan = computed(() => (model.isLog && model.isOpen ? 24 : 0))
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
    {
      type: 'custom-parameters',
      field: 'httpParams',
      name: t('project.node.http_parameters'),
      children: [
        {
          type: 'input',
          field: 'prop',
          span: 6,
          props: {
            placeholder: t('project.node.prop_tips'),
            maxLength: 256
          },
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('project.node.prop_tips'))
              }

              const sameItems = model.httpParams.filter(
                (item: { prop: string }) => item.prop === value
              )

              if (sameItems.length > 1) {
                return new Error(t('project.node.prop_repeat'))
              }
            }
          }
        },
        {
          type: 'select',
          field: 'httpParametersType',
          span: 6,
          options: POSITIONS,
          value: 'PARAMETER'
        },
        {
          type: 'input',
          field: 'value',
          span: 6,
          props: {
            placeholder: t('project.node.value_tips'),
            maxLength: 256
          },
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('project.node.value_required_tips'))
              }
            }
          }
        }
      ]
    },
    {
      type: 'input',
      field: 'httpBody',
      name: t('project.node.http_body'),
      props: {
        type: 'textarea',
        placeholder: t('project.node.http_body_tips')
      }
    },
    {
      type: 'select',
      field: 'httpCheckCondition',
      name: t('project.node.http_check_condition'),
      options: HTTP_CHECK_CONDITIONS
    },
    {
      type: 'input',
      field: 'condition',
      name: t('project.node.http_condition'),
      props: {
        type: 'textarea',
        placeholder: t('project.node.http_condition_tips')
      }
    },
    {
      type: 'select',
      field: 'flexibleHttpTaskType',
      // span: 12,
      name: '类型',
      options: SUB_TASK_HTTP_TASK_TYPE
    },
    {
      type: 'input-number',
      field: 'connectTimeout',
      name: t('project.node.connect_timeout'),
      span: 12,
      props: {
        max: Math.pow(7, 10) - 1
      },
      slots: {
        suffix: () => t('project.node.ms')
      },
      validate: {
        trigger: ['input', 'blur'],
        validator(validate: any, value: string) {
          if (!Number.isInteger(parseInt(value))) {
            return new Error(
              t('project.node.connect_timeout') +
              t('project.node.positive_integer_tips')
            )
          }
        }
      }
    },
    {
      type: 'input-number',
      field: 'socketTimeout',
      name: t('project.node.socket_timeout'),
      span: 12,
      props: {
        max: Math.pow(7, 10) - 1
      },
      slots: {
        suffix: () => t('project.node.ms')
      },
      validate: {
        trigger: ['input', 'blur'],
        validator(validate: any, value: string) {
          if (!Number.isInteger(parseInt(value))) {
            return new Error(
              t('project.node.socket_timeout') +
              t('project.node.positive_integer_tips')
            )
          }
        }
      }
    },
    {
      type: 'input',
      field: 'loopConditionField',
      name: '循环条件字段（JSON路径）',
      span: simpleRequestSpan,
      displayField: "flexibleHttpTaskType",
      displayValue: "LOOP_REQUEST",
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('请输入循环条件字段（JSON路径）'))
          }
        }
      }
    },
    {
      type: 'input',
      field: 'loopConditionValue',
      name:'循环条件期望值',
      span: simpleRequestSpan,
      displayField: "flexibleHttpTaskType",
      displayValue: "LOOP_REQUEST",
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('请输入循环条件期望值'))
          }
        }
      }
    },
    {
      type: 'input-number',
      field: 'loopInterval',
      name: '循环间隔（ms）',
      span: simpleRequestSpan,
      props: {
        max: Math.pow(7, 10) - 1
      },
      slots: {
        suffix: () => t('project.node.ms')
      },
      displayField: "flexibleHttpTaskType",
      displayValue: "LOOP_REQUEST",
      validate: {
        trigger: ['input', 'blur'],
        validator(validate: any, value: string) {
          if (!Number.isInteger(parseInt(value))) {
            return new Error('请输入循环间隔')
          }
        }
      }
      
    },
    {
      type: 'input-number',
      field: 'maxLoopNum',
      name: '最大循环次数',
      span: simpleRequestSpan,
      displayField: "flexibleHttpTaskType",
      displayValue: "LOOP_REQUEST",
      validate: {
        trigger: ['input', 'blur'],
        validator(validate: any, value: string) {
          if (!Number.isInteger(parseInt(value))) {
            return new Error('请输入最大循环次数')
          }
        }
      }
    },

    {
      type: 'input',
      field: 'iterateParam',
      name: '批量遍历参数',
      displayField: "flexibleHttpTaskType",
      displayValue: "ITERATE_REQUEST",
      span: IterateRequestSpan,
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('请输入批量遍历参数'))
          }
        }
      }
    },
    {
      type: 'input',
      field: 'iterateParamPath',
      name: '批量遍历参数路径(JSON路径)',
      displayField: "flexibleHttpTaskType",
      displayValue: "ITERATE_REQUEST",
      span: IterateRequestSpan,
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('请输入批量遍历参数路径(JSON路径)'))
          }
        }
      }
    },
    {
      type: 'custom-parameters',
      field: 'outputParameters',
      name: '输出参数',
      children: [
        {
          type: 'input',
          field: 'name',
          span: 6,
          props: {
            maxLength: 256
          },
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('project.node.prop_tips'))
              }

              const sameItems = model.outputParameters.filter(
                (item: { name: string }) => item.name === value
              )

              if (sameItems.length > 1) {
                return new Error(t('project.node.prop_repeat'))
              }
            }
          }
        },
        {
          type: 'input',
          field: 'jsonPath',
          span: 6,
          props: {
            maxLength: 256
          },
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('project.node.value_required_tips'))
              }
            }
          }
        },
      ]
    },
    {
      type: 'switch',
      field: 'isOpen',
      value: false,
      name: t('是否展开高级配置'),
    },
    {
      type: 'radio',
      field: 'trustAllCerts',
      name: t('是否信任所有证书'),
      span: isOpenSpan,
      options: [
        { label: t('是'), value: true },
        { label: t('否'), value: false }
      ]
    },
    {
      type: 'input',
      field: 'sslProtocol',
      name: '指定SSL协议',
      span: isOpenSpan,
    },
    {
      type: 'multi-input',
      field: 'cipherSuites',
      name: '指定加密套件',
      span: isOpenSpan,
      props: {
        placeholder: '请输入指定加密套件',
        type: 'input',
      }
    },
    // 高级扩展
    {
      type: 'input',
      field: 'bodyExpression',
      name: 'groovy表达式',
      span: isOpenSpan,
      props: {
        placeholder: '请输入bodyExpression',
        type: 'textarea',
      }
    },
    {
      type: 'input',
      field: 'headersExpression',
      name: '',
      span: isOpenSpan,
      props: {
        placeholder: '请输入headersExpression',
        type: 'textarea',
      }
    },
    {
      type: 'input',
      field: 'paramsExpression',
      name: '',
      span: isOpenSpan,
      props: {
        placeholder: '请输入paramsExpression',
        type: 'textarea',
      }
    },
    {
      type: 'input',
      field: 'pathExpression',
      name: '',
      span: isOpenSpan,
      props: {
        placeholder: '请输入pathExpression',
        type: 'textarea',
      }
    },
    {
      type: 'switch',
      field: 'isLog',
      value: false,
      span: isLogSwitchSpan,
      name: t('是否记录接口日志'),
    },
    {
      type: 'input',
      field: 'logUrl',
      name: '接口地址',
      span: isLogUrlSpan,
      props: {
        placeholder: '请输入pathExpression',
      },
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('project.node.http_url_tips'))
          }
          if (value.search(new RegExp(/http[s]{0,1}:\/\/\S*/, 'i'))) {
            return new Error(t('接口地址需包含http或者https'))
          }
        }
      }
    },
    // 上面三个关联 
    ...useCustomParams({
      model,
      field: 'localParams',
      isSimple: true
    }),
    {
      type: 'input',
      field: 'alias',
      name: t('project.node.alias'),
      hidden: true//TODO 隐藏别名，使用总体别名参数代替输入，实际存储位置是taskparams
      // span: 12,
    },
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
