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

import _ from 'lodash'
import { ref, reactive, SetupContext, toRaw } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  createResource,
  createUdfFunc,
  queryResourceList,
  updateUdfFunc
} from '@/service/modules/resources'
import { useAsyncState } from '@vueuse/core'
import log from '@/utils/log'
//import deepClone from '@/utils/deepClone'

export function useModal(
  state: any,
  uploadState: any,
  ctx: SetupContext<('update:show' | 'updateList')[]>
) {
  const { t } = useI18n()

  //TODO  接口搬迁
  interface SelectOptionItem {
    id: number
    paramName: string
    paramType: string // 也可以用联合类型限定为 'textInput' | 'numberInput' | 'selectOption'
    paramValue: string
    isRequired: boolean
    option?: {value:[]} // 可选字段,如果有下拉框就要返回
  }

  //创建udf函数不同类型返回不同结果
  const handleCreateFunc = async () => {
    submitRequest(
      async () =>{
        //深拷贝响应式对象
        let currentState = toRaw(state);
        // const currentState = {...state}
        //const currentState = JSON.parse(JSON.stringify(toRaw(state)))
        // const currentState = deepClone(state)
        //const currentState = copy(toRaw(state));
        //类型不同 上传时删除另一类型的数据
        if (currentState.functionForm.customType === 'HIVE'){
          currentState.functionForm.type = currentState.functionForm.customType
          Object.keys(currentState.functionForm).forEach(key => {
            if (currentState.functionForm.hasOwnProperty(key) && key.startsWith('seatunnel')) {
              //delete currentState.functionForm[key];
            }
          });
          delete currentState.functionForm["customType"];
          //置空 因为hive不需要list
          currentState.functionForm.list = JSON.stringify([]);
        }
        if (currentState.functionForm.customType === 'SEATUNNEL'){
          currentState.functionForm.type = currentState.functionForm.customType
          currentState.functionForm.funcName = currentState.functionForm.seatunnelDisplayName
          currentState.functionForm.className = currentState.functionForm.seatunnelAppName
          currentState.functionForm.argTypes = currentState.functionForm.seatunnelArgTypes
          currentState.functionForm.database = currentState.functionForm.seatunnelDatabase
          currentState.functionForm.description = currentState.functionForm.seatunnelDescription
          currentState.functionForm.resourceId = currentState.functionForm.seatunnelResourceId
          currentState.functionForm.fullName = currentState.functionForm.seatunnelFullName
          Object.keys(currentState.functionForm).forEach(key => {
            if (currentState.functionForm.hasOwnProperty(key) && key.startsWith('seatunnel')) {
              //delete currentState.functionForm[key];
            }
          });

          currentState.functionForm.list.forEach(item=> {
            console.log(item)
            if (item.type === 'input') {
              item.typeName = '文字输入框'
            }

            if (item.type === 'input-number') {
              item.typeName = '数字输入框'
            }

            if (item.type === 'select') {
              let wait_options:any[] = []
              item.typeName = '下拉选择框'+item.id.toString()
              if (item.type === 'select'){
                for (let i =0;i< item.option.value.length;i++) {
                  wait_options.push({value: item.option.value[i], label: item.option.value[i]})
                }
                item.options = wait_options
              }
            }
            item.field = item.name;
            item.props.placeholder = item.typeName;
          })

          currentState.functionForm.list = JSON.stringify(currentState.functionForm.list);
          delete currentState.functionForm.customType
        }
        await createUdfFunc({
          ...currentState.functionForm
        })
      }
    )
  }


  //表单里函数编辑确定的方法
  //todo  不同类型返回不同结果  后端未修改
  const handleRenameFunc = async (id: number) => {
    submitRequest(async () => {


      // let currentState = _.cloneDeep(state);
      // console.log(currentState)
      console.log(0)
      const currentState = toRaw(state)
      // const currentState = {...state}
      //const currentState = JSON.parse(JSON.stringify(toRaw(state)))
      // const currentState = deepClone(state)
      console.log(1)

      // const currentState = JSON.parse(JSON.stringify(state));
      // currentState.functionForm.list = JSON.stringify(currentState.functionForm.list);
      //类型不同 上传时删除另一类型的数据
      if (currentState.functionForm.customType === 'HIVE'){
        currentState.functionForm.type = currentState.functionForm.customType
        Object.keys(currentState.functionForm).forEach(key => {
          if (currentState.functionForm.hasOwnProperty(key) && key.startsWith('seatunnel')) {
            //delete currentState.functionForm[key];
          }
        });
        delete currentState.functionForm["customType"];
        //置空 因为hive不需要list
        currentState.functionForm.list = '[]';
      }
      if (currentState.functionForm.customType === 'SEATUNNEL'){
        currentState.functionForm.type = currentState.functionForm.customType
        currentState.functionForm.funcName = currentState.functionForm.seatunnelDisplayName
        currentState.functionForm.className = currentState.functionForm.seatunnelAppName
        currentState.functionForm.argTypes = currentState.functionForm.seatunnelArgTypes
        currentState.functionForm.database = currentState.functionForm.seatunnelDatabase
        currentState.functionForm.description = currentState.functionForm.seatunnelDescription
        currentState.functionForm.resourceId = currentState.functionForm.seatunnelResourceId
        currentState.functionForm.fullName = currentState.functionForm.seatunnelFullName
        Object.keys(currentState.functionForm).forEach(key => {
          if (currentState.functionForm.hasOwnProperty(key) && key.startsWith('seatunnel')) {
            //delete currentState.functionForm[key];
          }
        });
        console.log(typeof currentState.functionForm.list)
        currentState.functionForm.list.forEach(item=> {
          console.log(item)
          if (item.type === 'input') {
            item.typeName = '文字输入框'
          }

          if (item.type === 'input-number') {
            item.typeName = '数字输入框'
          }

          if (item.type === 'select') {
            let wait_options:any[] = []
            item.typeName = '下拉选择框'+item.id.toString()
            if (item.type === 'select'){
              for (let i =0;i< item.option.value.length;i++) {
                wait_options.push({value: item.option.value[i], label: item.option.value[i]})
              }
              item.options = wait_options
            }
          }
          item.field = item.name;
          item.props.placeholder = item.typeName;
        })

        currentState.functionForm.list = JSON.stringify(currentState.functionForm.list);
        delete currentState.functionForm.customType
      }
      await updateUdfFunc(
        {
          //...state.functionForm,
          ...currentState.functionForm,
          id
        },
        id
      )
    })
  }

  const submitRequest = async (serviceHandle: any) => {
    await state.functionFormRef.validate()

    if (state.saving) return
    state.saving = true

    try {
      await serviceHandle()
      window.$message.success(t('resource.function.success'))
      state.saving = false
      ctx.emit('updateList')
      ctx.emit('update:show')
    } catch (err) {
      state.saving = false
    }
  }

  const variables = reactive({
    uploadShow: ref(false),
    udfResourceList: [],
    udfResourceDirList: []
  })

  const filterEmptyDirectory = (list: any) => {
    const processedList = list.map((item: any) => {
      if (item.children) {
        const filteredChildren = filterEmptyDirectory(item.children)
        return {
          ...item,
          children: filteredChildren
        }
      }
      return item
    })

    // 2. 再对当前层级做过滤
    return processedList.filter((n: any) => {
      if (state.functionForm.customType === 'HIVE') {
        return (
          (/\.jar$/.test(n.name) && n.children?.length === 0 && n.customType != 1
          && n.customType != null) ||
          (!/\.jar$/.test(n.name) && n.children?.length > 0)
        )
      }

      if (state.functionForm.customType === 'SEATUNNEL') {
        return (
          (/\.jar$/.test(n.name) && n.children?.length === 0 && n.customType != 0
            && n.customType != null) ||
          (!/\.jar$/.test(n.name) && n.children?.length > 0)
        )
      }

      // 默认保留所有节点
      return true
    })
  }

  // filterJarFile
  const filterJarFile = (array: any) => {
    for (const item of array) {
      if (item.children) {
        item.children = filterJarFile(item.children)
      }
    }
    return array.filter((n: any) => !/\.jar$/.test(n.name))
  }

  // recursiveTree
  const recursiveTree = (item: any) => {
    // Recursive convenience tree structure
    item.forEach((item: any) => {
      item.children === '' ||
      item.children === undefined ||
      item.children === null ||
      item.children.length === 0
        ? delete item.children
        : recursiveTree(item.children)
    })
  }

  //获得udf列表
  const getUdfList = () => {
    const {state:state1} = useAsyncState(
      queryResourceList({ type: 'UDF', fullName: '' }).then((res: any) => {
        // let item = res
        let item = res
        let item1 = _.cloneDeep(res)

        //过滤空文件夹 保留jar  非jar有children的文件夹
        filterEmptyDirectory(item)
        item = filterEmptyDirectory(item)

        recursiveTree(item)
        recursiveTree(filterJarFile(item1))

        item1 = item1.filter((item: any) => {
          if (item.dirctory) {
            return item
          }
        })
        variables.udfResourceList = item
        variables.udfResourceDirList = item1
      }),{}
    )
    return state1
  }

  const resetUploadForm = () => {
    uploadState.uploadForm.name = ''
    uploadState.uploadForm.file = ''
    uploadState.uploadForm.pid = -1
    uploadState.uploadForm.currentDir = '/'
    uploadState.uploadForm.description = ''
  }

  const handleUploadFile = () => {
    uploadState.uploadFormRef.validate(async (valid: any) => {
      if (!valid) {
        const formData = new FormData()
        formData.append('file', uploadState.uploadForm.file)
        formData.append('type', 'UDF')
        formData.append('name', uploadState.uploadForm.name)
        formData.append('pid', uploadState.uploadForm.pid)
        formData.append('currentDir', uploadState.uploadForm.currentDir)
        formData.append('description', uploadState.uploadForm.description)

        //添加方法的表单里上传资源按钮
        formData.append('customType',state.functionForm.customType)
        const res = await createResource(formData as any)
        window.$message.success(t('resource.function.success'))
        variables.uploadShow = false
        resetUploadForm()
        getUdfList()
        state.functionForm.resourceId = res.id
      }
    })
  }

  //TODO 接口搬迁
  interface UdfParam {
    id: number
    name: string
    type: string
    typeName: string
    validate:{
      required:boolean,
    }
    props:{
      placeholder:string,
    }
    option: {
      value:[]
    }
  }

  //自定义函数  删除函数参数
  const handleDelSeatunnelParam = (id:number) => {
    const index = state.functionForm.list.findIndex((item:UdfParam) => item.id === id)
    if (index > -1) {
      // 删除指定 id 的项
      state.functionForm.list.splice(index, 1)
      // 可选：重新编号 id（保持连续）
      state.functionForm.list.forEach((item:UdfParam, i:number) => {
        item.id = i + 1
      })
    }
  }

  //自定义函数  添加函数参数
  const handleAddSeatunnelParam = () => {
    const newItem = {
      id: state.functionForm.list.length ? Math.max(...state.functionForm.list.map((i: any) => i.id)) + 1 : 1,
      name: "",
      field:'',
      type: 'input',
      typeName: '文字输入框',
      validate:{
        required:true,
      },
      props:{
        placeholder:"",
      },
      option: {
        value:[],
      }
    }
    state.functionForm.list.push(newItem)
  }

  //TODO 从后端获取相对应的下拉菜单options的值
  const getSeatunnelSelectOptions = () => {
    console.log('...')
  }


  return {
    variables,
    getUdfList,
    handleCreateFunc,
    handleRenameFunc,
    handleUploadFile,

    //返回自定义参数
    handleDelSeatunnelParam,
    handleAddSeatunnelParam,
    getSeatunnelSelectOptions,
  }
}
