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

import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormRules } from 'naive-ui'

export const useForm = () => {
  const { t } = useI18n()

  //声明初始的setunnel的参数选项,同时作为选择器的数据来源
  //如果有一天需求改变  可以直接从后端获取相关数据再进行响应式修改
  const options = [
    { label: '文字输入框', value: 'input' },
    { label: '数字输入框', value: 'input-number' },
    { label: '下拉选择框', value: 'select' }
    // 目前支持的表单
    // { label: '单选按钮', value: 'radio' },
    // { label: '多选框', value: 'checkbox' },
    // { label: '开关', value: 'switch' },
    // { label: '多行文本框', value: 'textarea' },
    // { label: '日期选择器', value: 'date' },
    // { label: '日期时间选择器', value: 'datetime' },
    // { label: '自定义组件', value: 'custom' }
  ]

  const initfunctionForm = {
    //自定义参数，用于选择类型，用于替代默认的type设置
    customType: 'HIVE',
    //hive参数
    type: 'HIVE',
    funcName: '',
    className: '',
    argTypes: '',
    database: '',
    description: '',
    resourceId: -1,
    fullName: '',

    //自定义参数，用于setunnel函数的表单校验，表单清空，上传等方法的使用
    seatunnelAppName: '', //应用名称限定英文
    seatunnelDisplayName: '', //显示名称，中文
    seatunnelArgTypes: '', //
    seatunnelDatabase: '', //
    seatunnelDescription: '', //应用描述
    seatunnelResourceId: -1,
    seatunnelFullName: '',
    //思路是：数据库增加list字段  如果hive函数则list置空  如果seatunnel函数则list填充相关参数
    list: [
      {
        id: 1,
        name: '',
        field: '',
        type: options[0].value,
        typeName: options[0].label,
        validate: {
          required: true
        },
        props: {
          placeholder: ''
        },
        option: {
          value: []
        }
      },
      {
        id: 2,
        name: '',
        field: '',
        type: options[1].value,
        typeName: options[1].label,
        validate: {
          required: true
        },
        props: {
          placeholder: ''
        },
        option: {
          value: []
        }
      },
      {
        id: 3,
        name: '',
        field: '',
        type: options[2].value,
        typeName: options[2].label,
        validate: {
          required: true
        },
        props: {
          placeholder: ''
        },
        option: {
          value: []
        }
      }
    ]
  }

  const state = reactive({
    functionFormRef: ref(),
    options: reactive(options), //变成响应式，等待后端传入相关参数
    functionForm: reactive(initfunctionForm),
    saving: false,
    //表单校验
    rules: {
      seatunnelDisplayName: {
        required: true,
        trigger: ['input', 'blur'],
        validator() {
          if (!state.functionForm.seatunnelDisplayName) {
            return new Error(t('resource.function.enter_app_name_tips'))
          }
        }
      },
      seatunnelAppName: {
        required: true,
        trigger: ['input', 'blur'],
        validator() {
          if (!state.functionForm.seatunnelAppName) {
            return new Error(t('resource.function.enter_display_name_tips'))
          }
          if (
            !/^[a-zA-Z_][a-zA-Z0-9_-]*$/.test(
              state.functionForm.seatunnelAppName
            )
          ) {
            return new Error(
              t('resource.function.enter_app_name_must_english_tips')
            )
          }
        }
      },
      seatunnelResourceId: {
        required: false,
        trigger: ['input', 'blur'],
        validator() {
          if (state.functionForm.seatunnelResourceId === -1) {
            return new Error(t('resource.function.enter_name_tips'))
          }
        }
      },
      seatunnelFullName: {
        required: true,
        trigger: ['input', 'blur'],
        validator() {
          if (state.functionForm.seatunnelFullName == '') {
            return new Error(t('resource.function.enter_name_tips'))
          }
        }
      },

      paramName: {
        required: true,
        trigger: ['input', 'blur'],
        validator() {
          //校验当前的参数名称是否重复
          const names = new Set();
          for (const item of state.functionForm.list) {
            const name = item.name;
            if (name === undefined || name === '') continue; // 跳过空值（可选）
            if (names.has(name)) {
              return new Error(t('resource.function.list_must_having_unique_params'))
            }
            names.add(name);
          }
        }
      },

      type: {
        required: true,
        trigger: ['input', 'blur'],
        validator() {
          if (!state.functionForm.type) {
            return new Error(t('resource.function.enter_name_tips'))
          }
        }
      },
      funcName: {
        required: true,
        trigger: ['input', 'blur'],
        validator() {
          if (!state.functionForm.funcName) {
            return new Error(t('resource.function.enter_name_tips'))
          }
        }
      },
      className: {
        required: true,
        trigger: ['input', 'blur'],
        validator() {
          if (!state.functionForm.className) {
            return new Error(t('resource.function.enter_name_tips'))
          }
        }
      },

      resourceId: {
        required: false,
        trigger: ['input', 'blur'],
        validator() {
          if (state.functionForm.resourceId === -1) {
            return new Error(t('resource.function.enter_name_tips'))
          }
        }
      },

      fullName: {
        required: true,
        trigger: ['input', 'blur'],
        validator() {
          if (state.functionForm.fullName == '') {
            return new Error(t('resource.function.enter_name_tips'))
          }
        }
      }
    } as FormRules
  })

  const uploadState = reactive({
    uploadFormRef: ref(),
    uploadForm: {
      name: '',
      file: '',
      description: '',
      pid: -1,
      currentDir: '/'
    },
    uploadRules: {
      pid: {
        required: true,
        trigger: ['input', 'blur'],
        validator() {
          if (uploadState.uploadForm.pid === -1) {
            return new Error(t('resource.function.enter_name_tips'))
          }
        }
      }
    } as FormRules
  })

  return {
    state,
    uploadState,
    initfunctionForm
  }
}
