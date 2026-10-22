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
import { ref, onMounted, watchEffect, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { IJsonItem } from '../../../types'
import { queryDataSourceListPaging, getDatasourceTableColumnDetailsById, getDatasourceTablesById } from '@/service/modules/data-source'
export function useDorisSink(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()
  const options = ref([] as { label: string; value: number; connectionParams?: any }[])
  const tableOptions = ref([] as { label: string; value: string }[])
  const sqlSpan = computed(() => (model.is_custom_sql ? 12 : 0))


  const getDataSourceList = async () => {
    const listRes = await queryDataSourceListPaging({
      pageNo: 1,
      pageSize: 100
    })
    if (listRes.totalList && listRes.totalList.length) {
      listRes.totalList.forEach((item: any) => {
        if (item.type == 'DORIS') {
          options.value.push({ label: item.name, value: item.id, connectionParams: JSON.parse(item.connectionParams) })
        }
      })
      watchEffect(() => {
        if (model.datasourceId) {
          const activeSource = options.value.find(source => source.value === model.datasourceId)
          const database = activeSource?.connectionParams.database
          model.database = database
          database && getTableList(model.datasourceId, database)
          if (!model.is_custom_sql && model.datasourceId && model.database && model.table) {
            getColumnList({ datasourceId: Number(model.datasourceId), database: model.database, tableName: model.table })
          }
        }
      },
        {
          flush: 'post'
        })
    }
  }
  const getTableList = async (datasourceId: number, database: string) => {
    tableOptions.value = []
    const listRes = await getDatasourceTablesById(datasourceId, database)
    if (listRes && listRes.length) {
      listRes.forEach((item: any) => {
        tableOptions.value.push({ label: item.label, value: item.value })
      })
    }
  }

  const getColumnList = async (params: any) => {
        // if(params.tableName.indexOf('.') > -1){
        //   params.tableName = params.tableName.split('.')[1]
        // }
    const listRes = await getDatasourceTableColumnDetailsById(params)
    if (listRes && listRes.length) {
      // let temp: any[] = []
      // listRes.forEach((item: any) => {
      //   temp.push(item.column_name)
      // })
      // model.outputFields = temp
    }
  }

  // const handleMouseLeave = () => {
  //   if (model.is_custom_sql && model.query) {
  //     getColumnList({ datasourceId: Number(model.datasourceId), database: model.database, sql: model.query })
  //   }
  // }

  onMounted(() => {
    getDataSourceList()
  })

  const schemaSaveModeOptions = [
    { value: 'RECREATE_SCHEMA', label: '表不存在时创建，表保存时删除并重建。' },
    { value: 'CREATE_SCHEMA_WHEN_NOT_EXIST', label: '表不存在时会创建，表存在时跳过。' },
    { value: 'ERROR_WHEN_SCHEMA_NOT_EXIST', label: '表不存在时会报错。' },
    { value: 'IGNORE', label: '忽略对表的处理。' }
  ]
  const dataSaveModeOptions = [
    { value: 'DROP_DATA', label: '保留数据库结构并删除数据。' },
    { value: 'APPEND_DATA', label: '保留数据库结构，保留数据。' },
    { value: 'CUSTOM_PROCESSING', label: '用户自定义处理。' },
    { value: 'ERROR_WHEN_DATA_EXISTS', label: '有数据时报错。' }
  ]

  return [
    {
      type: 'select',
      field: 'datasourceId',
      name: t('数据源'),
      options: options,
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('project.node.value_required_tips'))
          }
        }
      },
      props: {
        filterable: true,
        tag: true
      },
      span: 24
    },
    {
      type: 'select',
      field: 'table',
      name: t('表名称'),
      options: tableOptions,
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('请选择表名称'),)
          }
        }
      },
      props: {
        filterable: true,
        tag: true
      },
      span: 24
    },
      {
      type: 'input',
      field: 'feHttpPort',
      name: t('FE节点的HTTP服务端口'),
      value: 8030,
      span: 24,
    },
    // {
    //   type: 'select',
    //   field: 'dorisReadField',
    //   name: t('表字段'),
    //   options: tableOptions,
    //   validate: {
    //     trigger: ['input', 'blur'],
    //     validator(validate: any, value: string) {
    //       if (!value) {
    //         return new Error(t('请选择表字段'),)
    //       }
    //     }
    //   },
    //   props: {
    //     filterable: true,
    //     tag: true
    //   },
    //   span: 24
    // },
    {
      type: 'select',
      field: 'schemaSaveMode',
      name: t('保存模式'),
      options: schemaSaveModeOptions,
      span: 24
    },

    {
      type: 'select',
      field: 'dataSaveMode',
      name: t('数据保存模式'),
      options: dataSaveModeOptions,
      span: 24
    },
    {
      type: 'input',
      field: 'customSql',
      name: t('前置执行生成sql'),
      props: {
        placeholder: t(''),
      },
        span: computed(() => (model.dataSaveMode == 'CUSTOM_PROCESSING' ? 24 : 0)),
    },

    // =========================================

    {
      type: 'select',
      field: 'format',
      name: t('选择塞入模式'),
      span: 24,
      options: [
        { value: 'json', label: 'json' },
        { value: 'csv', label: 'csv' },
      ],
      props: {
        placeholder: t('请选择'),
      },
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('请选择塞入模式'),)
          }
        }
      },
    },

    {
      type: 'select',
      field: 'readJsonByLine',
      name: t('是否按整行读取'),
      options: [
        { value: 'true', label: '是' },
        { value: 'false', label: '否' }
      ],
      span: computed(() => (model.format == 'json' ? 24 : 0)),
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('请选择是否按整行读取'),)
          }
        }
      },
    },

    {
      type: 'input',
      field: 'columnSeparator',
      name: t('按照字符分割'),
      props: {
        placeholder: t(''),
      },
      span: computed(() => (model.format == 'csv' ? 24 : 0)),
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('请填写按照字符分割方式'),)
          }
        }
      },
    },

    // =========================================

    {
      type: 'switch',
      field: 'sinkEnableDelete',
      name: t('是否启用删除'),
      value: 'false',
      span: 24,
    },

    {
      type: 'switch',
      field: 'needsUnsupportedTypeCasting',
      name: t('是否启用不支持的类型转换'),
      value: 'false',
      span: 24,
    
    },
    {
      type: 'switch',
      field: 'sink',
      name: t('高级功能'),
      span: 24,
    },

    {
      type: 'select',
      field: 'sinkEnable2pc',
      name: t('是否启用两阶段提交'),
      value: 'false',
      options: [
        { value: 'true', label: '是' },
        { value: 'false', label: '否' }
      ],
      span: computed(() => (model.sink == true ? 12 : 0)),
    },

    {
      type: 'input-number',
      field: 'sinkCheckInterval',
      name: t('Doris扫描数据的超时时间'),
      span: computed(() => (model.sink == true ? 12 : 0)),
      value: 10000,
      // slots: {
      //   suffix: () => t('s')
      // },
    },

    {
      type: 'input-number',
      field: 'sinkMaxRetries',
      name: t('向数据库写入记录失败时的最大重试次数'),
      span: computed(() => (model.sink == true ? 12 : 0)),
      value: 3,
      // slots: {
      //   suffix: () => t('次')
      // },
    },

    {
      type: 'input-number',
      field: 'sinkBufferSize',
      name: t('用于缓存stream load数据的缓冲区大小'),
      span: computed(() => (model.sink == true ? 12 : 0)),
      value: 262144,
    },

    {
      type: 'input-number',
      field: 'sinkBufferCount',
      name: t('用于缓存stream load数据的缓冲区计数'),
      span: computed(() => (model.sink == true ? 12 : 0)),
      value: 3,
    },

    {
      type: 'input-number',
      field: 'dorisBatchSize',
      name: t('写入doris的批量大小'),
      span: computed(() => (model.sink == true ? 12 : 0)),
      value: 1024,
    },


  ]
}
