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

import {
  defineComponent,
  toRefs,
  PropType,
  watch,
  onMounted,
  ref,
  getCurrentInstance,
  nextTick,
  reactive
} from 'vue'
import {
  NUpload,
  NIcon,
  NForm,
  NFormItem,
  NInput,
  NInputGroup,
  NRadio,
  NTreeSelect,
  NButton,
  NRadioGroup,
  NFlex,
  NSpace,
  NSelect,
  NCheckbox,
  NDynamicTags
} from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { CloudUploadOutlined } from '@vicons/antd'
import Modal from '@/components/modal'
import { useForm } from './use-form'
import { useModal } from './use-modal'
import type { IUdf } from '../types'
import _ from 'lodash'

const props = {
  //传递数据
  row: {
    type: Object as PropType<IUdf>,
    default: {}
  },
  show: {
    type: Boolean as PropType<boolean>,
    default: false
  },
  mutiSelectIsDisabled: {
    type: Boolean as PropType<boolean>,
    default: false
  }
}

export default defineComponent({
  name: 'ResourceFileFolder',
  props,
  emits: ['update:show', 'updateList'],
  setup(props, ctx) {
    const treeRef = ref()
    const { state, uploadState, initfunctionForm } = useForm()

    const {
      variables,
      handleCreateFunc,
      handleRenameFunc,
      getUdfList,
      handleUploadFile,

      //导入自定义函数
      handleDelSeatunnelParam,
      handleAddSeatunnelParam,
      getSeatunnelSelectOptions
    } = useModal(state, uploadState, ctx)

    //取消操作后如何复原数据
    const hideModal = () => {
      ctx.emit('update:show')
    }

    //点击表单确认创建函数
    const handleCreate = () => {
      console.log('请求开始：', new Date())
      handleCreateFunc()
    }

    //函数编辑表单里的确定按钮方法
    const handleRename = () => {
      handleRenameFunc(props.row.id)
    }

    const handleUpload = () => {
      console.log(`/${treeRef.value.selectedOption?.fullName}`)
      //新 uploadState.uploadForm 对象里 currentDir 属性的值
      uploadState.uploadForm.currentDir = `${treeRef.value.selectedOption?.fullName}`
      handleUploadFile()
    }

    const customRequest = ({ file }: any) => {
      uploadState.uploadForm.name = file.name
      uploadState.uploadForm.file = file.file
    }

    const trim = getCurrentInstance()?.appContext.config.globalProperties.trim

    onMounted(() => {
      getUdfList()
    })

    watch(
      //监视 和 重新传值
      () => props.row,
      () => {
        variables.uploadShow = false
        if (!props.mutiSelectIsDisabled) {
          console.log()
          nextTick(() => {
            console.log(props.mutiSelectIsDisabled)
            //state.functionForm =_.cloneDeep(initfunctionForm)
            state.functionForm.customType = 'HIVE'
            state.functionForm.funcName = ''
            state.functionForm.className = ''
            state.functionForm.resourceId = -1
            state.functionForm.fullName = ''
            state.functionForm.description = ''
            state.functionForm.database = ''
            state.functionForm.argTypes = ''
            state.functionForm.seatunnelAppName = ''
            state.functionForm.seatunnelDisplayName = ''
            state.functionForm.seatunnelArgTypes = ''
            state.functionForm.seatunnelDatabase = ''
            state.functionForm.seatunnelDescription = ''
            state.functionForm.seatunnelResourceId = -1
            state.functionForm.seatunnelFullName = ''
            state.functionForm.list = [
              {
                id: 1,
                name: '',
                field: '',
                type: 'input',
                typeName: '文字输入框',
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
              // {
              //   id: 2,
              //   name: '',
              //   field: '',
              //   type: 'input-number',
              //   typeName: '数字输入框',
              //   validate: {
              //     required: true
              //   },
              //   props: {
              //     placeholder: ''
              //   },
              //   option: {
              //     value: []
              //   }
              // },
              // {
              //   id: 3,
              //   name: '',
              //   field: '',
              //   type: 'select',
              //   typeName: '下拉选择框',
              //   validate: {
              //     required: true
              //   },
              //   props: {
              //     placeholder: ''
              //   },
              //   option: {
              //     value: []
              //   }
              // }
            ]
          })
        }
        if (props.row.type === 'HIVE') {
          state.functionForm.customType = 'HIVE'
          //state.functionForm.type = props.row.type || 'HIVE' || 'SEATUNNEL'
          state.functionForm.funcName = props.row.funcName
          state.functionForm.className = props.row.className
          state.functionForm.resourceId = props.row.resourceId || -1
          state.functionForm.fullName = props.row.resourceName || ''
          state.functionForm.description = props.row.description
          if (Array.isArray(props.row.list) && props.row.list.length > 0) {
            state.functionForm.list = _.cloneDeep(props.row.list)
          }
        }
        if ((props.row.type as 'HIVE' | 'SEATUNNEL') === 'SEATUNNEL') {
          state.functionForm.customType = 'SEATUNNEL'
          //state.functionForm.type = props.row.type || 'HIVE' || 'SEATUNNEL'
          state.functionForm.seatunnelAppName = props.row.className
          state.functionForm.seatunnelDisplayName = props.row.funcName
          state.functionForm.seatunnelArgTypes = props.row.argTypes
          state.functionForm.seatunnelDatabase = props.row.database
          state.functionForm.seatunnelDescription = props.row.description
          state.functionForm.seatunnelResourceId = props.row.resourceId || -1
          state.functionForm.seatunnelFullName = props.row.resourceName || ''
          //后端返回了字符串  前端需要转化为对象数组
          //避免报错的写法 as unknown as string
          state.functionForm.list = JSON.parse(
            props.row.list as unknown as string
          )
        }
      },
      { deep: true }
    )

    watch(
      () => state.functionForm.customType,
      () => {
        getUdfList()
      }
    )

    return {
      treeRef,
      hideModal,
      handleCreate,
      handleRename,
      customRequest,
      handleUpload,
      ...toRefs(state),
      ...toRefs(uploadState),
      ...toRefs(variables),
      trim,

      //导出自定义函数,
      handleDelSeatunnelParam,
      handleAddSeatunnelParam,
      getSeatunnelSelectOptions
    }
  },
  render() {
    const { t } = useI18n()
    return (
      <Modal
        show={this.$props.show}
        title={
          this.row.id
            ? t('resource.function.edit_udf_function')
            : t('resource.function.create_udf_function')
        }
        onCancel={this.hideModal}
        onConfirm={this.row.id ? this.handleRename : this.handleCreate}
        confirmClassName='btn-submit'
        cancelClassName='btn-cancel'
        confirmLoading={this.saving}
      >
        {/* 自定义函数选项 */}

        <NForm rules={this.rules} ref='functionFormRef'>
          <NFormItem
            required
            label={t('resource.function.type')}
            path='customType'
            style={{ marginBottom: '-1vh' }}
          >
            {/* 通过传入的mutiSelectIsDisabled控制禁用状态 */}
            <NRadioGroup
              v-model={[this.functionForm.customType, 'value']}
              name='udf-type'
              class='radio-function-type'
              disabled={this.mutiSelectIsDisabled}
            >
              <NRadio value='HIVE'> HIVE UDF </NRadio>
              <NRadio value='SEATUNNEL'> SEATUNNEL UDF</NRadio>
            </NRadioGroup>
          </NFormItem>

          {/* hive */}
          {this.functionForm.customType === 'HIVE' && (
            <div>
              <NFormItem
                label={t('resource.function.udf_function_name')}
                path='funcName'
              >
                <NInput
                  allowInput={this.trim}
                  v-model={[this.functionForm.funcName, 'value']}
                  placeholder={t(
                    'resource.function.enter_udf_unction_name_tips'
                  )}
                  class='input-function-name'
                />
              </NFormItem>

              <NFormItem
                label={t('resource.function.package_name')}
                path='className'
              >
                <NInput
                  allowInput={this.trim}
                  v-model={[this.functionForm.className, 'value']}
                  placeholder={t('resource.function.enter_package_name_tips')}
                  class='input-class-name'
                />
              </NFormItem>

              <NFormItem
                label={t('resource.function.udf_resources')}
                path='fullName'
              >
                <NInputGroup>
                  <NTreeSelect
                    options={this.udfResourceList}
                    label-field='name'
                    key-field='fullName'
                    check-strategy='child'
                    v-model={[this.functionForm.fullName, 'value']}
                    placeholder={t(
                      'resource.function.enter_select_udf_resources_tips'
                    )}
                    defaultValue={this.functionForm.fullName}
                    disabled={this.uploadShow}
                    showPath={false}
                    class='btn-udf-resource-dropdown'
                  />
                  <NButton
                    type='primary'
                    ghost
                    onClick={() => (this.uploadShow = !this.uploadShow)}
                  >
                    {t('resource.function.upload_resources')}
                  </NButton>
                </NInputGroup>
              </NFormItem>

              {this.uploadShow && (
                <NForm rules={this.uploadRules} ref='uploadFormRef'>
                  <NFormItem
                    label={t('resource.function.udf_resources_directory')}
                    path='pid'
                    show-feedback={false}
                    style={{ marginBottom: '5px' }}
                  >
                    <NTreeSelect
                      ref='treeRef'
                      options={this.udfResourceDirList}
                      label-field='fullName'
                      key-field='id'
                      v-model={[this.uploadForm.pid, 'value']}
                      placeholder={t(
                        'resource.function.enter_select_udf_resources_directory_tips'
                      )}
                      defaultValue={this.uploadForm.pid}
                    />
                  </NFormItem>
                  <NFormItem
                    label=' '
                    show-feedback={false}
                    style={{ marginBottom: '5px' }}
                  >
                    <NInputGroup>
                      <NInput
                        allowInput={this.trim}
                        v-model={[this.uploadForm.name, 'value']}
                        placeholder={t('resource.function.enter_name_tips')}
                      />
                      <NUpload
                        v-model={[this.uploadForm.file, 'value']}
                        customRequest={this.customRequest}
                        showFileList={false}
                        style={{ width: 'auto' }}
                      >
                        <NButton>
                          {t('resource.function.upload')}
                          <NIcon>
                            <CloudUploadOutlined />
                          </NIcon>
                        </NButton>
                      </NUpload>
                    </NInputGroup>
                  </NFormItem>

                  <NFormItem
                    label=' '
                    path='description'
                    show-feedback={false}
                    style={{ marginBottom: '5px' }}
                  >
                    <NInput
                      type='textarea'
                      v-model={[this.uploadForm.description, 'value']}
                      placeholder={t(
                        'resource.function.enter_description_tips'
                      )}
                      class='input-description'
                    />
                  </NFormItem>
                  <NFormItem label=' '>
                    <NButton onClick={this.handleUpload}>
                      {t('resource.function.upload_udf_resources')}
                    </NButton>
                  </NFormItem>
                </NForm>
              )}

              <NFormItem
                label={t('resource.function.instructions')}
                path='description'
              >
                <NInput
                  type='textarea'
                  v-model={[this.functionForm.description, 'value']}
                  placeholder={t('resource.function.enter_instructions_tips')}
                  class='input-description'
                />
              </NFormItem>
            </div>
          )}

          {/* SEATUNNEL */}
          {this.functionForm.customType === 'SEATUNNEL' && (
            <div>
              {/* 应用名称 */}
              <NFlex justify='space-between'>
                {/* 显示名称 */}
                <div style={{ width: '45%' }}>
                  <NFormItem
                    label={t('resource.function.udf_display_name')}
                    path='seatunnelDisplayName'
                  >
                    <NInput
                      allowInput={this.trim}
                      v-model={[
                        this.functionForm.seatunnelDisplayName,
                        'value'
                      ]}
                      placeholder={t(
                        'resource.function.enter_seatunnel_display_udf_unction_name_tips'
                      )}
                      class='input-function-name'
                    />
                  </NFormItem>
                </div>

                <div style={{ width: '45%' }}>
                  <NFormItem
                    label={t('resource.function.udf_app_name')}
                    path='seatunnelAppName'
                  >
                    <NInput
                      allowInput={this.trim}
                      v-model={[this.functionForm.seatunnelAppName, 'value']}
                      placeholder={t(
                        'resource.function.enter_seatunnel_app_udf_unction_name_tips'
                      )}
                      class='input-function-name'
                    />
                  </NFormItem>
                </div>
              </NFlex>

              {/* udf资源 */}
              <NFormItem
                label={t('resource.function.udf_resources')}
                path='seatunnelFullName'
              >
                <NInputGroup>
                  <NTreeSelect
                    options={this.udfResourceList}
                    label-field='name'
                    key-field='fullName'
                    check-strategy='child'
                    v-model={[this.functionForm.seatunnelFullName, 'value']}
                    placeholder={t(
                      'resource.function.enter_select_udf_resources_tips'
                    )}
                    defaultValue={this.functionForm.seatunnelFullName}
                    disabled={this.uploadShow}
                    showPath={false}
                    class='btn-udf-resource-dropdown'
                  />
                  <NButton
                    type='primary'
                    ghost
                    onClick={() => (this.uploadShow = !this.uploadShow)}
                  >
                    {t('resource.function.upload_resources')}
                  </NButton>
                </NInputGroup>
              </NFormItem>

              {this.uploadShow && (
                <NForm rules={this.uploadRules} ref='uploadFormRef'>
                  <NFormItem
                    label={t('resource.function.udf_resources_directory')}
                    path='pid'
                    show-feedback={false}
                    style={{ marginBottom: '5px' }}
                  >
                    <NTreeSelect
                      ref='treeRef'
                      options={this.udfResourceDirList}
                      label-field='fullName'
                      key-field='id'
                      v-model={[this.uploadForm.pid, 'value']}
                      placeholder={t(
                        'resource.function.enter_select_udf_resources_directory_tips'
                      )}
                      defaultValue={this.uploadForm.pid}
                    />
                  </NFormItem>
                  <NFormItem
                    label=' '
                    show-feedback={false}
                    style={{ marginBottom: '5px' }}
                  >
                    <NInputGroup>
                      <NInput
                        allowInput={this.trim}
                        v-model={[this.uploadForm.name, 'value']}
                        placeholder={t('resource.function.enter_name_tips')}
                      />
                      <NUpload
                        v-model={[this.uploadForm.file, 'value']}
                        customRequest={this.customRequest}
                        showFileList={false}
                        style={{ width: 'auto' }}
                      >
                        <NButton>
                          {t('resource.function.upload')}
                          <NIcon>
                            <CloudUploadOutlined />
                          </NIcon>
                        </NButton>
                      </NUpload>
                    </NInputGroup>
                  </NFormItem>

                  <NFormItem
                    label=' '
                    path='description'
                    show-feedback={false}
                    style={{ marginBottom: '5px' }}
                  >
                    <NInput
                      type='textarea'
                      v-model={[this.uploadForm.description, 'value']}
                      placeholder={t(
                        'resource.function.enter_description_tips'
                      )}
                      class='input-description'
                    />
                  </NFormItem>
                  <NFormItem label=' '>
                    <NButton onClick={this.handleUpload}>
                      {t('resource.function.upload_udf_resources')}
                    </NButton>
                  </NFormItem>
                </NForm>
              )}

              {/* 描述类 */}
              <NFormItem
                  label={t('resource.function.instructions')}
                  path='setunnelDescription'
              >
                <NInput
                    type='textarea'
                    v-model={[this.functionForm.seatunnelDescription, 'value']}
                    placeholder={t('resource.function.enter_instructions_tips')}
                    class='input-description'
                />
              </NFormItem>

              {/* udf参数设置 */}
              <NFormItem label={t('resource.function.udf_param_name')} required>
                <div style={{
                  boxSizing: 'border-box',
                  width: '100%',
                  margin: 0,
                  padding: 0
                }}>
                  {this.functionForm.list.map((param) => (
                    <div style={{height: '72px'}}>
                      <NFlex
                        key={param.id}
                        justify='start'
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          flexWrap: 'nowrap',
                          boxSizing: 'border-box',
                          width: '100%',
                          margin: 0,
                          padding: 0
                        }}
                      >
                        {/* 参数几 */}
                        <div style={{
                          width: '10%' ,
                          boxSizing: 'border-box',
                          flexShrink: 0
                        }}>参数{param.id}:</div>

                        {/* 参数名 */}
                        <div style={{
                          width: '30%',
                          boxSizing: 'border-box' ,
                          flexShrink: 0
                        }}>
                          <NFormItem
                            path={'paramName'}
                            rule={{
                              required: true,
                              trigger: ['input', 'blur'],
                              validator() {
                                if (!param.name || param.name.trim() === '') {
                                  return new Error(
                                    t(
                                      'resource.function.list_must_having_args_or_params'
                                    )
                                  )
                                }
                              }
                            }}
                          >
                            <NInput
                              allowInput={this.trim}
                              v-model={[param.name, 'value']}
                              placeholder={t(
                                'resource.function.enter_seatunnel_params_input_udf_unction_name_tips'
                              )}
                              class='input-function-name'
                            />
                          </NFormItem>
                        </div>

                        {/* 参数选择 */}

                        <div style={{
                          width: '30%',
                          boxSizing: 'border-box' ,
                          flexShrink: 0
                        }}>
                          <NSpace vertical>
                            <NSelect
                              v-model={[param.type, 'value']}
                              options={this.options}
                            />
                          </NSpace>
                        </div>

                        {/* 是否必须 */}
                        <div style={{
                          width: '13%',
                          boxSizing: 'border-box',
                          flexShrink: 0
                        }}>
                          <NSpace
                            align='center'
                          >
                            <NCheckbox
                              size='large'
                              label='必填'
                              v-model={[param.validate.required, 'checked']}
                            />
                          </NSpace>
                        </div>

                        {/* 删除本行 */}
                        <div
                          onClick={() => this.handleDelSeatunnelParam(param.id)}
                          style={{
                            width: '3%',
                            boxSizing: 'border-box' ,
                            flexShrink: 0
                        }}
                        >
                          <NFormItem>
                            <NIcon size={25}>
                              <svg
                                xmlns='http://www.w3.org/2000/svg'
                                width='24'
                                height='24'
                                viewBox='0 0 24 24'
                              >
                                <circle cx='12' cy='12' r='10' fill='#CC0000' />
                                <line
                                  x1='7'
                                  y1='12'
                                  x2='17'
                                  y2='12'
                                  stroke='white'
                                  stroke-width='2'
                                  stroke-linecap='round'
                                />
                              </svg>
                            </NIcon>
                          </NFormItem>
                        </div>
                      </NFlex>
                      {/* 选项参数 */}
                      {param.type === 'select' && (
                        <div style={{marginTop:'-50px'}}
                          key={param.id}>
                          <NFormItem>
                            <NDynamicTags
                              v-model={[param.option.value, 'value']}
                            />
                          </NFormItem>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </NFormItem>

              {/* udf参数设置 */}
              {/*<NFormItem label={t('resource.function.udf_param_name')} required*/}
              {/*           style={{ marginTop: '-5px', }}>*/}
              {/*  <div style={{ marginTop: '-3vh', }}>*/}
              {/*    { this.functionForm.list.map((param) => (*/}
              {/*      <div key={param.id}>*/}
              {/*        <div*/}
              {/*          style={{*/}
              {/*            display: 'flex',*/}
              {/*            justifyContent: 'center',*/}
              {/*            alignItems: 'center',*/}
              {/*            flexWrap: 'nowrap',*/}
              {/*            height:'65px'*/}
              {/*          }}*/}
              {/*        >*/}
              {/*          /!* 参数几 *!/*/}
              {/*          <div style={{ width: '10vh',flexShrink:'1' }}>参数{param.id}:</div>*/}

              {/*          /!* 参数名 *!/*/}
              {/*          <NFormItem*/}
              {/*            style={{ width: '18vh',flexShrink:'1' }}*/}
              {/*            path={"paramName"}*/}
              {/*            rule={{*/}
              {/*                required: true,*/}
              {/*                trigger: ['input', 'blur'],*/}
              {/*                validator() {*/}
              {/*                  if (!param.name || param.name.trim() === '') {*/}
              {/*                    return new Error(t('resource.function.list_must_having_args_or_params'))*/}
              {/*                  }*/}
              {/*                }*/}
              {/*              }}*/}
              {/*          >*/}
              {/*            <NInput*/}
              {/*              allowInput={this.trim}*/}
              {/*              v-model={[param.name, 'value']}*/}
              {/*              placeholder={t(*/}
              {/*                'resource.function.enter_seatunnel_params_input_udf_unction_name_tips'*/}
              {/*              )}*/}
              {/*              class='input-function-name'*/}
              {/*            />*/}
              {/*          </NFormItem>*/}

              {/*          /!* 参数选择 *!/*/}
              {/*          <NSpace vertical style={{ width: '18vh',flexShrink:'1' }}>*/}
              {/*            <NSelect*/}
              {/*              v-model={[param.type, 'value']}*/}
              {/*              options={this.options}*/}
              {/*            />*/}
              {/*          </NSpace>*/}

              {/*          /!* 是否必须 *!/*/}
              {/*          <NSpace itemStyle={{ flexShrink:'1' }} align='center'>*/}
              {/*            <NCheckbox*/}
              {/*              size='large'*/}
              {/*              label='必填'*/}
              {/*              v-model={[param.validate.required, 'checked']}*/}
              {/*            />*/}
              {/*          </NSpace>*/}

              {/*          /!* 删除本行 *!/*/}
              {/*          <div style={{ width: '3vh',flexShrink:'1' }}></div>*/}
              {/*          <div onClick={() => this.handleDelSeatunnelParam(param.id)}>*/}
              {/*            <NFormItem>*/}
              {/*              <NIcon size={25}>*/}
              {/*                <svg*/}
              {/*                  xmlns='http://www.w3.org/2000/svg'*/}
              {/*                  width='24'*/}
              {/*                  height='24'*/}
              {/*                  viewBox='0 0 24 24'*/}
              {/*                >*/}
              {/*                  <circle cx='12' cy='12' r='10' fill='#CC0000' />*/}
              {/*                  <line*/}
              {/*                    x1='7'*/}
              {/*                    y1='12'*/}
              {/*                    x2='17'*/}
              {/*                    y2='12'*/}
              {/*                    stroke='white'*/}
              {/*                    stroke-width='2'*/}
              {/*                    stroke-linecap='round'*/}
              {/*                  />*/}
              {/*                </svg>*/}
              {/*              </NIcon>*/}
              {/*            </NFormItem>*/}
              {/*          </div>*/}
              {/*        </div>*/}

              {/*        /!* 选项参数 *!/*/}
              {/*        {param.type=== 'select' && (*/}
              {/*          <div key={param.id}>*/}
              {/*            <NFormItem>*/}
              {/*              <div style={{width:'3vh'}}></div>*/}
              {/*              <NDynamicTags*/}
              {/*                v-model={[param.option.value, 'value']}*/}
              {/*              />*/}
              {/*            </NFormItem>*/}
              {/*          </div>*/}
              {/*        )}*/}
              {/*      </div>*/}
              {/*    ))}*/}
              {/*  </div>*/}
              {/*</NFormItem>*/}

              {/* 添加符号图标 */}
              <div
                onClick={() => this.handleAddSeatunnelParam()}
                style={{
                  width: '2px',
                  marginTop:'-20px'
                }}
              >
                <NFormItem path='seatunnelArgOptions'>
                  <NIcon size={25}>
                    <svg
                      width='25'
                      height='25'
                      xmlns='http://www.w3.org/2000/svg'
                    >
                      <circle cx='12.5' cy='12.5' r='12' fill='#4895defd' />
                      <line
                        x1='6.5'
                        y1='12.5'
                        x2='17.5'
                        y2='12.5'
                        stroke='white'
                        stroke-width='2.5'
                      />
                      <line
                        x1='12.5'
                        y1='6.5'
                        x2='12.5'
                        y2='17.5'
                        stroke='white'
                        stroke-width='2.5'
                      />
                    </svg>
                  </NIcon>
                </NFormItem>
              </div>
            </div>
          )}
        </NForm>
      </Modal>
    )
  }
})
