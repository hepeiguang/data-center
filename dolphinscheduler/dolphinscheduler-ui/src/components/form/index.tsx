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

import { defineComponent, PropType, toRefs, h, unref } from 'vue'
import { NSpin, NGrid, NGi, NForm, NFormItemGi, FormRules, NTooltip, NIcon, NGridItem, type FormProps } from 'naive-ui'
import { QuestionCircleOutlined } from '@vicons/antd'
import { useForm } from './use-form'
import { GridProps, IFormItem, IMeta } from './types'

const props = {
  /*
meta表单元数据，包含所有字段配置
layout	GridProps	否	-	网格布局参数，传递给 NGrid
loading	boolean	否	false	控制是否显示加载动画
* interface IMeta extends Omit<FormProps, 'model' | 'rules'> {
  elements?: IFormItem[]
  model: object
  rules: IFormRules
  hint?: string
}
* */
  meta: {
    type: Object as PropType<IMeta>,
    default: {},
    required: true
  },
  layout: {
    type: Object as PropType<GridProps>
  },
  loading: {
    type: Boolean as PropType<boolean>,
    default: false
  }
}

const Form = defineComponent({
  name: 'DSForm',
  props,
  setup(props, { expose }) {
    const { state, ...rest } = useForm()
    expose({
      ...rest
    })
    return { ...toRefs(state) }
  },
  render(props: { meta: IMeta; layout?: GridProps; loading?: boolean }) {
    const { loading, layout, meta } = props
    const { elements = [], ...restFormProps } = meta
    return (
      <NSpin show={loading}>
        <NForm {...restFormProps} rules={meta.rules as FormRules} ref='formRef'>
          <NGrid {...layout}>
            {<NGridItem span={24} style="margin-bottom: 20px;"><div v-html={ meta.hint } ></div></NGridItem>}
            {elements.map((element) => {
              const { span = 24, path, widget, label, showTooltip, tooltip, ...formItemProps } = element

              return (
                <NFormItemGi
                  {...formItemProps}
                  span={unref(span) === void 0 ? 24 : unref(span)}
                  path={path}
                  key={path || String(Date.now() + Math.random())}
                >
                  {{
                    label: () => (
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <span style={{ lineHeight: '100%' }}>{label}</span>
                        {showTooltip && <NTooltip trigger="hover" v-slots={{
                          trigger: () => (
                            <NIcon>
                              <QuestionCircleOutlined />
                            </NIcon>
                          )
                        }}>
                          {/* {tooltip} */}
                          <div style={{ whiteSpace: 'pre-wrap' }}>{tooltip}</div>
                        </NTooltip>}
                      </div>
                    ),
                    default: () => (h(widget))
                  }}
                </NFormItemGi>
              )
            })}
          </NGrid>
        </NForm>
      </NSpin>
    )
  }
})

export default Form
