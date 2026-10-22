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

import { reactive } from 'vue'
import * as Fields from '../fields/index'
import type { IJsonItem, INodeData, ITaskData } from '../types'
import { useRoute, useRouter } from 'vue-router'
export function useFlexibleHttp({
  projectCode,
  from = 0,
  readonly,
  data
}: {
  projectCode: number
  from?: number
  readonly?: boolean
  data?: ITaskData
}) {
  const model = reactive({
    taskType: 'FLEXIBLE_HTTP',
    name: '',
    alias: '',
    flag: 'YES',
    description: '',
    timeoutFlag: false,
    localParams: [],
    environmentCode: null,
    failRetryInterval: 1,
    failRetryTimes: 0,
    workerGroup: 'default',
    delayTime: 0,
    timeout: 30,
    httpMethod: 'GET',
    httpBody: '',
    httpCheckCondition: 'STATUS_CODE_DEFAULT',
    httpParams: [],
    url: '',
    condition: '',
    connectTimeout: 60000,
    socketTimeout: 60000,
    timeoutNotifyStrategy: ['WARN'],
    flexibleHttpTaskType:'SIMPLE_REQUEST',
    loopConditionField:'',
    loopConditionValue:'',
    loopInterval:5000,
    maxLoopNum:3,
    outputParameters:[],
    iterateParam:"",
    iterateParamPath:"",
    otherParams: [],
    trustAllCerts:false,
    sslProtocol:"",
    cipherSuites:[],
    templateId: "",
    expressionConfig: {},
    bodyExpression: '',
    headersExpression: '',
    paramsExpression: '',
    pathExpression: '',
    isOpen: false,
    isLog: false,
    logUrl: ''
  } as INodeData)
  const route = useRoute()
  // 增加给基改使用内容
  // 查看路由跳转是否有类型参数
  const queryParamsType = route.query.type
  // 组装json
  let JsonData = [
    ...Fields.useFlexibleHttp(model),
    ...Fields.useTaskDefinition({ projectCode, from, readonly, data, model }),
    // Fields.useRunFlag(),
    // Fields.useCache(),
    Fields.useDescription(),
    // Fields.useTaskPriority(),
    // Fields.useWorkerGroup(projectCode),
    // Fields.useEnvironmentName(model, !data?.id),
    // ...Fields.useTaskGroup(model, projectCode),
    // ...Fields.useFailed(),
    // Fields.useDelayTime(model),
    // ...Fields.useTimeoutAlarm(model),
    // Fields.usePreTasks()
  ]
  if (queryParamsType) {
    JsonData.unshift(Fields.useTemplateSelect(model, false))
    JsonData.unshift(Fields.useAlias(from,model))
    JsonData.unshift(Fields.useName(from))
  } else {
    JsonData.unshift(Fields.useAlias(from,model))
    JsonData.unshift(Fields.useName(from))
  }




  return {
    json: JsonData as IJsonItem[],
    model
  }
}
