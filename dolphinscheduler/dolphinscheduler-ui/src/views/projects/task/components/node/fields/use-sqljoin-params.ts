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
import { Ref, computed, ref, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { IJsonItem } from '../types'
import { useTaskNodeStore } from '@/store/project/task-node'
import { useDagStore } from '@/store/project/dynamic/dag'
import {
  getDatasourceTableColumnDetailsById,
  getDatasourceTablesById,
  queryDataSource
} from '@/service/modules/data-source'
import { c } from 'naive-ui'
import dataQuality from '@/router/modules/data-quality'
import { left } from '@antv/x6/lib/registry/port-layout/line'

export function useSqljoinParams({
  model,
  Fields,
  span = 0,
}: {
  model: { [field: string]: any }
  Fields: any
  span?: Ref | number
}): IJsonItem[] {
  const { t } = useI18n()
  const options = ref({})
  const optionsSelect = ref([])
  const outOptionsleft= ref(
    [] as {
      selected: boolean
      table_name: string
      column_name: string
      new_name: string
    }[]
  )
  const outOptionsright = ref(
    [] as {
      selected: boolean
      table_name: string
      column_name: string
      new_name: string
    }[]
  )
  const dagStore = useDagStore()
  const left_table = ref([] as { label: string; value: any }[])
  const left_column = ref([] as { label: string; value: any }[])
  const right_table = ref([] as { label: string; value: any }[])
  const right_column = ref([] as { label: string; value: any }[])
     const handleMouseLeave =  () => {
      const taskDefinitionList = dagStore.getDagTaskDefinitionList
      const pretask = taskDefinitionList.find((task) => {
          return task.code == model.preTasks[0]
          })
       if (pretask.taskParams) { pretask.params = pretask.taskParams}   
       if (pretask.params.datasourceId && pretask.params.database && pretask.params.table){
            model.database = pretask.params.database
            model.tableNAMELEFT = pretask.params.table
       }else if (pretask.params.datasourceId && pretask.params.table_path) {
                model.database =pretask.params.table_path.split('.')[0]
                model.tableNAMELEFT = pretask.params.table_path.split('.')[1]
       }
      model.selectedColumns = []
      if(model.left_sql){
         model.relation = []
       setTimeout(()=>{model.relation.push({})},100)
        model.select_fields_left = []
        outOptionsleft.value = []
        getColumnList({
        datasourceId: Number(model.datasourceId),
        database: model.database,
        sql: model.left_sql
      },'left_column',true)
      }
  }
  const handleMouseLeaveright =  () => {
    const taskDefinitionList = dagStore.getDagTaskDefinitionList
      const pretask = taskDefinitionList.find((task) => {
          return task.code == model.preTasks[0]
          })
       if (pretask.taskParams) { pretask.params = pretask.taskParams}   
       if (pretask.params.datasourceId && pretask.params.database && pretask.params.table){
            model.database = pretask.params.database
            model.tableNAME = pretask.params.table
       }else if (pretask.params.datasourceId && pretask.params.table_path) {
                model.database =pretask.params.table_path.split('.')[0]
                model.tableNAME = pretask.params.table_path.split('.')[1]
       }
      model.selectedColumns = []
      if(model.right_sql){
         model.relation = []
        setTimeout(()=>{model.relation.push({})},100)
        model.select_fields_right = []
         outOptionsright.value = []
        getColumnList({
        datasourceId: Number(model.datasourceId),
        database: model.database,
        sql: model.right_sql
      },'right_column',true)
      } 

    }
  const getDataSourceList = async () => {
    if (!model.datasourceId) {
      return
    }
    const listRes = await queryDataSource(model.datasourceId)
    console.log('listRes:', listRes)
     if (listRes) {
        options.value = listRes
        optionsSelect.value = [{
        label: listRes.name,
        value: listRes.id
        }]
    }
    //页面渲染时判断表一和表二是否已经选择表然后更新关联关系选项
    if (model.left_sql) {
      const data = {
        database: options.value.database,
        datasourceId: options.value.id,
        sql: model.left_sql
      }
      getColumnList(data, 'left_column',false)
    }
    if (model.right_sql) {
      const data = {
        database: options.value.database,
        datasourceId: options.value.id,
        sql: model.right_sql
      }

      getColumnList( data,'right_column', false)
    }
    //算出表一选项内容
    left_table.value = []
    Fields.value.forEach((item: any) => {
        left_table.value.push({
        label: item.tableName.slice(item.tableName.indexOf('.') + 1),
        value: item.tableName
        })
    })
    //算出表二选项内容
    getTableList(options.value.id, options.value.database)
   
  }
  onMounted(() => {
    getDataSourceList()
  })
  //根据数据源查表
 const getTableList = async (datasourceId: number, database: string) => {
    const listRes = await getDatasourceTablesById(datasourceId, database)
    console.log(listRes)
    if (listRes && listRes.length) {
      right_table.value = []
      listRes.forEach((item: any) => {
        right_table.value.push({
          label: item.label,
          value: database + '.' + item.value
        })
      })
      console.log(right_table)
      // 2. 将 oldTable 转换为 JSON 字符串集合（用于深度比对）
      const oldEntries = new Set(
        left_table.value.map((item) => JSON.stringify(item))
      )
      // 3. 过滤 newTable，排除与 oldTable 完全相同的对象
      right_table.value = right_table.value.filter(
        (newItem) => !oldEntries.has(JSON.stringify(newItem))
      )
    }
}
  const getColumnList = async (params: any, name: string, state?: boolean) => {
  const listRes = await getDatasourceTableColumnDetailsById(params);
  
  if (listRes && listRes.length) {
    // 定义左右列映射关系
    const dir = name === 'right_column' ? 'right' : 'left';
    const TABLENAME = name === 'right_column' ? model.tableNAME : model.tableNAMELEFT;
    
    // 配置映射表
    const config = {
      right: {
        column: right_column,
        outOptions: outOptionsright,
        selectFields: 'select_fields_right'
      },
      left: {
        column: left_column,
        outOptions: outOptionsleft,
        selectFields: 'select_fields_left'
      }
    };
    
    const { column, outOptions, selectFields } = config[dir];
    
    // 更新列列表
    column.value = listRes.map(item => ({
      label: item.column_name,
      value: item.column_name
    }));
    
    // 状态处理函数
    const handleState = (isState: boolean) => {
      if (isState) {
        listRes.forEach((item: any) => {
          outOptions.value.push({
            selected: true,
            table_name: TABLENAME,
            column_name: item.column_name,
            new_name: ''
          });
          (model as any)[selectFields].push({
            selected: true,
            table_name: TABLENAME,
            column_name: item.column_name,
            new_name: ''
          });
        });
      } else {
        outOptions.value = [];
        listRes.forEach((item: any) => {
          const existing = (model as any)[selectFields].find(
            (f: any) => f.column_name === item.column_name
          );
          
          outOptions.value.push({
            selected: Boolean(existing),
            table_name: TABLENAME,
            column_name: item.column_name,
            new_name: ''
          });
        });
      }
    };
    
    handleState(!!state);
  }
};
  return [
    {
      type: 'select',
      field: 'datasourceId',
      name: t('数据源'),
      span,
      options: optionsSelect,
      props: {
        disabled: true // 添加禁用属性
      }
    },
     {
      type: 'input',
      field: 'left_sql',
      name:  t('数据集一'),
      span,
      props: {
        type: 'textarea',
        placeholder: t('示例：INSERT INTO 表名 (字段1, 字段2, 字段3) VALUES (值1, 值2, 值3)'),
        maxLength: 256,
        onBlur: handleMouseLeave
      },
      validate: {
          trigger: ['input', 'blur'],
          validator(validate: any, value: string) {
              if (!value) {
                  return new Error(t('project.node.value_required_tips'))
              }
          }
      }
    },
    {
      type: 'input',
       field: 'right_sql',
      name:  t('数据集二'),
      span,
      props: {
        type: 'textarea',
        placeholder: t('示例：INSERT INTO 表名 (字段1, 字段2, 字段3) VALUES (值1, 值2, 值3)'),
        maxLength: 256,
        onBlur: handleMouseLeaveright
      },
      validate: {
          trigger: ['input', 'blur'],
          validator(validate: any, value: string) {
              if (!value) {
                  return new Error(t('project.node.value_required_tips'))
              }
          }
      }
    },
    {
      type: 'custom-parameters',
      field: 'relation',
      name: '关联关系',
      class: 'btn-custom-parameters',
      span,
      children: [
        {
          type: 'select',
          field: 'left_column',
          options: left_column,
          span: 10,
          props: {
            clearable: true,
            placeholder: t('表一字段(必填)')
            
          },
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('project.node.left_column_required_tips'))
              }
            }
          }
        },
        {
          type: 'select',
          field: 'right_column',
          options: right_column,
          span: 10,
          props: {
            clearable: true,
            placeholder: t('表二字段(必填)')
          },
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('project.node.right_column_required_tips'))
              }
            }
          }
        }
      ]
    },
    {
      type: 'select',
      field: 'join_type',
      name: t('关联方式'),
      options: TYPE_LIST,
      span,
      props: { clearable: true },
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          console.log(model.relation)
          if (!value) {
            
            return new Error(t('project.node.join_type_required_tips'))
          }
        }
      }
      // validate: { required: true }
    },
    {
      type: 'output-field',
      field: 'select_fields_left',
      name: t('输出字段(数据集一)'),
      span,
      options: outOptionsleft,
      props: {
        isSpan:true,
        clearable: true,
      }
      // validate: { required: true }
    },
    {
      type: 'output-field',
      field: 'select_fields_right',
      name: t('输出字段(数据集二)'),
      span,
      options: outOptionsright,
      props: {
        isSpan:true,
        clearable: true,
      }
      // validate: { required: true }
    }
  ]
}

export const TYPE_LIST = [
  {
    value: 'LEFT',
    label: '左连接'
  },
  {
    value: 'RIGHT',
    label: '右连接'
  },
  {
    value: 'INNER',
    label: '内连接'
  },
  {
    value: 'FULL',
    label: '全连接'
  }
]
