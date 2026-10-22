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
import {useI18n} from 'vue-i18n'
import {useUdfs} from '../../use-udfs'
import type {IJsonItem} from '../../../types'
import {language} from '@codemirror/language'
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { queryDataSourceListPaging, getDatasourceTableColumnDetailsById } from '@/service/modules/data-source'
import { useDagStore } from '@/store/project/dynamic/dag'

export function useJavaTransform(model: { [field: string]: any }): IJsonItem[] {
  const {t} = useI18n()
   const dagStore = useDagStore()
      const fieldsSpan = computed(() => (!!model.preTasks[0] ? 24 : 0))
      const fieldsSpansm = computed(() => (!!model.preTasks[0] ? 12 : 0))
      const contrastStr = dagStore.getContrastStr
      const sortOptions = ref([] as { label: string; value: number; connectionParams?: any }[])
      const compile_language = ref([] as { label: string; value: number; connectionParams?: any }[])
      let stopWatch: (() => void) | null = null
    
        const fieldArr = ref([] as any)
        const getDataSourceList = async () => {
            const listRes = await queryDataSourceListPaging({
                pageNo: 1,
                pageSize: 100
            })
            if (listRes.totalList && listRes.totalList.length) {
              console.log(111)
                listRes.totalList.forEach((item: any) => {
                    sortOptions.value.push({ label: item.name, value: item.id, connectionParams: JSON.parse(item.connectionParams) })
                })
                // stopWatch = watch(model.range_field, (newValue, oldValue) => {
                //     if (!model.preTasks[0]) {
                //         return
                //     }
                //     if (model.range_field) {
                //       console.log(222)
                //         const taskDefinitionList = dagStore.getDagTaskDefinitionList
                //         const pretask = taskDefinitionList.find((task) => {
                //             return task.code == model.preTasks[0]
                //         })
                //         const queryParames = {
                //             datasourceId: '',
                //             database: '',
                //             tableName: ''
                //         }
                //         console.log(pretask.taskParams)
                //         if (pretask.taskParams) {
                //             pretask.params = pretask.taskParams
                //         }
                //         console.log(pretask.params.datasourceId , pretask.params.database , pretask.params.table)
                //         if (pretask.params.datasourceId && pretask.params.database && pretask.params.table) {
                //             queryParames.datasourceId = pretask.params.datasourceId
                //             queryParames.database = pretask.params.database
                //             queryParames.tableName = pretask.params.table
                //         } else if (pretask.params.datasourceId && pretask.params.table_path) {
                //             queryParames.datasourceId = pretask.params.datasourceId
                //             queryParames.database = pretask.params.table_path.split('.')[0]
                //             queryParames.tableName = pretask.params.table_path.split('.')[1]
                //         }
                //         if (!(contrastStr && JSON.stringify(queryParames) === contrastStr)) {
                //             dagStore.setContrastStr(JSON.stringify(queryParames))
                //             model.range_field = []
                //             fieldArr.value = []
                //         }
                //         model.table_name = queryParames.tableName
                //         // getColumnList(Number(queryParames.datasourceId), queryParames.database, queryParames.tableName)
                //     }
    
                // }, { deep: true, immediate: true })
            }
        }
    
        // const getColumnList = async (datasourceId: number, database: string, tableName: string) => {
        //     const listRes = await getDatasourceTableColumnDetailsById({ datasourceId, database, tableName })
        //     if (listRes && listRes.length) {
        //         listRes.forEach((item: any) => {
        //             compile_language.value.push({ label: item.column_name, value: item.column_name })
        //         })
        //         if (stopWatch) stopWatch()
        //     }
        // }
    
        onMounted(() => {
            getDataSourceList()
        })
    
        onUnmounted(() => {
            // if (stopWatch) stopWatch()
        })
  const customSpan = computed(() => (model.compile_pattern == 'ABSOLUTE_PATH' ? 24 : 0))
  const unCustomSpan = computed(() => (model.compile_pattern == 'SOURCE_CODE' && !!model.preTasks[0] ? 24 : 0))
  watch(
    () => model.compile_pattern,
    (newVal) => {
      if (newVal === 'ABSOLUTE_PATH') {
        model.source_code = ''
      } else {
        model.absolute_path = ''
      }
    }
  )


  return [
    {
      type: 'select',
      field: 'compile_language',
      name: t('开发语言'),
      span: fieldsSpansm,
      options: [
        {value: 'JAVA', label: 'JAVA'},
        {value: 'GROOVY', label: 'GROOVY'},
      ],
      validate: {
        trigger: ['input', 'trigger'],
        required: true,
        message: t('该字段为必填')
      },
    },
    {
      type: 'select',
      field: 'compile_pattern',
      span: fieldsSpansm,
      value: 'SOURCE_CODE',
      options: [
        {value: 'SOURCE_CODE', label: '源码'},
        {value: 'ABSOLUTE_PATH', label: '文件路径'},
      ],
      name: t('编译模式'),
      validate: {
        trigger: ['input', 'trigger'],
        required: true,
        message: t('该字段为必填')
      },
    },
    {
      type: 'mirror',
      field: 'source_code',
      name: t('源码'),
      value: model.compile_pattern == 'ABSOLUTE_PATH' ? '' : model.source_code,
      tooltip: t(`在代码中，你必须实现两个方法：getInlineOutputColumns和getInlineOutputFieldValues`),
      span: unCustomSpan,
      validate: {
        trigger: ['input', 'trigger'],
        required: true,
        message: t('该字段为必填')
      },
      props: {
        language: 'java',
        placeholder: t(`import org.apache.seatunnel.api.table.catalog.Column;
import org.apache.seatunnel.api.table.type.SeaTunnelRowAccessor;
import org.apache.seatunnel.api.table.catalog.*;
import org.apache.seatunnel.api.table.type.*;
import java.util.ArrayList;

public Column[] getInlineOutputColumns(CatalogTable inputCatalogTable) {
    PhysicalColumn col1 =
            PhysicalColumn.of(
             "compile_language",
             BasicType.STRING_TYPE,
             10L,
             true,
             "",
             "");
    PhysicalColumn col2 =
            PhysicalColumn.of(
             "age",
             BasicType.INT_TYPE,
             0L,
             false,
             false,
             ""
            );
    return new Column[]{
            col1, col2
    };
}
   
   
public Object[] getInlineOutputFieldValues(SeaTunnelRowAccessor inputRow) {
    Object[] fieldValues = new Object[2];
    // get age 
    Object ageField = inputRow.getField(1);
    fieldValues[0] = "JAVA";
    if (Integer.parseInt(ageField.toString()) == 20) {
        fieldValues[1] = 40;
    } else {
        fieldValues[1] = ageField;
    }
    return fieldValues;
} `),
      },
    },
    {
      type: 'input',
      field: 'absolute_path',
      name: t('文件路径'),
      value: model.compile_pattern == 'SOURCE_CODE' ? '' : model.absolute_path,
      tooltip: t('服务器上Java或Groovy文件的绝对路径：/tmp/GroovyFile'),
      span: customSpan,
      validate: {
        trigger: ['input', 'trigger'],
        required: true,
        message: t('该字段为必填')
      },
    },
  ]
}

