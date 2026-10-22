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

package org.apache.dolphinscheduler.api.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.api.configuration.ApiConfig;
import org.apache.dolphinscheduler.api.dto.DagDataSchedule;
import org.apache.dolphinscheduler.api.dto.TableCondition;
import org.apache.dolphinscheduler.api.dto.auto.*;
import org.apache.dolphinscheduler.api.dto.treeview.Instance;
import org.apache.dolphinscheduler.api.dto.treeview.TreeViewDto;
import org.apache.dolphinscheduler.api.dto.workflow.WorkflowCreateRequest;
import org.apache.dolphinscheduler.api.dto.workflow.WorkflowFilterRequest;
import org.apache.dolphinscheduler.api.dto.workflow.WorkflowUpdateRequest;
import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.exceptions.ServiceException;
import org.apache.dolphinscheduler.api.service.*;
import org.apache.dolphinscheduler.api.utils.*;
import org.apache.dolphinscheduler.common.constants.Constants;
import org.apache.dolphinscheduler.common.enums.*;
import org.apache.dolphinscheduler.common.enums.ConditionType;
import org.apache.dolphinscheduler.common.graph.DAG;
import org.apache.dolphinscheduler.common.lifecycle.ServerLifeCycleManager;
import org.apache.dolphinscheduler.common.model.TaskNodeRelation;
import org.apache.dolphinscheduler.common.utils.CodeGenerateUtils;
import org.apache.dolphinscheduler.common.utils.CodeGenerateUtils.CodeGenerateException;
import org.apache.dolphinscheduler.common.utils.DateUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.dao.entity.*;
import org.apache.dolphinscheduler.dao.mapper.*;
import org.apache.dolphinscheduler.dao.model.PageListingResult;
import org.apache.dolphinscheduler.dao.repository.ProcessDefinitionDao;
import org.apache.dolphinscheduler.dao.repository.ProcessDefinitionLogDao;
import org.apache.dolphinscheduler.dao.repository.TaskDefinitionLogDao;
import org.apache.dolphinscheduler.dao.utils.WorkerGroupUtils;
import org.apache.dolphinscheduler.plugin.datasource.api.utils.DataSourceUtils;
import org.apache.dolphinscheduler.plugin.datasource.httpserver.param.HttpServerConnectionParam;
import org.apache.dolphinscheduler.plugin.task.api.TaskPluginManager;
import org.apache.dolphinscheduler.plugin.task.api.enums.DataType;
import org.apache.dolphinscheduler.plugin.task.api.enums.Direct;
import org.apache.dolphinscheduler.plugin.task.api.enums.SqlType;
import org.apache.dolphinscheduler.plugin.task.api.enums.TaskTimeoutStrategy;
import org.apache.dolphinscheduler.plugin.task.api.model.Property;
import org.apache.dolphinscheduler.plugin.task.api.parameters.ParametersNode;
import org.apache.dolphinscheduler.plugin.task.api.parameters.SqlParameters;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunneTransformDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormJsonPathColumnDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormJsonPathDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormSqlDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.ColumnErrorHandleWayEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.RowErrorHandleWayEnum;
import org.apache.dolphinscheduler.service.alert.ListenerEventAlertManager;
import org.apache.dolphinscheduler.service.model.TaskNode;
import org.apache.dolphinscheduler.service.process.ProcessService;
import org.apache.dolphinscheduler.spi.datasource.BaseConnectionParam;
import org.apache.dolphinscheduler.spi.enums.DbType;
import org.apache.dolphinscheduler.spi.params.base.ParamsOptions;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.stream.Collectors.toSet;
import static org.apache.dolphinscheduler.api.constants.ApiFuncIdentificationConstant.*;
import static org.apache.dolphinscheduler.api.enums.Status.PROCESS_DEFINE_NOT_EXIST;
import static org.apache.dolphinscheduler.common.constants.CommandKeyConstants.CMD_PARAM_SUB_PROCESS_DEFINE_CODE;
import static org.apache.dolphinscheduler.common.constants.Constants.*;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.*;

/**
 * process definition service impl
 */
@Service
@Slf4j
public class ProcessDefinitionServiceImpl extends BaseServiceImpl implements ProcessDefinitionService {

    private static final String RELEASESTATE = "releaseState";

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private ProcessDefinitionLogMapper processDefinitionLogMapper;

    @Autowired
    private ProcessDefinitionMapper processDefinitionMapper;

    @Autowired
    private ProcessDefinitionDao processDefinitionDao;

    @Autowired
    private ProcessDefinitionLogDao processDefinitionLogDao;
    @Lazy
    @Autowired
    private ProcessInstanceService processInstanceService;

    @Autowired
    private TaskInstanceMapper taskInstanceMapper;

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Autowired
    private ProcessService processService;

    @Autowired
    private TaskDefinitionLogDao taskDefinitionLogDao;

    @Autowired
    private ProcessTaskRelationMapper processTaskRelationMapper;

    @Autowired
    private ProcessTaskRelationLogMapper processTaskRelationLogMapper;

    @Autowired
    TaskDefinitionLogMapper taskDefinitionLogMapper;

    @Lazy
    @Autowired
    private TaskDefinitionService taskDefinitionService;

    @Autowired
    private TaskDefinitionLogService taskDefinitionLogService;

    @Autowired
    private TaskDefinitionMapper taskDefinitionMapper;

    @Lazy
    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    private DataSourceMapper dataSourceMapper;

    @Autowired
    private WorkFlowLineageService workFlowLineageService;

    @Autowired
    private MetricsCleanUpService metricsCleanUpService;

    @Autowired
    private ListenerEventAlertManager listenerEventAlertManager;

    // todo 后续需要删除
    @Lazy
    @Autowired
    private ExecutorService execService;
    @Lazy
    @Autowired
    private ProcessInstanceMapper processInstanceMapper;

    // 预编译日期时间模式（用于推断）
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    // OkHttpClient 实例
    private static final OkHttpClient client = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)  // 连接超时
            .readTimeout(60, TimeUnit.SECONDS)     // 读取超时
            .writeTimeout(60, TimeUnit.SECONDS)    // 写入超时
            .build();

    /**
     * create process definition
     *
     * @param loginUser          login user
     * @param projectCode        project code
     * @param name               process definition name
     * @param description        description
     * @param globalParams       global params
     * @param locations          locations for nodes
     * @param timeout            timeout
     * @param taskRelationJson   relation json for nodes
     * @param taskDefinitionJson taskDefinitionJson
     * @return create result code
     */
    @Override
    @Transactional
    public Map<String, Object> createProcessDefinition(User loginUser, long projectCode, String name, String description, String globalParams,
                                                       String locations, int timeout, String taskRelationJson, String taskDefinitionJson,
                                                       String otherParamsJson, ProcessExecutionTypeEnum executionType) {
        Project project = projectMapper.queryByCode(projectCode);

        // check if user have write perm for project
        Map<String, Object> result = new HashMap<>();
        boolean hasProjectAndWritePerm = projectService.hasProjectAndWritePerm(loginUser, project, result);
        if (!hasProjectAndWritePerm) {
            return result;
        }
        if (checkDescriptionLength(description)) {
            log.warn("Parameter description is too long.");
            throw new ServiceException(Status.DESCRIPTION_TOO_LONG_ERROR);
        }
        // check whether the new process define name exist
        ProcessDefinition definition = processDefinitionMapper.verifyByDefineName(project.getCode(), name);
        if (definition != null) {
            log.warn("Process definition with the same name {} already exists, processDefinitionCode:{}.", definition.getName(), definition.getCode());
            throw new ServiceException(Status.PROCESS_DEFINITION_NAME_EXIST, name);
        }
        List<TaskDefinitionLog> taskDefinitionLogs = generateTaskDefinitionList(taskDefinitionJson);
        List<ProcessTaskRelationLog> taskRelationList = generateTaskRelationList(taskRelationJson, taskDefinitionLogs);

        long processDefinitionCode = CodeGenerateUtils.genCode();
        ProcessDefinition processDefinition = new ProcessDefinition(projectCode, name, processDefinitionCode, description, globalParams, locations, timeout, loginUser.getId());
        processDefinition.setExecutionType(executionType);

        result = createDagDefine(loginUser, taskRelationList, processDefinition, taskDefinitionLogs);
        if (result.get(STATUS) == Status.SUCCESS) {
            listenerEventAlertManager.publishProcessDefinitionCreatedListenerEvent(loginUser, processDefinition, taskDefinitionLogs, taskRelationList);
        }
        return result;
    }

    private void createWorkflowValid(User user, ProcessDefinition processDefinition) {
        Project project = projectMapper.queryByCode(processDefinition.getProjectCode());
        if (project == null) {
            throw new ServiceException(Status.PROJECT_NOT_FOUND, processDefinition.getProjectCode());
        }
        // check user access for project
        projectService.checkProjectAndAuthThrowException(user, project, WORKFLOW_CREATE);

        if (checkDescriptionLength(processDefinition.getDescription())) {
            throw new ServiceException(Status.DESCRIPTION_TOO_LONG_ERROR);
        }

        // check whether the new process define name exist
        ProcessDefinition definition = processDefinitionMapper.verifyByDefineName(project.getCode(), processDefinition.getName());
        if (definition != null) {
            throw new ServiceException(Status.PROCESS_DEFINITION_NAME_EXIST, processDefinition.getName());
        }

    }

    private void syncObj2Log(User user, ProcessDefinition processDefinition) {
        ProcessDefinitionLog processDefinitionLog = new ProcessDefinitionLog(processDefinition);
        processDefinitionLog.setOperator(user.getId());
        int result = processDefinitionLogMapper.insert(processDefinitionLog);
        if (result <= 0) {
            throw new ServiceException(Status.CREATE_PROCESS_DEFINITION_LOG_ERROR);
        }
    }

    /**
     * create single process definition
     *
     * @param loginUser             login user
     * @param workflowCreateRequest the new workflow object will be created
     * @return New ProcessDefinition object created just now
     */
    @Override
    @Transactional
    public ProcessDefinition createSingleProcessDefinition(User loginUser, WorkflowCreateRequest workflowCreateRequest) {
        ProcessDefinition processDefinition = workflowCreateRequest.convert2ProcessDefinition();
        this.createWorkflowValid(loginUser, processDefinition);

        long processDefinitionCode;
        try {
            processDefinitionCode = CodeGenerateUtils.genCode();
        } catch (CodeGenerateException e) {
            throw new ServiceException(Status.INTERNAL_SERVER_ERROR_ARGS);
        }

        processDefinition.setCode(processDefinitionCode);
        processDefinition.setUserId(loginUser.getId());

        int create = processDefinitionMapper.insert(processDefinition);
        if (create <= 0) {
            throw new ServiceException(Status.CREATE_PROCESS_DEFINITION_ERROR);
        }
        this.syncObj2Log(loginUser, processDefinition);
        return processDefinition;
    }

    @Override
    @Transactional
    public Map<String, Object> createProcessDefinitionByDatasourceApis(User loginUser, long projectCode, String name,
                                                                       Integer sourceDatasourceId,
                                                                       Integer targetDatasourceId,
                                                                       int serialTaskNum, String etlTechnology,
                                                                       String deployMode, List<ApiExcelDto> apiConfigs,
                                                                       String tokenMode, String tokenBothJson,
                                                                       String acceptHeader, String protocol,
                                                                       String slowMode,
                                                                       boolean isSleep, boolean isDepend,
                                                                       String globalParams,
                                                                       String replaceRules,
                                                                       boolean enableJsonField) {
        // 1. 验证源数据源和目标数据源是否存在
        DataSource sourceDataSource = dataSourceService.getDataSourceById(sourceDatasourceId);
        if (sourceDataSource == null) {
            throw new ServiceException(Status.DATASOURCE_NOT_EXIST);
        }

        DataSource targetDataSource = dataSourceService.getDataSourceById(targetDatasourceId);
        if (targetDataSource == null) {
            throw new ServiceException(Status.DATASOURCE_NOT_EXIST);
        }

        ProcessDefinition definition = processDefinitionMapper.verifyByDefineName(projectCode, name);
        if (definition != null) {
            log.warn("Process definition with the same name {} already exists, processDefinitionCode:{}.", definition.getName(), definition.getCode());
            throw new ServiceException(Status.PROCESS_DEFINITION_NAME_EXIST, name);
        }

        String description = String.format("自动同步 %s 个 API 接口数据到目标数据源。", apiConfigs.size());

        Map<String, Object> param = buildTaskParam(loginUser, projectCode, serialTaskNum, etlTechnology,
                deployMode, apiConfigs, tokenMode, tokenBothJson, acceptHeader, protocol, slowMode, isSleep, isDepend, sourceDataSource,
                targetDataSource, replaceRules, enableJsonField);

        // 5. 转换为JSON
        String taskDefinitionJson = JSONUtils.toJsonString(param.get("taskDefinitions"));
        String taskRelationJson = JSONUtils.toJsonString(param.get("taskRelationList"));
        String locations = JSONUtils.toJsonString(param.get("locationsList"));

        // 6. 创建工作流定义
        return createProcessDefinition(loginUser,
                projectCode,
                name, description,
                globalParams != null ? globalParams : createGlobalParams(),
                locations,
                0,
                taskRelationJson,
                taskDefinitionJson,
                null,
                ProcessExecutionTypeEnum.PARALLEL);
    }

    private Map<String, Object> buildTaskParam(User loginUser, long projectCode,
                                               int serialTaskNum, String etlTechnology,
                                               String deployMode, List<ApiExcelDto> apiConfigs,
                                               String tokenMode, String tokenBothJson,
                                               String acceptHeader, String protocol,
                                               String slowMode,
                                               boolean isSleep, boolean isDepend,
                                               DataSource sourceDataSource,
                                               DataSource targetDataSource,
                                               String replaceRules,
                                               boolean enableJsonField) {
        Map<String, Object> param = new HashMap<>();

        // 3. 创建任务定义、关系和位置
        List<TaskDefinitionLog> taskDefinitions = new ArrayList<>();
        List<Map<String, Object>> taskRelationList = new ArrayList<>();
        List<Map<String, Object>> locationsList = new ArrayList<>();

        int x = 50;
        int y = 50;
        long preTaskCode = 0L;
        int preTaskVersion = 0;
        int serialTaskIndex = 0;

        for (ApiExcelDto apiConfig : apiConfigs) {
            TaskDefinitionLog taskDefinition = new TaskDefinitionLog();
            taskDefinitions.add(taskDefinition);
            long sourceTaskCode = CodeGenerateUtils.genCode();
            long sinkTaskCode = CodeGenerateUtils.genCode();
            long taskCode = CodeGenerateUtils.genCode();
            String etlTechnologyUpper = etlTechnology.toUpperCase(Locale.ROOT);
            taskDefinition.setCode(taskCode);
            taskDefinition.setTaskType(etlTechnologyUpper);
            taskDefinition.setUserId(loginUser.getId());
            taskDefinition.setProjectCode(projectCode);
            taskDefinition.setOperator(loginUser.getId());
            // 任务位置
//            locationsList.add(createTaskLocation(taskCode, x, y, 280, 180));
            int height = 2 * 80 + apiConfig.getTransforms().size() * 80;
            locationsList.add(createTaskLocation(taskCode, x, y, 300, height));
            // 任务关系
            taskRelationList.add(createTaskRelation(preTaskCode, taskCode, preTaskVersion));
            // 根据ETL技术处理任务定义
            int ctX = x + 40;
            int ctY = y + 40;
            switch (etlTechnologyUpper) {
                case "SEATUNNEL":
                    processApiTaskDefinition(taskDefinition, apiConfig, sourceDataSource, targetDataSource, deployMode,
                            tokenBothJson, acceptHeader, protocol, slowMode, tokenMode, isSleep, isDepend, replaceRules,
                            enableJsonField, sourceTaskCode, sinkTaskCode);
                    // API调用子节点位置
                    locationsList.add(createTaskLocation(sourceTaskCode, ctX, ctY, 220, 50));
                    for (SeatunnelTaskNodeVO transform : apiConfig.getTransforms()) {
                        locationsList.add(createTaskLocation(Long.valueOf(transform.getCode()), ctX, ctY += 70, 220, 50));
                    }
                    // 数据入库子节点位置
                    locationsList.add(createTaskLocation(sinkTaskCode, ctX, ctY += 70, 220, 50));
                    break;
                case "DATAX":
                    break;
            }

            // 更新任务位置关系
            preTaskCode = taskCode;

            if (++serialTaskIndex >= serialTaskNum) {
                x = 50;
                y += 350;
                preTaskVersion = 0;
                preTaskCode = 0L;
                serialTaskIndex = 0;
            } else {
                if (isSleep) {
                    // 创建Shell等待任务
                    long sleepTaskCode = CodeGenerateUtils.genCode();
                    TaskDefinitionLog sleepTask = createSleepShellTask(loginUser, projectCode, sleepTaskCode);
                    taskDefinitions.add(sleepTask);
                    taskRelationList.add(createTaskRelation(preTaskCode, sleepTaskCode, 1));
                    int waitTaskX = x + 350;  // 300宽度 + 20间距
                    int waitTaskY = y + 100; // 垂直居中
                    locationsList.add(createTaskLocation(sleepTaskCode, waitTaskX, waitTaskY, 120, 50));
                    // 3. 等待任务成为下一个任务的起点
                    preTaskCode = sleepTaskCode;
                    preTaskVersion = 1;

                    // 4. 移动X坐标
                    x = waitTaskX + 190 + 50;
                } else {
                    x += 350;
                    preTaskVersion = 1;
                }
            }

        }
        param.put("taskDefinitions", taskDefinitions);
        param.put("taskRelationList", taskRelationList);
        param.put("locationsList", locationsList);
        return param;
    }

    private String createGlobalParams() {
        List<Map<String, Object>> globalParams = new ArrayList<>();
        String[][] timeParams = {
                {"full_yesterday_start", "$[yyyy-MM-dd 00:00:00-1]", "昨天开始时间"},
                {"full_today_start", "$[yyyy-MM-dd 00:00:00]", "今天开始时间"},
                {"today", "$[yyyy-MM-dd]", "今天日期"},
                {"yesterday", "$[yyyy-MM-dd-1]", "昨天日期"},
                {"current_month", "$[yyyy-MM]", "当前月份"},
                {"last_month", "$[add_months(yyyy-MM,-1)]", "上个月份"},
                {"before30day", "$[yyyy-MM-dd-30]", "30天前日期"},
                {"timestamp_today_start", "$[timestamp_ms(yyyyMMdd000000)]", "今天开始时间的时间戳"},
                {"timestamp_yesterday_start", "$[timestamp_ms(yyyyMMdd000000-1)]", "昨日开始时间的时间戳"}
        };
        for (String[] param : timeParams) {
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("prop", param[0]);
            paramMap.put("direct", "OUT");
            paramMap.put("type", "VARCHAR");
            paramMap.put("value", param[1]);
            globalParams.add(paramMap);
        }
        return JSONUtils.toJsonString(globalParams);
    }

    /**
     * 创建Shell等待任务
     */
    private TaskDefinitionLog createSleepShellTask(User loginUser, long projectCode, long taskCode) {
        TaskDefinitionLog sleepTask = new TaskDefinitionLog();
        sleepTask.setCode(taskCode);
        sleepTask.setName("暂停一会");
        sleepTask.setVersion(1);
        sleepTask.setDescription("");
        sleepTask.setProjectCode(projectCode);
        sleepTask.setUserId(loginUser.getId());
        sleepTask.setTaskType("SHELL");
        // 设置任务参数
        Map<String, Object> taskParams = new HashMap<>();
        List<Property> properties = new ArrayList<>();
        // Shell脚本内容，使用全局参数sleep
        String rawScript = "#!/bin/bash\nsleep ${sleep}";
        taskParams.put("localParams", properties);
        taskParams.put("rawScript", rawScript);
        taskParams.put("resourceList", new ArrayList<>());
        sleepTask.setTaskParams(JSONUtils.toJsonString(taskParams));
        // 设置任务其他属性
        sleepTask.setTaskParamList(new ArrayList<>());
        sleepTask.setFlag(Flag.YES);
        sleepTask.setIsCache(Flag.NO);
        sleepTask.setTaskPriority(Priority.MEDIUM);
        sleepTask.setWorkerGroup("default");
        sleepTask.setEnvironmentCode(-1);
        sleepTask.setFailRetryTimes(0);
        sleepTask.setFailRetryInterval(1);
        sleepTask.setTimeoutFlag(TimeoutFlag.CLOSE);
        sleepTask.setTimeout(0);
        sleepTask.setDelayTime(0);
        sleepTask.setResourceIds(null);
        sleepTask.setTaskGroupId(0);
        sleepTask.setTaskGroupPriority(0);
        sleepTask.setCpuQuota(-1);
        sleepTask.setMemoryMax(-1);
        sleepTask.setTaskExecuteType(TaskExecuteType.BATCH);
        sleepTask.setOperator(loginUser.getId());
        sleepTask.setOperateTime(new Date());
        return sleepTask;
    }


    private void processApiTaskDefinition(TaskDefinitionLog taskDefinition, ApiExcelDto apiConfig, DataSource sourceDataSource,
                                          DataSource targetDataSource, String deployMode, String tokenBothJson,
                                          String acceptHeader, String protocol, String slowMode, String tokenMode,
                                          boolean isSleep, boolean isDepend, String replaceRules, boolean enableJsonField,
                                          long sourceTaskCode, long sinkTaskCode) {

        // 设置基本任务属性
        taskDefinition.setName(apiConfig.getTableName());
        taskDefinition.setVersion(1);
        taskDefinition.setDescription(String.format("同步%s接口数据到表%s中", apiConfig.getApiName(), apiConfig.getTableName()));
        taskDefinition.setFlag(Flag.YES);
        taskDefinition.setIsCache(Flag.NO);
        taskDefinition.setTaskPriority(Priority.MEDIUM);
        taskDefinition.setWorkerGroup(WorkerGroupUtils.getDefaultWorkerGroup());
        taskDefinition.setEnvironmentCode(-1);
        taskDefinition.setFailRetryInterval(1);
        taskDefinition.setFailRetryTimes(0);
        taskDefinition.setTimeout(0);
        taskDefinition.setDelayTime(0);
        taskDefinition.setTaskGroupId(0);
        taskDefinition.setTaskGroupPriority(0);
        taskDefinition.setCpuQuota(-1);
        taskDefinition.setMemoryMax(-1);
        taskDefinition.setTaskExecuteType(TaskExecuteType.BATCH);
        taskDefinition.setTimeoutFlag(TimeoutFlag.CLOSE);
        taskDefinition.setTimeoutNotifyStrategy(TaskTimeoutStrategy.WARN);
        taskDefinition.setWorkerGroup("default");
        // 设置任务参数
        taskDefinition.setTaskParams(getSeatunnelHttpTaskParams(apiConfig, sourceDataSource, targetDataSource, deployMode,
                tokenBothJson, acceptHeader, protocol, slowMode, tokenMode, isDepend, replaceRules, enableJsonField,
                sourceTaskCode, sinkTaskCode));
    }

    /**
     * 生成HTTP API Seatunnel任务参数
     */
    private String getSeatunnelHttpTaskParams(ApiExcelDto apiConfig, DataSource sourceDataSource, DataSource targetDataSource,
                                              String deployMode, String tokenBothJson,
                                              String acceptHeader, String protocol, String slowMode, String tokenMode, boolean idDepend,
                                              String replaceRules, boolean enableJsonField,
                                              long sourceTaskCode, long sinkTaskCode) {
        Map<String, Object> taskParams = new HashMap<>();
        List<Property> properties = new ArrayList<>();
        properties.add(new Property("parallelism", Direct.IN, DataType.VARCHAR, "2"));
        properties.add(new Property("job.mode", Direct.IN, DataType.VARCHAR, "BATCH"));
        properties.add(new Property("checkpoint.interval", Direct.OUT, DataType.VARCHAR, "10000"));
        taskParams.put("localParams", properties);
        taskParams.put("resourceList", new ArrayList<>());
        taskParams.put("startupScript", "seatunnel.sh");
        taskParams.put("useCustom", true);
        taskParams.put("scriptType", "visualize");
        taskParams.put("deployMode", deployMode);

        List<Map<String, Object>> rawScript = new ArrayList<>();
        long postTaskCode = CollectionUtils.isNotEmpty(apiConfig.getTransforms()) ?
                Long.valueOf(apiConfig.getTransforms().get(0).getCode()) : sinkTaskCode;
        Map<String, Object> httpSourceNode = createHttpSourceNode(apiConfig, sourceDataSource, sourceTaskCode, postTaskCode,
                tokenBothJson, acceptHeader, protocol, slowMode, tokenMode, idDepend, replaceRules, enableJsonField);
        rawScript.add(httpSourceNode);
        long preTaskCode = sourceTaskCode;
        List<ProcessTaskRelation> relations = new ArrayList<>();
        relations.add(new ProcessTaskRelation(0L, sourceTaskCode));
        for (int i = 0; i < apiConfig.getTransforms().size(); i++) {
            relations.add(new ProcessTaskRelation(preTaskCode, postTaskCode));
            SeatunnelTaskNodeVO transform = apiConfig.getTransforms().get(i);
            transform.setPreTaskcode(String.valueOf(preTaskCode));
            if (i + 1 < apiConfig.getTransforms().size()) {
                postTaskCode = Long.valueOf(apiConfig.getTransforms().get(i + 1).getCode());
            } else {
                postTaskCode = sinkTaskCode;
            }
            transform.setPostTaskcode(String.valueOf(postTaskCode));
            rawScript.add(JSONUtils.toMap(JSONUtils.toJsonString(transform), String.class, Object.class));
            preTaskCode = Long.valueOf(transform.getCode());
        }
        // DORIS SINK节点
        Map<String, Object> dorisSinkNode = createDorisSinkNode(apiConfig, targetDataSource, preTaskCode, sinkTaskCode);
        rawScript.add(dorisSinkNode);
        relations.add(new ProcessTaskRelation(preTaskCode, sinkTaskCode));
        taskParams.put("rawScript", JSONUtils.toJsonString(rawScript));
        taskParams.put("relation", JSONUtils.toJsonString(relations));
        return JSONUtils.toJsonString(taskParams);
    }

    /**
     * 创建HTTP SOURCE节点 (严格遵循 Form-data 规则)
     */
    private Map<String, Object> createHttpSourceNode(ApiExcelDto apiConfig, DataSource sourceDataSource, long sourceTaskCode,
                                                     long sinkTaskCode, String tokenJson, String acceptHeader, String protocol,
                                                     String slowMode, String tokenMode, boolean idDepend, String replaceRules,
                                                     boolean enableJsonField) {
        Map<String, Object> httpNode = new HashMap<>();
        if (idDepend && StringUtils.isNotEmpty(apiConfig.getDepend())) {
            httpNode.put("type", "TRANSFORM");
        } else {
            httpNode.put("type", "SOURCE");
        }
        httpNode.put("pluginName", "HTTP");
        httpNode.put("code", String.valueOf(sourceTaskCode));
        String name = apiConfig.getSeq() + "_" + apiConfig.getFunction() + "_" + apiConfig.getApiName();
        httpNode.put("name", name);
        httpNode.put("postTaskcode", String.valueOf(sinkTaskCode));

        // 构建HTTP参数
        Map<String, Object> params = new HashMap<>();
//        params.put("outputFields", null);

        TokenBothConfig bothConfig = null;
        Map<String, String> extraHeaders = null;
        Map<String, String> testHeaders = null;
        try {
            bothConfig = JSONUtils.parseObject(tokenJson, TokenBothConfig.class);

            Map<String, Object> map = JSONUtils.parseObject(tokenJson, Map.class);
            if (map.containsKey("extraHeaders")) {
                Object extraHeadersObj = map.get("extraHeaders");
                if (extraHeadersObj instanceof Map) {
                    extraHeaders = (Map<String, String>) extraHeadersObj;
                }
            }

            if (map.containsKey("testHeaders")) {
                Object testObj = map.get("testHeaders");
                if (testObj instanceof Map) {
                    testHeaders = (Map<String, String>) testObj;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("parse token json failed.");
        }

        String url = apiConfig.getUrl();
        String urlWithoutQuery = url;
        List<Map<String, Object>> urlParams = new ArrayList<>();
        if (StringUtils.isNotBlank(url) && url.contains("?")) {
            String[] parts = url.split("\\?", 2);
            urlWithoutQuery = parts[0];
            if (parts.length > 1 && StringUtils.isNotBlank(parts[1])) {
                urlParams = parseKeyValueParams(parts[1]);
            }
        }
        params.put("url", urlWithoutQuery);

        // 请求方法
        String method = StringUtils.defaultIfBlank(apiConfig.getMethod(), "GET").toUpperCase(Locale.ROOT);
        params.put("method", method);
        params.put("format", "json");

        List<Map<String, Object>> allKeyValueParams = new ArrayList<>(urlParams);
        String bodyContent = "";
        List<Map<String, Object>> httpExtra = new ArrayList<>();
        String paramsStr = StringUtils.defaultIfBlank(apiConfig.getParams(), "");
        boolean isJsonBody = false;

        // 预留原始参数
        List<Map<String, Object>> oraginParams = new ArrayList<>();
        String oraginBody = "{}";

        if (StringUtils.isNotBlank(paramsStr)) {
            String trimmedParams = paramsStr.trim();
            if (trimmedParams.startsWith("{") && trimmedParams.endsWith("}")) {
                // 识别为 JSON Body
                isJsonBody = true;
                bodyContent = trimmedParams;
                oraginBody = trimmedParams;
            } else {
                List<Map<String, Object>> parsedParams = parseKeyValueParams(trimmedParams);
                // 将解析的参数添加到allKeyValueParams
                allKeyValueParams.addAll(parsedParams);
                // 创建去重后的原始参数列表
                Map<String, Map<String, Object>> uniqueParams = new LinkedHashMap<>();
                // 先处理URL参数（优先级较高）
                for (Map<String, Object> urlParam : urlParams) {
                    String prop = (String) urlParam.get("prop");
                    Map<String, Object> newParam = new HashMap<>();
                    newParam.put("prop", prop);
                    newParam.put("value", urlParam.get("value"));
                    uniqueParams.put(prop, newParam);
                }
                // 再处理paramsStr中的参数（后面的覆盖前面的）
                for (Map<String, Object> param : parsedParams) {
                    String prop = (String) param.get("prop");
                    Map<String, Object> newParam = new HashMap<>();
                    newParam.put("prop", prop);
                    newParam.put("value", param.get("value"));
                    uniqueParams.put(prop, newParam); // 这里会覆盖URL中相同的参数
                }
                List<Map<String, Object>> paramList = new ArrayList<>(uniqueParams.values());
                oraginParams = deepCopyParams(paramList);
            }
        }

        // 创建用于工作流的参数副本（将包含占位符）
        List<Map<String, Object>> workflowParams = deepCopyParams(oraginParams);
        // 创建用于API测试的参数副本（将包含实际值）
        List<Map<String, Object>> testParams = deepCopyParams(oraginParams);

        // 处理认证配置
        List<Map<String, Object>> httpHeaders = new ArrayList<>();
        if ("both".equalsIgnoreCase(tokenMode)) {
            // --- 1. 处理 Query 参数 ---
            String queryKey = bothConfig.getQuery();
            String queryValue = bothConfig.getQueryToken();

            if (StringUtils.isNotBlank(queryKey)) {
                // 工作流参数：用占位符
                boolean foundInWorkflow = false;
                for (Map<String, Object> param : workflowParams) {
                    if (queryKey.equals(param.get("prop"))) {
                        param.put("value", "${api_token_query}");
                        foundInWorkflow = true;
                        break;
                    }
                }
                if (!foundInWorkflow) {
                    Map<String, Object> tokenParam = new HashMap<>();
                    tokenParam.put("prop", queryKey);
                    tokenParam.put("value", "${api_token_query}");
                    workflowParams.add(tokenParam);
                }

                // 测试参数：用实际值
                boolean foundInTest = false;
                for (Map<String, Object> param : testParams) {
                    if (queryKey.equals(param.get("prop"))) {
                        param.put("value", queryValue);
                        foundInTest = true;
                        break;
                    }
                }
                if (!foundInTest) {
                    Map<String, Object> tokenTestParam = new HashMap<>();
                    tokenTestParam.put("prop", queryKey);
                    tokenTestParam.put("value", queryValue);
                    testParams.add(tokenTestParam);
                }
            }

            // --- 2. 处理 Header ---
            String headerName = bothConfig.getHeader();
            String headerValue = bothConfig.getHeaderToken();
            if (StringUtils.isNotBlank(headerName) && StringUtils.isNotBlank(headerValue)) {
                Map<String, Object> headerEntry = new HashMap<>();
                headerEntry.put("prop", headerName);
                // 工作流中header使用占位符
                headerEntry.put("value", "${api_token}");
                httpHeaders.add(headerEntry);
            }
        } else if ("query".equalsIgnoreCase(tokenMode)) {
            // 仅query模式
            String queryKey = bothConfig.getQuery();
            String queryValue = bothConfig.getQueryToken();

            if (StringUtils.isNotBlank(queryKey)) {
                // 工作流参数：用占位符
                boolean foundInWorkflow = false;
                for (Map<String, Object> param : workflowParams) {
                    if (queryKey.equals(param.get("prop"))) {
                        param.put("value", "${api_token_query}");
                        foundInWorkflow = true;
                        break;
                    }
                }
                if (!foundInWorkflow) {
                    Map<String, Object> tokenParam = new HashMap<>();
                    tokenParam.put("prop", queryKey);
                    tokenParam.put("value", "${api_token_query}");
                    workflowParams.add(tokenParam);
                }

                // 测试参数：用实际值
                boolean foundInTest = false;
                for (Map<String, Object> param : testParams) {
                    if (queryKey.equals(param.get("prop"))) {
                        param.put("value", queryValue);
                        foundInTest = true;
                        break;
                    }
                }
                if (!foundInTest) {
                    Map<String, Object> tokenTestParam = new HashMap<>();
                    tokenTestParam.put("prop", queryKey);
                    tokenTestParam.put("value", queryValue);
                    testParams.add(tokenTestParam);
                }
            }

        } else if ("header".equalsIgnoreCase(tokenMode)) {
            // 仅header模式
            String headerName = bothConfig.getHeader();
            String headerValue = bothConfig.getHeaderToken();

            if (StringUtils.isNotBlank(headerName)) {
                Map<String, Object> tokenHeader = new HashMap<>();
                tokenHeader.put("prop", headerName);
                tokenHeader.put("value", "${api_token}");
                httpHeaders.add(tokenHeader);
            }
        }

        if (extraHeaders != null) {
            for (Map.Entry<String, String> entry : extraHeaders.entrySet()) {
                Map<String, Object> customHeader = new HashMap<>();
                customHeader.put("prop", entry.getKey());
                String value = entry.getValue();
                if (value != null && value.startsWith("${") && value.endsWith("}")) {
                    customHeader.put("value", value);
                } else {
                    customHeader.put("value", value);
                }
                httpHeaders.add(customHeader);
            }
        }
        params.put("httpHeaders", httpHeaders);


        if (isJsonBody) {
            params.put("body", bodyContent);
            params.put("httpParams", urlParams);
        } else {
            params.put("body", "");
            // 使用包含占位符的workflowParams
            params.put("httpParams", workflowParams);
            if ("POST".equals(method)) {
                boolean hasAnyParams = CollectionUtils.isNotEmpty(workflowParams);
                if (hasAnyParams) {
                    httpExtra.add(createHttpExtra("keep_params_as_form", "true"));
                }
            }
        }
        params.put("httpExtra", httpExtra);


        // 分页参数处理
        String paginationStr = StringUtils.defaultIfBlank(apiConfig.getPaginationParams(), "");
        boolean hasPagination = StringUtils.isNotBlank(paginationStr);
        Map<String, Object> pageingVo = new HashMap<>();


        if (hasPagination) {
            params.put("isPage", true);
            Map<String, Object> paginationConfigMap = new HashMap<>();
            try {
                paginationConfigMap = JSONUtils.parseObject(paginationStr, Map.class);
            } catch (Exception e) {
                log.error("解析分页配置 JSON 失败: {}", paginationStr, e);
            }

            // 确保page_field有值
            if (!paginationConfigMap.containsKey("page_field") ||
                    StringUtils.isBlank((String) paginationConfigMap.get("page_field"))) {
                paginationConfigMap.put("page_field", "page");
            }
            params.put("pageingVo", JSONUtils.toJsonString(paginationConfigMap));
            pageingVo = paginationConfigMap;
        } else {
            params.put("isPage", false);
        }

        // 替换参数中的分页参数值为${page_fieldxxx}
        if (hasPagination) {
            String pageFieldFinal = (String) pageingVo.get("page_field");
            String placeholder = String.format("${%s}", pageFieldFinal);

            List<Map<String, Object>> httpParamsList = (List<Map<String, Object>>) params.get("httpParams");

            if (CollectionUtils.isNotEmpty(httpParamsList)) {
                for (Map<String, Object> param : httpParamsList) {
                    String prop = (String) param.get("prop");
                    if (pageFieldFinal.equals(prop)) {
                        // 将该 Key 对应的 Value 替换为占位符
                        param.put("value", placeholder);
                    }
                }
            }

            // 2. 替换 bodyContent 中的分页参数值 (仅在 JSON Body 模式下)
            if (isJsonBody) {
                try {
                    // 将 JSON 字符串解析为可修改的节点
                    ObjectNode rootNode = (ObjectNode) JSONUtils.parseObject(bodyContent);
                    // 遍历并替换与 pageFieldFinal 匹配的顶层键
                    Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> field = fields.next();
                        String fieldName = field.getKey();
                        if (pageFieldFinal.equals(fieldName)) {
                            rootNode.put(fieldName, placeholder);
                            bodyContent = rootNode.toString();
                            params.put("body", bodyContent);
                            log.info("JSON Body 分页字段 '{}' 已自动替换为占位符: {}", fieldName, placeholder);
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.error("自动替换 JSON Body 分页参数失败, 请用户确保手动设置了占位符。Body: {}", bodyContent, e);
                }
            }
        }

        // 固定参数
        params.put("datasourceId", sourceDataSource != null ? sourceDataSource.getId() : null);
        params.put("isIterative", false);
        params.put("localParams", null);

        // 内容获取设置
        String json_contentField = apiConfig.getJson_contentField();
        if (enableJsonField) {
            params.put("field_path_type", "json");
            params.put("jsonFieldVo", json_contentField);
        } else {
            params.put("field_path_type", "content");
            params.put("content_field", json_contentField);
            if (StringUtils.isEmpty(json_contentField)) {
                params.put("content_field", "*");
            }
        }

        HttpServerConnectionParam httpParam = (HttpServerConnectionParam) DataSourceUtils.buildConnectionParams(sourceDataSource.getType(), sourceDataSource.getConnectionParams());
        String baseUrl = protocol + httpParam.getHost() + ":" + httpParam.getPort() + urlWithoutQuery;
//        String baseUrl = urlWithoutQuery;

        boolean apiCallSuccess = false; // 新增：标记API调用是否成功

        // 调用API获取数据生成Schema
        try {
            Request.Builder builder = new Request.Builder();
            // Accept头
            String accept = StringUtils.isNotBlank(acceptHeader) ? acceptHeader : "application/json, text/plain, */*";
            builder.addHeader("Accept", accept);

            if (bothConfig != null) {
                String realHeaderValue = null;
                String headerName = null;
                if ("header".equalsIgnoreCase(tokenMode) || "both".equalsIgnoreCase(tokenMode)) {
                    realHeaderValue = bothConfig.getHeaderToken();
                    headerName = bothConfig.getHeader();
                }
                if (StringUtils.isNotBlank(realHeaderValue) && StringUtils.isNotBlank(headerName)) {
                    builder.addHeader(headerName, realHeaderValue);
                }

                if (testHeaders != null && !testHeaders.isEmpty()) {
                    for (Map.Entry<String, String> entry : testHeaders.entrySet()) {
                        builder.addHeader(entry.getKey(), entry.getValue());
                    }
                }
            }

            if ("GET".equals(method)) {
                // GET请求
                HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl).newBuilder();
                if (CollectionUtils.isNotEmpty(testParams)) {
                    for (Map<String, Object> param : testParams) {
                        urlBuilder.addQueryParameter((String) param.get("prop"), String.valueOf(param.get("value")));
                    }
                }
                builder.url(urlBuilder.build()).get();
            } else {
                // POST请求
                builder.url(baseUrl);

                if (isJsonBody && StringUtils.isNotBlank(oraginBody)) {
                    builder.post(RequestBody.create(oraginBody, okhttp3.MediaType.parse("application/json")));
                    builder.addHeader("Content-Type", "application/json");
                } else {
                    FormBody.Builder formBuilder = new FormBody.Builder();
                    if (CollectionUtils.isNotEmpty(testParams)) {
                        for (Map<String, Object> param : testParams) {
                            formBuilder.add((String) param.get("prop"), String.valueOf(param.get("value")));
                        }
                    }
                    builder.post(formBuilder.build());
                }
            }

            Random r = new Random();
            long delayMs;
            if ("slow".equalsIgnoreCase(slowMode)) {
                // 10-15秒
                delayMs = 7_000L + (long) (r.nextDouble() * 6_001);
                Thread.sleep(delayMs);
            } else {
                delayMs = 1_000L + (long) (r.nextDouble() * 1_001);
                Thread.sleep(delayMs);
            }

            if (StringUtils.isNotEmpty(apiConfig.getSchema())) {
                params.put("schemaVo", apiConfig.getSchema());
            } else {
                try (Response response = client.newCall(builder.build()).execute()) {
                    apiCallSuccess = true;
                    String resultData = response.body().string();
                    String schema = generateSchema(json_contentField, resultData, enableJsonField);

                    if (StringUtils.isBlank(schema) || "{}".equals(schema.trim()) || "{\n\n}".equals(schema.trim())) {
                        httpNode.put("name", "(err02)" + name);
                        log.error("获取schema失败，rows: {}, apiName: {}", apiConfig.getSeq(), apiConfig.getApiName());
//                        schema = apiConfig.getSchema();
                        schema = "{}";
                    }
                    params.put("schemaVo", schema);
                }
            }
        } catch (Exception e) {
            log.error("调用API失败: row is {}, apiName is {}, error is {},", apiConfig.getSeq(), apiConfig.getApiName(), e.getMessage());
            if (!apiCallSuccess) {
                httpNode.put("name", "(err01)" + name);
            }
        }

        log.info("api {} is called", name);

        if (StringUtils.isNotEmpty(replaceRules)) {
            // 变量替换规则示例
//            replaceMap.put("2024/01/01 00:00:00", "${full_yesterday_start}");
//            replaceMap.put("2025/12/25 00:00:00", "${full_today_start}");

            // 参数替换自定义规则
            Map<String, String> replaceMap = new HashMap<>();

            try {
                ObjectMapper mapper = new ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, String> parsedMap = mapper.readValue(replaceRules, Map.class);
                replaceMap.putAll(parsedMap);
            } catch (Exception e) {
                String jsonStr = replaceRules.replaceAll("\\s+", "");
                if (jsonStr.startsWith("{") && jsonStr.endsWith("}")) {
                    String content = jsonStr.substring(1, jsonStr.length() - 1);
                    String[] pairs = content.split(",");
                    for (String pair : pairs) {
                        int colonIndex = pair.indexOf(':');
                        if (colonIndex > 0) {
                            String key = pair.substring(0, colonIndex);
                            String value = pair.substring(colonIndex + 1);
                            key = key.replace("\"", "").trim();
                            value = value.replace("\"", "").trim();
                            replaceMap.put(key, value);
                        }
                    }
                }
            }

            String body = (String) params.get("body");
            if (StringUtils.isNotBlank(body)) {
                for (Map.Entry<String, String> entry : replaceMap.entrySet()) {
                    body = body.replace(entry.getKey(), entry.getValue());
                }
                params.put("body", body);
            }
            List<Map<String, Object>> httpParams = (List<Map<String, Object>>) params.get("httpParams");
            if (CollectionUtils.isNotEmpty(httpParams)) {
                for (Map<String, Object> param : httpParams) {
                    String value = (String) param.get("value");
                    if (StringUtils.isNotBlank(value)) {
                        if (replaceMap.containsKey(value)) {
                            param.put("value", replaceMap.get(value));
                        }
                    }
                }
            }
        } else {
            // todo 替换掉各种类型的时间参数
        }

        httpNode.put("params", params);
//        httpNode.put("outputFields", new ArrayList<>());
        return httpNode;
    }

    private List<Map<String, Object>> deepCopyParams(List<Map<String, Object>> params) {
        List<Map<String, Object>> copy = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(params)) {
            Map<String, Map<String, Object>> uniqueParams = new LinkedHashMap<>();

            for (Map<String, Object> param : params) {
                String prop = (String) param.get("prop");

                // 创建新参数
                Map<String, Object> newParam = new HashMap<>();
                newParam.put("prop", prop);
                newParam.put("value", param.get("value"));

                // 检查是否已存在，如果存在则跳过（或覆盖）
                if (uniqueParams.containsKey(prop)) {
                    log.debug("发现重复参数: {}，跳过", prop);
                    continue; // 跳过重复参数
                }

                uniqueParams.put(prop, newParam);
                copy.add(newParam);
            }

//            // 记录去重信息
//            if (uniqueParams.size() < params.size()) {
//                log.info("去除了 {} 个重复参数", params.size() - uniqueParams.size());
//            }
        }
        return copy;
    }

    @Override
    public String generateSchema(String json_contentField, String apiResultExample, boolean enableJsonField) {
        if (StringUtils.isBlank(apiResultExample)) {
            return null;
        }

        // 如果是 jsonField 模式
        if (enableJsonField) {
            return generateSchemaFromJsonField(json_contentField, apiResultExample);
        }

        // 原有的 contentField 模式处理
        return generateSchemaFromContentField(json_contentField, apiResultExample);
    }

    /**
     * 从 jsonField 配置生成 schema
     *
     * @param jsonFieldConfig  jsonField配置，格式如：{"field1":"$.path1","field2":"$.path2"}
     * @param apiResultExample API返回的JSON示例
     * @return schema字符串
     */
    private String generateSchemaFromJsonField(String jsonFieldConfig, String apiResultExample) {
        if (StringUtils.isBlank(jsonFieldConfig) || StringUtils.isBlank(apiResultExample)) {
            return null;
        }
        try {
            // 1. 解析 jsonField 配置
            Map<String, String> fieldMapping = JSONUtils.parseObject(jsonFieldConfig,
                    new TypeReference<Map<String, String>>() {
                    });

            if (fieldMapping == null || fieldMapping.isEmpty()) {
                return null;
            }

            // 2. 解析API返回的JSON
            JsonNode rootNode = parseContentFieldJsonNode(apiResultExample);
            if (rootNode == null) {
                return null;
            }

            // 3. 根据映射关系推断字段类型
            StringBuilder schemaBuilder = new StringBuilder("{\n");

            // 3. 根据映射关系推断字段类型，只保留能拿到数据的字段
            List<String> validFieldEntries = new ArrayList<>();

            for (Map.Entry<String, String> entry : fieldMapping.entrySet()) {
                String fieldName = entry.getKey();
                String jsonPath = entry.getValue();

                // 4. 根据 jsonPath 提取对应的值并推断类型
                String fieldType = inferFieldTypeFromPath(jsonPath, rootNode);
//                fieldEntries.add(String.format("  \"%s\": \"%s\"", fieldName, fieldType));
                // 5. 如果字段类型不为空且不是"null"（表示能拿到数据），则保留
                if (StringUtils.isNotBlank(fieldType) && !"null".equalsIgnoreCase(fieldType)) {
                    validFieldEntries.add(String.format("  \"%s\": \"%s\"", fieldName, fieldType));
                } else {
                    log.debug("字段 {} 的jsonpath {} 无法获取数据，将从schema中移除", fieldName, jsonPath);
                }
            }

            schemaBuilder.append(String.join(",\n", validFieldEntries));
            schemaBuilder.append("\n}");

            return schemaBuilder.toString();

        } catch (Exception e) {
            log.error("从jsonField生成schema失败，jsonFieldConfig: {}, error: {}",
                    jsonFieldConfig, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 根据jsonPath从JSON中提取值并推断类型
     */
    private String inferFieldTypeFromPath(String jsonPath, JsonNode rootNode) {
        try {
            log.debug("解析jsonPath: {}", jsonPath);
            // 处理 $['ReturnData']['BizObjectArray'][*]['field'] 格式
            if (jsonPath.startsWith("$['")) {
                // 移除开头的 $[' 和结尾的 ']
                String path = jsonPath.substring(3, jsonPath.length() - 2);
                // 分割路径
                String[] parts = path.split("']\\['");
                JsonNode currentNode = rootNode;
                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i];
                    // 处理数组通配符 [*]
                    if (part.endsWith("[*]")) {
                        String fieldName = part.substring(0, part.length() - 3);
                        if (StringUtils.isNotEmpty(fieldName)) {
                            currentNode = currentNode.path(fieldName);
                        }
                        // 如果是数组，取第一个元素继续后续路径
                        if (currentNode != null && currentNode.isArray() && currentNode.size() > 0) {
                            currentNode = currentNode.get(0);
                        } else {
                            return null;
                        }
                    } else {
                        currentNode = currentNode.path(part);
                    }
                    if (currentNode == null || currentNode.isMissingNode() || currentNode.isNull()) {
                        return null;
                    }
                }
                return inferJsonType(currentNode);
            }

            // 处理 $.ReturnData.BizObjectArray[*].field 格式
            if (jsonPath.startsWith("$.")) {
                // 移除开头的 $.
                String path = jsonPath.substring(2);
                // 分割路径
                String[] parts = path.split("\\.");

                JsonNode currentNode = rootNode;
                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i];

                    // 处理数组通配符 [*]
                    if (part.endsWith("[*]")) {
                        String fieldName = part.substring(0, part.length() - 3);
                        if (StringUtils.isNotEmpty(fieldName)) {
                            currentNode = currentNode.path(fieldName);
                        }

                        // 如果是数组，取第一个元素继续后续路径
                        if (currentNode != null && currentNode.isArray() && currentNode.size() > 0) {
                            currentNode = currentNode.get(0);
                        } else {
                            return null;
                        }
                    } else {
                        currentNode = currentNode.path(part);
                    }

                    if (currentNode == null || currentNode.isMissingNode() || currentNode.isNull()) {
                        return null;
                    }
                }

                return inferJsonType(currentNode);
            }

            log.warn("不支持的jsonPath格式: {}", jsonPath);
            return null;

        } catch (Exception e) {
            log.error("解析jsonPath失败: {}", jsonPath, e);
            return null;
        }
    }

    public String generateSchemaFromContentField(String contentField, String apiResultExample) {
        if (StringUtils.isBlank(apiResultExample)) {
            return null;
        }
        JsonNode rootNode;
        try {
//            rootNode = JSONUtils.parseObject(apiResultExample);
            rootNode = parseContentFieldJsonNode(apiResultExample);
        } catch (Exception e) {
            log.error("解析 API Result 示例 JSON 失败，无法推断 Schema。示例: {}", apiResultExample, e);
            return null;
        }

        JsonNode dataNode = null;

        // 特殊处理：当 contentField 是 $ 或 $[*] 时，直接使用根节点
        if (StringUtils.isNotBlank(contentField)) {
            if ("$".equals(contentField) || "$[*]".equals(contentField)) {
                // 情况1：直接根数组或对象
                if (rootNode.isArray() && rootNode.size() > 0) {
                    dataNode = rootNode.get(0);
                } else if (rootNode.isObject()) {
                    dataNode = rootNode;
                }
            } else if (contentField.startsWith("$.")) {
                // 情况2：$.开头的路径，如 $.rows[*]
                String pathKey = contentField.replace("$.", "").replace("[*]", "").replace(".*", "").trim();
                if (StringUtils.isNotBlank(pathKey)) {
                    if (pathKey.contains(".")) {
                        String[] pathParts = pathKey.split("\\.");
                        JsonNode currentNode = rootNode;
                        for (int i = 0; i < pathParts.length; i++) {
                            String part = pathParts[i];
                            if (currentNode != null && currentNode.has(part)) {
                                currentNode = currentNode.get(part);
                                if (i == pathParts.length - 1) {
                                    dataNode = currentNode;
                                }
                            } else {
                                dataNode = null;
                                break;
                            }
                        }
                    } else {
                        dataNode = rootNode.get(pathKey);
                    }
                }
            }
        } else {
            // contentField为空的情况，尝试使用根节点
            if (rootNode.isArray() && rootNode.size() > 0) {
                dataNode = rootNode.get(0);
            } else if (rootNode.isObject()) {
                dataNode = rootNode;
            }
        }

        // 处理提取到的节点 - 关键修复点
        if (dataNode != null) {
            // 如果 dataNode 是数组，取第一个元素
            if (dataNode.isArray() && dataNode.size() > 0) {
                dataNode = dataNode.get(0);
            }
            // 如果 dataNode 是对象，但只有一个字段且该字段是数组
            else if (dataNode.isObject() && dataNode.size() == 1) {
                Iterator<String> fieldNames = dataNode.fieldNames();
                if (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    JsonNode childNode = dataNode.get(fieldName);
                    if (childNode.isArray() && childNode.size() > 0) {
                        dataNode = childNode.get(0);
                    }
                }
            }
        } else if (rootNode.isArray() && rootNode.size() > 0) {
            // 如果 dataNode 仍然为空，直接使用 rootNode 的第一个数组元素
            dataNode = rootNode.get(0);
        }

        // 3. 提取字段和推断类型 - 关键修复点
        JsonNode targetNode = dataNode;

        // 最终确保我们处理的是对象
        if (targetNode != null && targetNode.isArray() && targetNode.size() > 0) {
            targetNode = targetNode.get(0);
        }

        if (targetNode != null && targetNode.isObject()) {
            try {
                StringBuilder fields = new StringBuilder();
                ObjectNode objectNode = (ObjectNode) targetNode;  // 现在安全了
                Iterator<Map.Entry<String, JsonNode>> fieldIterator = objectNode.fields();
                while (fieldIterator.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fieldIterator.next();
                    String fieldName = entry.getKey();
                    JsonNode valueNode = entry.getValue();
                    String type = inferJsonType(valueNode);
                    fields.append(String.format("  \"%s\": \"%s\",\n", fieldName, type));
                }
                if (fields.length() > 0) {
                    fields.setLength(fields.length() - 2);
                    return String.format("{\n%s\n}", fields.toString());
                }
            } catch (ClassCastException e) {
                log.error("类型转换异常，targetNode实际类型: {}",
                        targetNode.getClass().getSimpleName(), e);
                return null;
            }
        }
        log.error("contentField: {}, dataNode类型: {}",
                contentField,
                dataNode != null ? dataNode.getClass().getSimpleName() : "null");
        return null;
    }

    private JsonNode parseContentFieldJsonNode(String jsonText) {
        if (StringUtils.isBlank(jsonText)) {
            return null;
        }
        try {
            // 方法1：创建新的ObjectMapper
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(jsonText);
        } catch (Exception e) {
            log.error("解析JSON失败", e);
            return null;
        }
    }

    /**
     * 根据 JSON 节点的值推断最合适的 SeaTunnel Schema 类型。
     */
    private String inferJsonType(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isBoolean()) {
            return "boolean";
        }
        if (node.isIntegralNumber()) {
            if (node.canConvertToInt()) {
                return "int";
            }
            return "bigint";
        }
        if (node.isFloatingPointNumber()) {
            return "double";
        }
        if (node.isTextual()) {
            String text = node.textValue();
            if (StringUtils.isBlank(text)) {
                return "string";
            }
            if (TIMESTAMP_PATTERN.matcher(text).matches()) {
//                return "timestamp";
                return "string";
            }
            if (DATE_PATTERN.matcher(text).matches()) {
//                return "date";
                return "string";
            }
            return "string";
        }
        if (node.isArray()) {
//            if (node.isEmpty()) {
//                // 空数组 → 默认元素类型为 string
//                return "array<string>";
//            }
//            // 查找第一个非 null 元素
//            for (JsonNode element : node) {
//                if (!element.isNull()) {
//                    String elementType = inferJsonType(element);
//                    return "array<" + elementType + ">";
//                }
//            }
//            // 所有元素都是 null → 仍返回 array<string>（而非 array<null>）
//            return "array<string>";
            return "string";
        }

        if (node.isObject()) {
//            if (node.isEmpty()) {
//                return "map<string, string>";
//            }
//            // 查找第一个非 null 的 value
//            for (JsonNode value : node) {
//                if (!value.isNull()) {
//                    String valueType = inferJsonType(value);
//                    return "map<string, " + valueType + ">";
//                }
//            }
//            // 所有值都是 null → 保守返回 map<string, string>
//            return "map<string, string>";
            return "string";
        }

        return "string";
    }


    /**
     * 解析 Key-Value 格式的参数字符串 (例如：key1=value1&key2=value2)
     *
     * @param paramStr 待解析的参数字符串
     * @return 包含参数属性和值的列表
     */
    /**
     * 解析Key-Value参数，自动去重
     */
    /**
     * 解析Key-Value参数，自动去重
     */
    private List<Map<String, Object>> parseKeyValueParams(String paramsStr) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (StringUtils.isBlank(paramsStr)) {
            return result;
        }

        Map<String, String> uniqueParams = new LinkedHashMap<>(); // 使用LinkedHashMap保持顺序

        // 按&分割参数
        String[] pairs = paramsStr.split("&");
        for (String pair : pairs) {
            if (StringUtils.isBlank(pair)) continue;

            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                String key = kv[0].trim();
                String value = kv[1].trim();

                // 如果已经有相同的key，覆盖（后面的参数覆盖前面的）
                if (uniqueParams.containsKey(key)) {
                    log.debug("发现重复参数 '{}'，使用最新值 '{}' 覆盖原值 '{}'", key, value, uniqueParams.get(key));
                }
                uniqueParams.put(key, value);
            } else if (kv.length == 1) {
                // 只有key没有value的情况
                String key = kv[0].trim();
                if (uniqueParams.containsKey(key)) {
                    log.debug("发现重复参数 '{}'", key);
                }
                uniqueParams.put(key, "");
            }
        }
        // 转换回List<Map>
        for (Map.Entry<String, String> entry : uniqueParams.entrySet()) {
            Map<String, Object> param = new HashMap<>();
            param.put("prop", entry.getKey());
            param.put("value", entry.getValue());
            result.add(param);
        }

        return result;
    }

    /**
     * 创建DORIS SINK节点
     */
    private Map<String, Object> createDorisSinkNode(ApiExcelDto apiConfig,
                                                    DataSource targetDataSource,
                                                    long sourceTaskCode,
                                                    long sinkTaskCode) {
        Map<String, Object> dorisNode = new HashMap<>();
        dorisNode.put("type", "SINK");
        dorisNode.put("pluginName", "DORIS");
        dorisNode.put("code", String.valueOf(sinkTaskCode));
        dorisNode.put("name", apiConfig.getTableName());
        dorisNode.put("preTaskcode", String.valueOf(sourceTaskCode));

        // 构建Doris参数
        Map<String, Object> params = new HashMap<>();
//        params.put("outputFields", Collections.singletonList(createOutputField("OTHER", "Http", "", null, new ArrayList<>())));
        params.put("datasourceId", targetDataSource != null ? targetDataSource.getId() : null);
        params.put("table", apiConfig.getTableName());
        params.put("schemaSaveMode", "CREATE_SCHEMA_WHEN_NOT_EXIST");
        params.put("dataSaveMode", "APPEND_DATA");
        params.put("format", "json");
        params.put("readJsonByLine", "true");
        params.put("columnSeparator", "");
        params.put("sinkEnableDelete", "false");
        params.put("needsUnsupportedTypeCasting", "false");
        params.put("sink", false);
        params.put("sinkEnable2pc", "false");
        params.put("customSql", "");
        params.put("sinkCheckInterval", 10000);
        params.put("sinkMaxRetries", 3);
        params.put("sinkBufferSize", 262144);
        params.put("sinkBufferCount", 3);
        params.put("dorisBatchSize", 1024);

        // 主键设置
        if (StringUtils.isNotBlank(apiConfig.getPrimaryKey())) {
            params.put("primary_keys", Collections.singletonList(apiConfig.getPrimaryKey()));
        } else {
            // 默认主键
            params.put("primary_keys", Collections.singletonList("id"));
        }

        params.put("batch_size", 1000);
        params.put("transaction_timeout_sec", -1);
        params.put("field_ide", "ORIGINAL");
        params.put("plugin_input", "");
        params.put("custom_sql", "");
        params.put("enable_upsert", true);
        params.put("use_copy_statement", false);
        params.put("schema_save_mode", "CREATE_SCHEMA_WHEN_NOT_EXIST");
        params.put("data_save_mode", "APPEND_DATA");
        params.put("feHttpPort", 8030);
        params.put("localParams", null);

        dorisNode.put("params", params);

        // 输出字段
//        List<Map<String, Object>> outputFields = new ArrayList<>();
//        outputFields.add(createOutputField("OTHER", "Http", "", null, new ArrayList<>()));
//        dorisNode.put("outputFields", outputFields);

        return dorisNode;
    }

    /**
     * 创建HTTP额外参数
     */
    private Map<String, Object> createHttpExtra(String prop, String value) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("prop", prop);
        extra.put("value", value);
        return extra;
    }

    /**
     * 创建输出字段
     */
    private Map<String, Object> createOutputField(String type, String taskName,
                                                  String tableName, Object datasourceId,
                                                  List<Object> fields) {
        Map<String, Object> outputField = new HashMap<>();
        outputField.put("type", type);
        outputField.put("taskName", taskName);
        outputField.put("tableName", tableName);
        outputField.put("datasourceId", datasourceId);
        outputField.put("fields", fields);
        return outputField;
    }


    @Override
//    @Transactional
    public Map<String, Object> updateProcessDefinitionChuanyunApis(User loginUser, long projectCode,
                                                                   Integer sourceDatasourceId, Integer targetDatasourceId,
                                                                   int serialTaskNum, String etlTechnology,
                                                                   String deployMode, String tokenMode,
                                                                   String tokenBothJson, String acceptHeader,
                                                                   String protocol, String slowMode,
                                                                   boolean isSleep, boolean isDepend) throws Exception {
        // 1. 校验数据源
        DataSource sourceDataSource = dataSourceService.getDataSourceById(sourceDatasourceId);
        if (sourceDataSource == null) {
            throw new ServiceException(Status.DATASOURCE_NOT_EXIST);
        }

        DataSource targetDataSource = dataSourceService.getDataSourceById(targetDatasourceId);
        if (targetDataSource == null) {
            throw new ServiceException(Status.DATASOURCE_NOT_EXIST);
        }
        String connectionParams = targetDataSource.getConnectionParams();
        JsonNode jsonNode = JSONUtils.parseObject(connectionParams);
        String database = jsonNode.get("database").asText();

        List<ChuanYunApp> dbApps = getDatabaseChuanyunApps(targetDatasourceId);
        Map<String, String> headers = generateChuanyunApiHeaders(tokenBothJson);
        String chuanyunApiUrl = "https://www.h3yun.com/OpenApi/Invoke";
        // 数据库中的应用列表
        // api拿到的应用列表
        List<ChuanYunApp> apiApps = getAPiChuanyunApps(chuanyunApiUrl, headers);
        // 处理重复的应用名
        apiApps = handleDuplicateAppNames(apiApps);

        // 数据库中拿到所有的表单列表 并且根据应用code分类
        List<ChuanYunForm> dbForms = getDatabaseChuanyunForms(targetDatasourceId);
        Map<String, List<ChuanYunForm>> dbFormsMap = dbForms.stream().collect(Collectors.groupingBy(ChuanYunForm::getAppCode));

        Map<String, String> replaceRule = new HashMap<>();
        replaceRule.put("2020/01/01 00:00:00", "${full_yesterday_start}");
        replaceRule.put("2030/01/01 00:00:00", "${full_today_start}");
        replaceRule.put("11", "500");

        List<Map<String, Object>> globalParams = new ArrayList<>();
        String[][] timeParams = {
                {"full_yesterday_start", "$[yyyy/MM/dd 00:00:00-3650]", "昨天开始时间"},
                {"full_today_start", "$[yyyy/MM/dd 00:00:00]", "今天开始时间"},
                {"engine_code", headers.get("EngineCode"), "企业编码"},
                {"engine_secret", headers.get("EngineSecret"), "企业密钥"},
                {"sleep", "6", "暂停时间"}
        };
        for (String[] param : timeParams) {
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("prop", param[0]);
            paramMap.put("direct", "OUT");
            paramMap.put("type", "VARCHAR");
            paramMap.put("value", param[1]);
            globalParams.add(paramMap);
        }

        int i = 0;
        for (ChuanYunApp apiApp : apiApps) {

            List<ApiExcelDto> apiConfigs = new ArrayList<>();
            List<ChuanYunForm> lastForms = new ArrayList<>();
            boolean isChange = false;

            // 判断当前应用是否存在数据库 不存在应新增
            ChuanYunApp dbApp = dbApps.stream()
                    .filter(a -> Objects.equals(a.getObjectId(), apiApp.getObjectId()))
                    .findFirst()
                    .orElse(null);
            if (dbApp == null || !Objects.equals(dbApp.getDisplayName(), apiApp.getDisplayName())) {
                isChange = true;
            }

            // 从api获得的该应用下的表单
            List<ChuanYunForm> apiForms;
            try {
                apiForms = getAPiChuanyunForms(chuanyunApiUrl, headers, apiApp.getCode());
            } catch (Exception e) {
                throw new RuntimeException("can not call chuanyun api", e);
            }

            lastForms.addAll(apiForms);

            // 获取数据库中的氚云表单
            List<ChuanYunForm> dbAppForms = dbFormsMap.getOrDefault(apiApp.getCode(), Collections.emptyList());
            Map<String, ChuanYunForm> dbFormMap = dbAppForms.stream()
                    .collect(Collectors.toMap(ChuanYunForm::getCode, f -> f, (a, b) -> a));
            List<ChuanYunSchema> schemas = null;
            try {
                schemas = getAPiChuanyunFormSchemas(chuanyunApiUrl, headers, apiApp.getCode());
            } catch (IOException e) {
                log.error("can not call chuanyun get schema api faild.", e);
                throw new RuntimeException(e);
            }

            Map<String, ChuanYunSchema> apiSchemaMap = schemas.stream()
                    .filter(s -> s.getAppCode().equals(apiApp.getCode()))
                    .collect(Collectors.toMap(ChuanYunSchema::getFormCode, schema -> schema));

            if (apiForms.size() != dbAppForms.size()) {
                isChange = true;
            }

            List<String> commentSqls = new ArrayList<>();
            int seq = 1;
            apiForms.sort(Comparator.comparingInt(ChuanYunForm::getSortKey));
            for (ChuanYunForm apiForm : apiForms) {
                if (apiForm == null) {
                    log.error("apiForm is null");
                    continue;
                }
                ChuanYunForm dbForm = dbFormMap.get(apiForm.getCode());
                // 判断字段是否改变
                ChuanYunSchema apiSchema = apiSchemaMap.get(apiForm.getCode());
                String dbSchema = dbForm != null ? dbForm.getSchema() : null;
                // TODO 判断schema是否改变
                isChange = false;
                // 从 aPiChuanyunFormSchemas 找到appCode 和 formCOde 相等的
//                if (dbForm == null || !isSchemaEqual(dbSchema, apiSchema)) {
//                }
                String appTableNamePrefix = PinyinUtil.getFirstLetter(apiApp.getDisplayName());
                ApiExcelDto apiConfig = generateChuanYunApiConfig(apiForm, apiSchema, seq++, appTableNamePrefix);
                apiConfig.addTransforms(buildJsonPathTransform());
                apiConfig.addTransforms(buildSqlTransform(apiSchema));
                apiConfigs.add(apiConfig);
                String alterSql = generateTableCommentSql(apiConfig.getTableName(), apiApp.getDisplayName(),
                        apiForm.getDisplayName(), apiSchema.getSchema(), database);
                commentSqls.add(alterSql);
                // TODO 添加sql注释语句及修改表schemaSQL
//                List<String> commentSqls = new ArrayList<>();
//                // 收集sql注释
//                if (apiSchema != null && !apiSchema.isEmpty()) {
//                    String commentSql = generateTableCommentSql(
//                            apiConfig.getTableName(),
//                            apiApp.getDisplayName(),
//                            apiForm.getDisplayName(),
//                            apiSchema,
//                            database
//                    );
//                    if (commentSql != null) {
//                        commentSqls.add(commentSql);
//                    }
//                }
//                if (!commentSqls.isEmpty()) {
//                    batchExecuteCommentSqls(targetDatasourceId, commentSqls);
//                }

            }
            batchExecuteCommentSqls(targetDatasourceId, commentSqls);
            if (commentSqls.isEmpty() || !commentSqls.isEmpty()) {
                continue;
            }
            String name = StringUtils.isEmpty(apiApp.getDisplayName()) ? apiApp.getCode() : apiApp.getDisplayName();
//            List<ApiExcelDto> apiConfigs = convertChuanyunFormsToApiConfigs(apiApp,lastForms);
            ProcessDefinition definition = processDefinitionMapper.verifyByDefineName(projectCode, name);

            // 修改工作流
            if (definition != null) {
                long workflowCode = definition.getCode();

                // TODO 更改为修改工作流
                // 清空 当前应用 表单 工作流的关联关系
//                deleteChuanYunFormWorkflowRelations(targetDatasourceId, workflowCode);
//                // 新建 当前应用 表单 工作流的关联关系
//                createChuanYunFormWorkflowRelations(
//                        targetDatasourceId,
//                        apiApp,
//                        lastForms,
//                        apiConfigs,
//                        workflowCode,
//                        name,
//                        projectCode,
//                        "氚云"
//                );

                // 更新
                Map<String, Object> param = buildTaskParam(loginUser, projectCode, serialTaskNum, etlTechnology,
                        deployMode, apiConfigs, tokenMode, tokenBothJson, acceptHeader, protocol, slowMode, isSleep, isDepend,
                        sourceDataSource, targetDataSource, JSONUtils.toJsonString(replaceRule), false);

                String taskDefinitionJson = JSONUtils.toJsonString(param.get("taskDefinitions"));
                String taskRelationJson = JSONUtils.toJsonString(param.get("taskRelationList"));
                String locations = JSONUtils.toJsonString(param.get("locationsList"));

                // 如果工作流在上线状态先下线
                if (ReleaseState.ONLINE.equals(definition.getReleaseState())) {
                    offlineWorkflowDefinition(loginUser, projectCode, workflowCode);
                }

                Map<String, Object> updateResult = updateProcessDefinition(loginUser, projectCode, name, workflowCode, definition.getDescription(), definition.getGlobalParams(),
                        locations, 0, taskRelationJson, taskDefinitionJson, ProcessExecutionTypeEnum.PARALLEL);
                if (updateResult != null && Status.SUCCESS.equals(updateResult.get(Constants.STATUS))) {
                    // 清空 当前应用 表单 工作流的关联关系
                    deleteChuanYunFormWorkflowRelations(targetDatasourceId, workflowCode);
                    // 新建 当前应用 表单 工作流的关联关系
                    createChuanYunFormWorkflowRelations(
                            targetDatasourceId,
                            apiApp,
                            lastForms,
                            apiConfigs,
                            workflowCode,
                            name,
                            projectCode,
                            "氚云"
                    );
                    log.debug("workflow {} is create over with code: {}.", name, workflowCode);
                } else {
                    log.error("Create workflow {} success but no process definition data returned", name);
                }
                log.debug("workflow {} is update over.", name);
            } else {

                Map<String, Object> createResult = createProcessDefinitionByDatasourceApis(loginUser, projectCode, name, sourceDatasourceId, targetDatasourceId,
                        serialTaskNum, etlTechnology, deployMode, apiConfigs, tokenMode, tokenBothJson, acceptHeader, protocol,
                        slowMode, isSleep, isDepend, JSONUtils.toJsonString(globalParams), JSONUtils.toJsonString(replaceRule), false);

                if (createResult != null && Status.SUCCESS.equals(createResult.get(Constants.STATUS))) {
                    // 从返回结果中获取 ProcessDefinition 对象
                    ProcessDefinition newDefinition = (ProcessDefinition) createResult.get(DATA_LIST);
                    if (newDefinition != null) {
                        long workflowCode = newDefinition.getCode();
                        // 清空 当前应用 表单 工作流的关联关系
                        deleteChuanYunFormWorkflowRelations(targetDatasourceId, workflowCode);
                        // 新建 当前应用 表单 工作流的关联关系
                        createChuanYunFormWorkflowRelations(
                                targetDatasourceId,
                                apiApp,
                                lastForms,
                                apiConfigs,
                                workflowCode,
                                name,
                                projectCode,
                                "氚云"
                        );
                        log.debug("workflow {} is create over with code: {}.", name, workflowCode);
                    } else {
                        log.error("Create workflow {} success but no process definition data returned", name);
                    }
                } else {
                    log.error("Failed to create workflow {}, result: {}", name, createResult);
                }
                log.debug("workflow {} is create over.", name);
            }

            log.debug("workflow {} is {} over.process is {}/{}.", name, definition == null ? "create" : isChange ? "update" : "ignore", i + 1, apiApps.size());
            i++;
        }
        return null;
    }

    @NotNull
    private static Map<String, String> generateChuanyunApiHeaders(String tokenBothJson) {
        Map<String, Object> map = JSONUtils.parseObject(tokenBothJson, Map.class);
        Map<String, String> headers = new HashMap<>();
        if (map.containsKey("testHeaders")) {
            Object testObj = map.get("testHeaders");
            if (testObj instanceof Map) {
                headers = (Map<String, String>) testObj;
            }
        }
        return headers;
    }

    private SeatunnelTaskNodeVO buildSqlTransform(ChuanYunSchema apiSchema) {
        SeatunnelTaskNodeVO sqlTransform = new SeatunnelTaskNodeVO();
        sqlTransform.setType("TRANSFORM");
        sqlTransform.setPluginName("SQL");
        sqlTransform.setName("SQL语句");
        sqlTransform.setCode(String.valueOf(CodeGenerateUtils.genCode()));
        Map<String, Object> params = new HashMap<>();
        sqlTransform.setParams(params);
        params.put("type", "TRANSFORM");
        params.put("flag", "YES");
        String columnStr = apiSchema.getSqlColumnString();
        params.put("query", String.format("SELECT %s FROM dule", columnStr));
        return sqlTransform;
    }

    private SeatunnelTaskNodeVO buildJsonPathTransform() {
        SeatunnelTaskNodeVO jsonPathTransform = new SeatunnelTaskNodeVO();
        jsonPathTransform.setType("TRANSFORM");
        jsonPathTransform.setPluginName("JSONPATH");
        jsonPathTransform.setName("Json解析器");
        jsonPathTransform.setCode(String.valueOf(CodeGenerateUtils.genCode()));
        TransFormJsonPathDTO transFormJsonPathDTO = new TransFormJsonPathDTO();
        transFormJsonPathDTO.setRowErrorHandleWay(RowErrorHandleWayEnum.SKIP);
        List<TransFormJsonPathColumnDTO> columnDTOS = new ArrayList<>();
        transFormJsonPathDTO.setColumns(columnDTOS);
        for (String[] field : ChuanYunSchema.BASE_FIELD) {
            columnDTOS.add(new TransFormJsonPathColumnDTO().setSrcField(field[0]).setDestField(field[0] + "Id")
                    .setDestType("STRING").setPath("$.ObjectId").setColumnErrorHandleWay(ColumnErrorHandleWayEnum.SKIP));
            columnDTOS.add(new TransFormJsonPathColumnDTO().setSrcField(field[0]).setDestField(field[0] + "Name")
                    .setDestType("STRING").setPath("$.Name").setColumnErrorHandleWay(ColumnErrorHandleWayEnum.SKIP));
        }
        jsonPathTransform.setParams(JSONUtils.toMap(JSONUtils.toJsonString(transFormJsonPathDTO), String.class, Object.class));
        return jsonPathTransform;
    }

    private ApiExcelDto generateChuanYunApiConfig(ChuanYunForm apiForm, ChuanYunSchema apiSchema, int seq, String appTableNamePrefix) {

        String displayName = apiForm.getDisplayName();
        if (StringUtils.isEmpty(displayName)) {
            displayName = apiForm.getCode();
        }
        String apiurl = "/OpenApi/Invoke";
        // 生成所需的数据结构
        ApiExcelDto apiConfig = new ApiExcelDto();
        apiConfig.setSeq(String.valueOf(seq));
        apiConfig.setFunction("表单");
        apiConfig.setApiName(displayName);
        apiConfig.setUrl(apiurl);
        apiConfig.setMethod("POST");
        // 构造jsonField

        apiConfig.setJson_contentField("$.ReturnData.BizObjectArray[*]");
        apiConfig.setSchema(JSONUtils.toJsonString(apiSchema.toSimpleSchema()));
        // 构造请求参数
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("ActionName", "LoadBizObjects");
        paramMap.put("SchemaCode", apiForm.getCode());
        // 拼接Filter参数
        Map<String, Object> matcher = new HashMap<>();
        matcher.put("Type", "And");
        List<Map<String, Object>> list = new ArrayList<>();
        // 查询条件
        // Operator运算符：0 =大于，1=大于等于，2=等于，3=小于等于，4=小于，5=不等于，6=在某个范围内，7=不在某个范围内。
        Map<String, Object> query1 = new HashMap<>();
        // 200 普通表单 210 流程表单
        query1.put("Type", "Item");
        if (apiForm.getNodeType() == 200) {
            query1.put("Name", "ModifiedTime");
        } else if (apiForm.getNodeType() == 210) {
            query1.put("Name", "CreatedTime");
        } else {
            query1.put("Name", "ModifiedTime");
        }
        query1.put("Operator", 1);
        query1.put("Value", "2020/01/01 00:00:00");
        list.add(query1);

        Map<String, Object> query2 = new HashMap<>();
        query2.put("Type", "Item");
        if (apiForm.getNodeType() == 200) {
            query2.put("Name", "ModifiedTime");
        } else if (apiForm.getNodeType() == 210) {
            query2.put("Name", "CreatedTime");
        } else {
            query2.put("Name", "ModifiedTime");
        }
        query2.put("Operator", 4);
        query2.put("Value", "2030/01/01 00:00:00");
        list.add(query2);

        matcher.put("Matchers", list);
        // 最终参数
        Map<String, Object> filterMap = new HashMap<>();
        filterMap.put("FromRowNum", 0);
        filterMap.put("RequireCount", false);
        filterMap.put("ReturnItems", new String[0]);
        filterMap.put("SortByCollection", new String[0]);
        filterMap.put("ToRowNum", 11);
        filterMap.put("Matcher", matcher);
        String filterJson = JSONUtils.toJsonString(filterMap);
        paramMap.put("Filter", filterJson);
        String paramMapJson = JSONUtils.toJsonString(paramMap);
        apiConfig.setParams(paramMapJson);
        // 设置分页参数
        Map<String, Object> page = new HashMap<>();
        page.put("page_type", "PageNumber");
        page.put("start_page_number", 1);
        page.put("page_field", "page");
        page.put("batch_size", "500");
        page.put("keep_page_param_as_http_body", true);
        page.put("from_index_field", "$.Filter.FromRowNum");
        page.put("to_index_field", "$.Filter.ToRowNum");
        apiConfig.setPaginationParams(JSONUtils.toJsonString(page));
        // 设置表名 大于64 前一部分全拼音 后面缩写
        String prefix = "ods_chuanyun_" + appTableNamePrefix + "_";
        List<String> segment = WordSegmentUtils.segment(displayName);
        int availableLength = 60 - prefix.length();
        String pinyinPart = PinyinUtil.getSmartTableNamePinyin(segment, availableLength);
        apiConfig.setTableName(prefix + pinyinPart);
        // 设置主键
        apiConfig.setPrimaryKey("ObjectId");
        Map<String, String> otherParams = new HashMap<>();
        otherParams.put("formCode", apiForm.getCode());
        otherParams.put("ObjectId", apiForm.getObjectId());
        apiConfig.setOtherParams(JSONUtils.toJsonString(otherParams));
        return apiConfig;
    }

    private String getChuanyunformSchema(String apiSchema) {
        Map<String, String> baseFieldMap = getChunyunFormBaseField();
        if (StringUtils.isEmpty(apiSchema)) {
            throw new RuntimeException("apiSchema is empty");
        }

        return null;
    }

    private static Map<String, String> getChunyunFormBaseField() {
        Map<String, String> baseFieldMap = new HashMap<>();
        baseFieldMap.put("ObjectId", "数据id");
        baseFieldMap.put("Name", "数据名");
        baseFieldMap.put("CreatedBy", "创建用户名");
        baseFieldMap.put("OwnerId", "拥有者用户名");
        baseFieldMap.put("OwnerDeptId", "拥有者部门名");
        baseFieldMap.put("CreatedTime", "创建时间");
        baseFieldMap.put("ModifiedBy", "修改用户名");
        baseFieldMap.put("ModifiedTime", "修改时间");
        baseFieldMap.put("WorkflowInstanceId", "工作流id");
        baseFieldMap.put("CreatedByObject", "创建人信息");
        baseFieldMap.put("ModifiedByObject", "修改人信息");
        baseFieldMap.put("OwnerIdObject", "拥有者信息");
        baseFieldMap.put("OwnerDeptIdObject", "拥有者部门信息");
        return baseFieldMap;
    }


    public static String sendChuanYunPost(String url, String jsonBody, Map<String, String> headers) throws IOException {
        RequestBody body = RequestBody.create(jsonBody, okhttp3.MediaType.get("application/json; charset=utf-8"));
        Request.Builder requestBuilder = new Request.Builder().url(url).post(body);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        Request request = requestBuilder.build();
        try (Response response = client.newCall(request).execute()) {

            byte[] responseBytes = null;
            if (response.body() != null) {
                responseBytes = response.body().bytes();
            }
            String responseBodyString = null;
            if (responseBytes != null) {
                try {
                    responseBodyString = new String(responseBytes, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    log.error("cannot parse chuanyun api result", e);
                }
            }

            if (!response.isSuccessful()) {
                String errorDetail = (responseBodyString != null && !responseBodyString.isEmpty()) ? responseBodyString : response.message();
                log.error("请求失败: HTTP {}, 详情: {}", response.code(), errorDetail);
            }
            return responseBodyString;
        }
    }

    private List<ChuanYunApp> getAPiChuanyunApps(String url, Map<String, String> headers) throws IOException {
        // 1. 构建请求
        Map<String, String> params = new HashMap<>();
        params.put("ActionName", "loadAppList");
        params.put("Controller", "AppList");
        params.put("AppCode", "D293502D293502");
        // 2. 发送请求
        String jsonParams = JSONUtils.toJsonString(params);
        String response = sendChuanYunPost(url, jsonParams, headers);
        // 3. 解析响应
        JsonObject result = JsonParser.parseString(response).getAsJsonObject();
        if (!result.get("Successful").getAsBoolean()) {
            throw new IOException("API失败: " + result.get("ErrorMessage"));
        }
        JsonArray data = result.getAsJsonObject("ReturnData").getAsJsonArray("data");
        // 4. 转换为对象列表
        List<ChuanYunApp> apps = new ArrayList<>();
        for (JsonElement element : data) {
            JsonObject obj = element.getAsJsonObject();
            ChuanYunApp app = new ChuanYunApp();
            app.setCode(getStringSafely(obj, "Code"));
            app.setDisplayName(getStringSafely(obj, "DisplayName"));
            app.setObjectId(getStringSafely(obj, "ObjectId"));
            app.setSortKey(Integer.parseInt(getStringSafely(obj, "SortKey")));
            apps.add(app);
        }
        return apps;
    }

    /**
     * 获取应用下的表单列表
     */
    private List<ChuanYunForm> getAPiChuanyunForms(String url, Map<String, String> headers, String appCode) throws IOException {
        // 1. 构建请求
        Map<String, String> params = new HashMap<>();
        params.put("ActionName", "loadFormList");
        params.put("Controller", "FormList");
        params.put("AppCode", "D293502D293502");
        params.put("currentCode", appCode);

        // 2. 发送请求
        String jsonParams = JSONUtils.toJsonString(params);
        String response = sendChuanYunPost(url, jsonParams, headers);

        // 3. 解析响应
        JsonObject result = JsonParser.parseString(response).getAsJsonObject();
        if (!result.get("Successful").getAsBoolean()) {
            throw new IOException("API失败: " + result.get("ErrorMessage"));
        }
        JsonArray data = result.getAsJsonObject("ReturnData").getAsJsonArray("data");
        List<ChuanYunForm> forms = new ArrayList<>();
        for (JsonElement element : data) {
            JsonObject obj = element.getAsJsonObject();
            ChuanYunForm form = new ChuanYunForm();
            form.setCode(getStringSafely(obj, "Code"));
            form.setSchema(getStringSafely(obj, "schema"));
            form.setAppCode(getStringSafely(obj, "AppCode"));
            form.setDisplayName(getStringSafely(obj, "DisplayName"));
            form.setObjectId(getStringSafely(obj, "ObjectId"));
            form.setNodeType(Integer.parseInt(getStringSafely(obj, "NodeType")));
            form.setSortKey(Integer.parseInt(getStringSafely(obj, "SortKey")));
            forms.add(form);
        }
        return forms;
    }

    /**
     * 获取应用下的表单列表
     */
    private List<ChuanYunSchema> getAPiChuanyunFormSchemas(String url, Map<String, String> headers, String currentCode) throws IOException {
        // 1. 构建请求
        Map<String, String> params = new HashMap<>();
        params.put("ActionName", "loadSchemaList");
        params.put("Controller", "SchemaList");
        params.put("AppCode", "D293502D293502");
        params.put("currentCode", currentCode);

        // 2. 发送请求
        String jsonParams = JSONUtils.toJsonString(params);
        String response = sendChuanYunPost(url, jsonParams, headers);

        // 3. 解析响应
        JsonObject result = JsonParser.parseString(response).getAsJsonObject();
        if (!result.get("Successful").getAsBoolean()) {
            throw new IOException("API失败: " + result.get("ErrorMessage"));
        }
        JsonArray data = result.getAsJsonObject("ReturnData").getAsJsonArray("data");
        List<ChuanYunSchema> schemas = new ArrayList<>();
        for (JsonElement element : data) {
            JsonObject obj = element.getAsJsonObject();
            ChuanYunSchema schema = new ChuanYunSchema();
            schema.setAppCode(getStringSafely(obj, "appCode"));
            schema.setFormCode(getStringSafely(obj, "formCode"));
            schema.setSchema(JSONUtils.toMap(getStringSafely(obj, "schema")));
            schema.setType(JSONUtils.toMap(getStringSafely(obj, "type")));
            schemas.add(schema);
        }
        return schemas;
    }

    private String getStringSafely(JsonObject jsonObject, String key) {
        return getStringSafely(jsonObject, key, "");
    }

    private String getStringSafely(JsonObject jsonObject, String key, String defaultValue) {
        if (jsonObject == null || !jsonObject.has(key)) {
            return defaultValue;
        }

        JsonElement element = jsonObject.get(key);
        if (element == null || element.isJsonNull()) {
            return defaultValue;
        }

        try {
            String value = element.getAsString();
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            return element.toString();
        }
    }

    /**
     * 获取数据库里的氚云应用
     */
    private List<ChuanYunApp> getDatabaseChuanyunApps(Integer targetDatasourceId) {
        try {
            String sql = "SELECT Code,DisplayName,State,ObjectId,SortKey FROM dhdw.ods_chuanyun_app";
            List<ChuanYunApp> apps = dataSourceService.executeQuery(targetDatasourceId, sql, (rs, rowNum) -> {
                ChuanYunApp app = new ChuanYunApp();
                app.setCode(rs.getString("Code"));
                app.setDisplayName(rs.getString("DisplayName"));
                app.setObjectId(rs.getString("ObjectId"));
                app.setSortKey(rs.getInt("SortKey"));
                app.setSortKey(rs.getInt("pinyin_initials"));
                return app;
            });
            log.info("从Doris获取到{}个氚云应用", apps.size());
            return apps;
        } catch (Exception e) {
            log.error("从Doris获取氚云应用失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取数据库里的氚云表单列表（从Doris - ods_chuanyun_form表）
     */
    private List<ChuanYunForm> getDatabaseChuanyunForms(Integer targetDatasourceId) {
        try {
            String sql = String.format(
                    "SELECT f.Code,f.AppCode,f.DisplayName,f.NodeType,f.ObjectId,f.SortKey,s.schema FROM dhdw.ods_chuanyun_form f " +
                            "LEFT JOIN dhdw.ods_chuanyun_schema s ON f.AppCode = s.appCode AND f.Code = s.formCode"
            );
            List<ChuanYunForm> forms = dataSourceService.executeQuery(targetDatasourceId, sql, (rs, rowNum) -> {
                ChuanYunForm form = new ChuanYunForm();
                form.setCode(rs.getString("Code"));
                form.setAppCode(rs.getString("AppCode"));
                form.setDisplayName(rs.getString("DisplayName"));
                form.setNodeType(rs.getInt("NodeType"));
                form.setObjectId(rs.getString("ObjectId"));
                form.setSortKey(rs.getInt("SortKey"));
                form.setSchema(rs.getString("schema"));
                return form;
            });
            return forms;
        } catch (Exception e) {
            log.error("从Doris获取氚云表单失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 删除指定工作流的关联关系
     */
    private void deleteChuanYunFormWorkflowRelations(Integer targetDatasourceId, Long workflowCode) {
        try {
            String sql = "DELETE FROM dhdw.ods_chuanyun_relation WHERE workflowCode  = " + workflowCode;
            int deletedCount = dataSourceService.executeUpdate(targetDatasourceId, sql);
            log.debug("已删除工作流 {} 的关联关系，删除{}条记录", workflowCode, deletedCount);
        } catch (Exception e) {
            log.error("删除关联关系失败", e);
        }
    }

    /**
     * 创建表单-工作流关联关系（批量执行优化版）
     */
    private void createChuanYunFormWorkflowRelations(Integer targetDatasourceId,
                                                     ChuanYunApp app,
                                                     List<ChuanYunForm> forms,
                                                     List<ApiExcelDto> apiConfigs,
                                                     Long workflowCode,
                                                     String workflowName,
                                                     Long projectCode,
                                                     String projectName) {

        if (forms.isEmpty()) {
            log.info("没有表单需要创建关联关系");
            return;
        }
        // 构建批量INSERT语句
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("INSERT INTO dhdw.ods_chuanyun_relation ")
                .append("(id, formCode, formName, appCode, appName, tableName, ")
                .append("workflowCode, workflowName, projectCode, projectName, CreateTime) ")
                .append("VALUES ");
        List<String> valueParts = new ArrayList<>();
        for (int i = 0; i < forms.size(); i++) {
            ChuanYunForm form = forms.get(i);
            ApiExcelDto apiConfig = apiConfigs.get(i);
            String valuePart = String.format(
                    "('%s', '%s', '%s', '%s', '%s', '%s', %d, '%s', %d, '%s', NOW())",
                    form.getObjectId(),
                    form.getCode(),
                    form.getDisplayName(),
                    app.getCode(),
                    app.getDisplayName(),
                    apiConfig.getTableName(),
                    workflowCode,
                    workflowName,
                    projectCode,
                    projectName
            );
            valueParts.add(valuePart);
        }
        sqlBuilder.append(String.join(", ", valueParts));

        try {
            int result = dataSourceService.executeUpdate(targetDatasourceId, sqlBuilder.toString());
            log.debug("批量创建关联关系成功，影响{}条记录", result);
        } catch (Exception e) {
            log.error("批量创建关联关系失败 - 工作流: {}", workflowCode, e);
        }
    }

    private boolean isSchemaEqual(String schema1, String schema2) {
        if (StringUtils.isEmpty(schema1) && StringUtils.isEmpty(schema2)) {
            return true;
        }
        if (StringUtils.isEmpty(schema1) || StringUtils.isEmpty(schema2)) {
            return false;
        }
        try {
            Map<String, Object> map1 = JSONUtils.parseObject(schema1, Map.class);
            Map<String, Object> map2 = JSONUtils.parseObject(schema2, Map.class);
            return Objects.equals(map1, map2);
        } catch (Exception e) {
            log.warn("解析schema JSON失败，使用字符串比较", e);
            return schema1.equals(schema2);
        }
    }

    /**
     * 生成表注释SQL语句
     */
    private String generateTableCommentSql(String tableName, String appName, String formName, Map<String, String> schemaMap, String database) {
        try {
            // 解析Schema
            if (schemaMap == null || schemaMap.isEmpty()) {
                return null;
            }
            StringBuilder sqlBuilder = new StringBuilder();
            String tablePath = database + "." + tableName;
            // 1. 表注释
            String tableComment = String.format("氚云应用[%s]的表单[%s]", appName, formName);
            sqlBuilder.append(String.format("ALTER TABLE %s MODIFY COMMENT '%s';\n",
                    tablePath, tableComment));

            // 2. 字段注释（只使用Schema中的字段）
            for (Map.Entry<String, String> entry : schemaMap.entrySet()) {
                String fieldName = entry.getKey();
                String comment = entry.getValue();
                // 跳过空值
                if (StringUtils.isEmpty(fieldName) || StringUtils.isEmpty(comment)) {
                    continue;
                }
                // 为字段生成注释
                sqlBuilder.append(String.format("ALTER TABLE %s MODIFY COLUMN %s COMMENT '%s';\n",
                        tablePath, fieldName, comment));
            }
            for (String[] field : ChuanYunSchema.BASE_FIELD_COMMENT) {
                sqlBuilder.append(String.format("ALTER TABLE %s MODIFY COLUMN %s COMMENT '%s';\n",
                        tablePath, field[0], field[1]));
            }
            return sqlBuilder.toString();
        } catch (Exception e) {
            log.error("生成表{}注释SQL失败: {}", tableName, e.getMessage());
            return null;
        }
    }

    /**
     * 批量执行注释SQL
     */
    private void batchExecuteCommentSqls(Integer targetDatasourceId, List<String> commentSqls) {
        if (commentSqls == null || commentSqls.isEmpty()) {
            return;
        }
        // 将所有SQL合并成一个字符串
        StringBuilder allSqls = new StringBuilder();
        int totalCount = 0;
        for (String sql : commentSqls) {
            if (StringUtils.isNotEmpty(sql)) {
                allSqls.append(sql);
                totalCount++;
            }
        }
        if (allSqls.length() == 0) {
            return;
        }
        try {
            log.info("开始批量执行{}个表的注释SQL...", totalCount);
            dataSourceService.executeUpdate(targetDatasourceId, allSqls.toString());
            log.info("成功执行{}个表的注释SQL", totalCount);
        } catch (Exception e) {
            log.error("批量执行注释SQL失败: {}", e.getMessage(), e);
        }
    }

    private List<ChuanYunApp> handleDuplicateAppNames(List<ChuanYunApp> apiApps) {
        Map<String, List<ChuanYunApp>> nameToAppsMap = new HashMap<>();
        for (ChuanYunApp app : apiApps) {
            String originalName = StringUtils.isEmpty(app.getDisplayName()) ? app.getCode() : app.getDisplayName();
            nameToAppsMap.computeIfAbsent(originalName, k -> new ArrayList<>()).add(app);
        }
        List<ChuanYunApp> processedApps = new ArrayList<>();
        for (Map.Entry<String, List<ChuanYunApp>> entry : nameToAppsMap.entrySet()) {
            String originalName = entry.getKey();
            List<ChuanYunApp> sameNameApps = entry.getValue();
            if (sameNameApps.size() == 1) {
                processedApps.add(sameNameApps.get(0));
            } else {
                // 按SortKey排序，保证顺序稳定
                sameNameApps.sort(Comparator.comparingInt(app -> {
                    Integer sortKey = app.getSortKey();
                    return sortKey != null ? sortKey : 0;
                }));
                // 为重复的应用添加数字后缀
                for (int i = 0; i < sameNameApps.size(); i++) {
                    ChuanYunApp app = sameNameApps.get(i);
                    ChuanYunApp processedApp = new ChuanYunApp();
                    processedApp.setCode(app.getCode());
                    processedApp.setObjectId(app.getObjectId());
                    processedApp.setSortKey(app.getSortKey());
                    String finalName;
                    if (i == 0) {
                        // 第一个保持原名
                        finalName = originalName;
                    } else {
                        // 后续的添加数字后缀
                        finalName = originalName + (i + 1);
                    }
                    processedApp.setDisplayName(finalName);
                    processedApps.add(processedApp);
                }
            }
        }
        return processedApps;
    }

    // todo 之前的

    @NotNull
    private static Map<String, Object> createTaskLocation(long taskCode, int x, int y, int width, int height) {
        Map<String, Object> taskLocation = new HashMap<>();
        taskLocation.put("taskCode", taskCode);
        taskLocation.put("x", x);
        taskLocation.put("y", y);
        taskLocation.put("width", width);
        taskLocation.put("height", height);
        return taskLocation;
    }

    @NotNull
    private static Map<String, Object> createTaskRelation(long preTaskCode, long taskCode, int preTaskVersion) {
        Map<String, Object> relation = new HashMap<>();
        relation.put("preTaskCode", preTaskCode);
        relation.put("postTaskCode", taskCode);
        relation.put("preTaskVersion", preTaskVersion);
        relation.put("postTaskVersion", 1);
        relation.put("conditionType", "NONE");
        relation.put("conditionParams", "{}");
        return relation;
    }

    @Override
    public void asyncBatchExecuteWithTimeParams(User loginUser, long projectCode, long code, int start, int end, int step, String replaceStr, Boolean isReverse) {
        log.info(">>> 补数任务启动: ProjectCode={}, Code={}, Range=[{}, {}], Step={}",
                projectCode, code, start, end, step);
        try {

            // 1. 获取工作流定义，确保工作流在线
            ProcessDefinition processDefinition = processDefinitionMapper.queryByCode(code);
            if (processDefinition == null) {
                log.error("未找到工作流定义: {}", code);
                return;
            }

            // 2. 确保工作流在线
            if (processDefinition.getReleaseState() != ReleaseState.ONLINE) {
                log.info(">>> 工作流当前离线，先上线");
                onlineWorkflowDefinition(loginUser, projectCode, code);
            }

            // 3. 确定循环方向
            boolean reverse = (isReverse != null && isReverse);
            log.info(">>> 执行方向: {}", reverse ? "反向(从现在往过去)" : "正向(从过去往现在)");

            int directionStep = reverse ? -Math.abs(step) : step;

            // 3. 处理每个偏移量 - 串行执行
            for (int currentOffset = start; reverse ? (currentOffset >= end) : (currentOffset <= end); currentOffset += directionStep) {

                log.info(">>> 正在处理偏移量: {} 天", currentOffset);

                // 4. 构建执行参数，覆盖时间参数
                Map<String, String> startParams = new HashMap<>();

                // 对标准时间格式的支持
                String fullTodayEnd = generateDateParam("yyyy-MM-dd", currentOffset + step) + " 00:00:00";
                String fullTodayStart = generateDateParam("yyyy-MM-dd", currentOffset) + " 00:00:00";
                String today = generateDateParam("yyyy-MM-dd", currentOffset + step);
                String yesterday = generateDateParam("yyyy-MM-dd", currentOffset);

                // 对时间戳格式的支持
//                String timestampEnd = generateTimestampParam("yyyyMMdd000000", currentOffset + step);
//                String timestampStart = generateTimestampParam("yyyyMMdd000000", currentOffset);

                // 根据你的实际参数名设置
                startParams.put("full_today_start", fullTodayEnd);
                startParams.put("full_yesterday_start", fullTodayStart);
                startParams.put("today", today);
                startParams.put("yesterday", yesterday);

                Map<String, Object> runResult = execService.execProcessInstance(
                        loginUser,
                        projectCode,
                        code,
                        "now",  // scheduleTime: 立即执行
                        CommandType.START_PROCESS,  // execType
                        FailureStrategy.CONTINUE,  // failureStrategy
                        "",  // startNodeList
                        TaskDependType.TASK_POST,  // taskDependType
                        WarningType.NONE,  // warningType
                        null,  // warningGroupId
                        RunMode.RUN_MODE_SERIAL,  // runMode
                        Priority.MEDIUM,  // processInstancePriority
                        "default",  // workerGroup
                        "default",  // tenantCode
                        -1L,  // environmentCode
                        7000,  // timeout
                        startParams,  // 这里传递覆盖的参数！
                        null,  // expectedParallelismNumber
                        0,  // dryRun
                        0,  // testFlag
                        ComplementDependentMode.OFF_MODE,  // complementDependentMode
                        null,  // version
                        false,  // allLevelDependent
                        ExecutionOrder.DESC_ORDER  // executionOrder
                );

                if (runResult != null && Status.SUCCESS.equals(runResult.get(Constants.STATUS))) {
                    log.info(">>> 执行成功，偏移量: {}", currentOffset);

                    // 6. 等待这个工作流的所有任务实例执行完成
                    waitForAllTasksComplete(projectCode, code);
                    log.info(">>> 偏移量 {} 执行完成", currentOffset);
                } else {
                    log.error("执行失败，偏移量: {}, 原因: {}",
                            currentOffset, runResult != null ? runResult.get(Constants.MSG) : "Unknown");
                    // break; // 如果要停止后续执行，取消注释这行
                }

//                 7. 短暂延迟，再执行下一个
                Thread.sleep(10000);
            }

            int totalCount = Math.abs((end - start) / Math.abs(step)) + 1;
            log.info(">>> 补数任务全部结束，共处理 {} 个偏移量", totalCount);

        } catch (Exception e) {
            log.error("补数任务线程发生异常中断", e);
        }
    }

    private void waitForAllTasksComplete(long projectCode, long workflowCode) {
        int maxWaitMinutes = 120;
        long startTime = System.currentTimeMillis();
        int[] runningStates = new int[]{0, 1, 12, 17};

        while (true) {
            List<ProcessInstance> runningList = processInstanceMapper.queryByProcessDefineCodeAndStatus(
                    workflowCode,
                    runningStates
            );

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            List<ProcessInstance> runningList1 = processInstanceMapper.queryByProcessDefineCodeAndStatus(
                    workflowCode,
                    runningStates
            );

            // 修复：分别判断两个列表是否为空，而不是相加
            boolean allEmpty = (runningList == null || runningList.isEmpty())
                    && (runningList1 == null || runningList1.isEmpty());

            if (allEmpty) {
                log.info(">>> 工作流 {} 运行结束。", workflowCode);
                return;
            }

            long elapsed = (System.currentTimeMillis() - startTime) / (1000 * 60);
            if (elapsed >= maxWaitMinutes) {
                log.error(">>> 补数任务等待超时");
                throw new RuntimeException("Wait workflow timeout");
            }

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            log.info(">>> 工作流 {} 仍在运行，已等待 {} 分钟...", workflowCode, elapsed);
        }
    }


    /**
     * 生成 $[yyyy-MM-dd] 格式的日期表达式
     */
    private String generateDateParam(String pattern, int offset) {
        if (offset == 0) {
            return String.format("$[%s]", pattern);
        } else if (offset > 0) {
            return String.format("$[%s+%d]", pattern, offset);
        } else {
            return String.format("$[%s%d]", pattern, offset);
        }
    }

    /**
     * 生成 $[timestamp_ms(yyyyMMdd000000)] 格式的时间戳表达式
     */
    private String generateTimestampParam(String pattern, int offset) {
        if (offset == 0) {
            return String.format("$[timestamp_ms(%s)]", pattern);
        } else if (offset > 0) {
            return String.format("$[timestamp_ms(%s+%d)]", pattern, offset);
        } else {
            return String.format("$[timestamp_ms(%s%d)]", pattern, offset);
        }
    }


    protected Map<String, Object> createDagDefine(User loginUser, List<ProcessTaskRelationLog> taskRelationList, ProcessDefinition processDefinition, List<TaskDefinitionLog> taskDefinitionLogs) {
        Map<String, Object> result = new HashMap<>();
        int saveTaskResult = processService.saveTaskDefine(loginUser, processDefinition.getProjectCode(), taskDefinitionLogs, Boolean.TRUE);
        if (saveTaskResult == Constants.EXIT_CODE_SUCCESS) {
            log.info("The task has not changed, so skip");
        }
        if (saveTaskResult == DEFINITION_FAILURE) {
            log.error("Save task definition error.");
            throw new ServiceException(Status.CREATE_TASK_DEFINITION_ERROR);
        }
        int insertVersion = processService.saveProcessDefine(loginUser, processDefinition, Boolean.TRUE, Boolean.TRUE);
        if (insertVersion == 0) {
            log.error("Save process definition error, processCode:{}.", processDefinition.getCode());
            throw new ServiceException(Status.CREATE_PROCESS_DEFINITION_ERROR);
        } else {
            log.info("Save process definition complete, processCode:{}, processVersion:{}.", processDefinition.getCode(), insertVersion);
        }
        int insertResult = processService.saveTaskRelation(loginUser, processDefinition.getProjectCode(), processDefinition.getCode(), insertVersion, taskRelationList, taskDefinitionLogs, Boolean.TRUE);
        if (insertResult != Constants.EXIT_CODE_SUCCESS) {
            log.error("Save process task relations error, projectCode:{}, processCode:{}, processVersion:{}.", processDefinition.getProjectCode(), processDefinition.getCode(), insertVersion);
            throw new ServiceException(Status.CREATE_PROCESS_TASK_RELATION_ERROR);
        } else {
            log.info("Save process task relations complete, projectCode:{}, processCode:{}, processVersion:{}.", processDefinition.getProjectCode(), processDefinition.getCode(), insertVersion);
        }

        putMsg(result, Status.SUCCESS);
        result.put(DATA_LIST, processDefinition);
        return result;
    }

    private List<TaskDefinitionLog> generateTaskDefinitionList(String taskDefinitionJson) {
        try {
            List<TaskDefinitionLog> taskDefinitionLogs = JSONUtils.toList(taskDefinitionJson, TaskDefinitionLog.class);
            if (CollectionUtils.isEmpty(taskDefinitionLogs)) {
                log.error("Generate task definition list failed, the given taskDefinitionJson is invalided: {}", taskDefinitionJson);
                throw new ServiceException(Status.DATA_IS_NOT_VALID, taskDefinitionJson);
            }
            for (TaskDefinitionLog taskDefinitionLog : taskDefinitionLogs) {
                if (!TaskPluginManager.checkTaskParameters(ParametersNode.builder().taskType(taskDefinitionLog.getTaskType()).taskParams(taskDefinitionLog.getTaskParams()).dependence(taskDefinitionLog.getDependence()).build())) {
                    log.error("Generate task definition list failed, the given task definition parameter is invalided, taskName: {}, taskDefinition: {}", taskDefinitionLog.getName(), taskDefinitionLog);
                    throw new ServiceException(Status.PROCESS_NODE_S_PARAMETER_INVALID, taskDefinitionLog.getName());
                }
            }
            return taskDefinitionLogs;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("Generate task definition list failed, meet an unknown exception", e);
            throw new ServiceException(Status.REQUEST_PARAMS_NOT_VALID_ERROR);
        }
    }

    private List<ProcessTaskRelationLog> generateTaskRelationList(String taskRelationJson, List<TaskDefinitionLog> taskDefinitionLogs) {
        try {
            List<ProcessTaskRelationLog> taskRelationList = JSONUtils.toList(taskRelationJson, ProcessTaskRelationLog.class);
            if (CollectionUtils.isEmpty(taskRelationList)) {
                log.error("Generate task relation list failed the taskRelation list is empty, taskRelationJson: {}", taskRelationJson);
                throw new ServiceException(Status.DATA_IS_NOT_VALID);
            }
            List<ProcessTaskRelation> processTaskRelations = taskRelationList.stream().map(processTaskRelationLog -> JSONUtils.parseObject(JSONUtils.toJsonString(processTaskRelationLog), ProcessTaskRelation.class)).collect(Collectors.toList());
            List<TaskNode> taskNodeList = processService.transformTask(processTaskRelations, taskDefinitionLogs);
            if (taskNodeList.size() != taskRelationList.size()) {
                Set<Long> postTaskCodes = taskRelationList.stream().map(ProcessTaskRelationLog::getPostTaskCode).collect(toSet());
                Set<Long> taskNodeCodes = taskNodeList.stream().map(TaskNode::getCode).collect(toSet());
                Collection<Long> codes = CollectionUtils.subtract(postTaskCodes, taskNodeCodes);
                if (CollectionUtils.isNotEmpty(codes)) {
                    String taskCodes = StringUtils.join(codes, Constants.COMMA);
                    log.error("Task definitions do not exist, taskCodes:{}.", taskCodes);
                    throw new ServiceException(Status.TASK_DEFINE_NOT_EXIST, taskCodes);
                }
            }
            if (graphHasCycle(taskNodeList)) {
                log.error("Process DAG has cycle.");
                throw new ServiceException(Status.PROCESS_NODE_HAS_CYCLE);
            }

            // check whether the task relation json is normal
            for (ProcessTaskRelationLog processTaskRelationLog : taskRelationList) {
                if (processTaskRelationLog.getPostTaskCode() == 0) {
                    log.error("The post_task_code or post_task_version of processTaskRelationLog can not be zero, " + "processTaskRelationLogId:{}.", processTaskRelationLog.getId());
                    throw new ServiceException(Status.CHECK_PROCESS_TASK_RELATION_ERROR);
                }
            }
            return taskRelationList;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("Check task relation list error, meet an unknown exception, given taskRelationJson: {}", taskRelationJson, e);
            throw new ServiceException(Status.REQUEST_PARAMS_NOT_VALID_ERROR);
        }
    }

    /**
     * query process definition list
     *
     * @param loginUser   login user
     * @param projectCode project code
     * @return definition list
     */
    @Override
    public Map<String, Object> queryProcessDefinitionList(User loginUser, long projectCode) {
        Project project = projectMapper.queryByCode(projectCode);
        // check user access for project
        Map<String, Object> result = projectService.checkProjectAndAuth(loginUser, project, projectCode, WORKFLOW_DEFINITION);
        if (result.get(STATUS) != Status.SUCCESS) {
            return result;
        }
        List<ProcessDefinition> resourceList = processDefinitionMapper.queryAllDefinitionList(projectCode);
        List<DagData> dagDataList = resourceList.stream().map(processService::genDagData).collect(Collectors.toList());
        result.put(DATA_LIST, dagDataList);
        putMsg(result, Status.SUCCESS);
        return result;
    }

    /**
     * query process definition simple list
     *
     * @param loginUser   login user
     * @param projectCode project code
     * @return definition simple list
     */
    @Override
    public Map<String, Object> queryProcessDefinitionSimpleList(User loginUser, long projectCode) {
        Project project = projectMapper.queryByCode(projectCode);
        // check user access for project
        Map<String, Object> result = projectService.checkProjectAndAuth(loginUser, project, projectCode, WORKFLOW_DEFINITION);
        if (result.get(STATUS) != Status.SUCCESS) {
            return result;
        }
        List<ProcessDefinition> processDefinitions = processDefinitionMapper.queryAllDefinitionList(projectCode);
        ArrayNode arrayNode = JSONUtils.createArrayNode();
        for (ProcessDefinition processDefinition : processDefinitions) {
            ObjectNode processDefinitionNode = JSONUtils.createObjectNode();
            processDefinitionNode.put("id", processDefinition.getId());
            processDefinitionNode.put("code", processDefinition.getCode());
            processDefinitionNode.put("name", processDefinition.getName());
            processDefinitionNode.put("projectCode", processDefinition.getProjectCode());
            arrayNode.add(processDefinitionNode);
        }
        result.put(DATA_LIST, arrayNode);
        putMsg(result, Status.SUCCESS);
        return result;
    }

    /**
     * query process definition list paging
     *
     * @param loginUser   login user
     * @param projectCode project code
     * @param searchVal   search value
     * @param userId      user id
     * @param pageNo      page number
     * @param pageSize    page size
     * @return process definition page
     */
    @Override
    public PageInfo<ProcessDefinition> queryProcessDefinitionListPaging(@NonNull User loginUser, long projectCode, String searchVal, String otherParamsJson, String userId, Integer pageNo, Integer pageSize) {

        // check user access for project
        projectService.checkProjectAndAuthThrowException(loginUser, projectCode, WORKFLOW_DEFINITION);

        PageListingResult<ProcessDefinition> processDefinitionsPageListingResult = processDefinitionDao.listingProcessDefinition(pageNo, pageSize, searchVal, userId, projectCode);
        List<ProcessDefinition> processDefinitions = processDefinitionsPageListingResult.getRecords();

        List<Long> processDefinitionCodes = processDefinitions.stream().map(ProcessDefinition::getCode).collect(Collectors.toList());
        Map<Long, Schedule> scheduleMap = schedulerService.queryScheduleByProcessDefinitionCodes(processDefinitionCodes).stream().collect(Collectors.toMap(Schedule::getProcessDefinitionCode, Function.identity()));
        List<UserWithProcessDefinitionCode> userWithCodes = userMapper.queryUserWithProcessDefinitionCode(processDefinitionCodes);
        for (ProcessDefinition pd : processDefinitions) {
            userWithCodes.stream().filter(userWithCode -> userWithCode.getProcessDefinitionCode() == pd.getCode() && userWithCode.getProcessDefinitionVersion() == pd.getVersion()).findAny().ifPresent(userWithCode -> {
                pd.setModifyBy(userWithCode.getModifierName());
                pd.setUserName(userWithCode.getCreatorName());
            });
            Schedule schedule = scheduleMap.get(pd.getCode());
            pd.setScheduleReleaseState(schedule == null ? null : schedule.getReleaseState());
            pd.setSchedule(schedule);
        }

        PageInfo<ProcessDefinition> pageInfo = new PageInfo<>(pageNo, pageSize);
        pageInfo.setTotal((int) processDefinitionsPageListingResult.getTotalCount());
        pageInfo.setTotalList(processDefinitions);

        return pageInfo;
    }

    /**
     * Filter resource process definitions
     *
     * @param loginUser             login user
     * @param workflowFilterRequest workflow filter requests
     * @return List process definition
     */
    @Override
    public PageInfo<ProcessDefinition> filterProcessDefinition(User loginUser, WorkflowFilterRequest workflowFilterRequest) {
        ProcessDefinition processDefinition = workflowFilterRequest.convert2ProcessDefinition();
        if (workflowFilterRequest.getProjectName() != null) {
            Project project = projectMapper.queryByName(workflowFilterRequest.getProjectName());
            // check user access for project
            projectService.checkProjectAndAuthThrowException(loginUser, project, WORKFLOW_DEFINITION);
            processDefinition.setProjectCode(project.getCode());
        }

        Page<ProcessDefinition> page = new Page<>(workflowFilterRequest.getPageNo(), workflowFilterRequest.getPageSize());
        IPage<ProcessDefinition> processDefinitionIPage = processDefinitionMapper.filterProcessDefinition(page, processDefinition);

        List<ProcessDefinition> records = processDefinitionIPage.getRecords();
        for (ProcessDefinition pd : records) {
            ProcessDefinitionLog processDefinitionLog = processDefinitionLogMapper.queryByDefinitionCodeAndVersion(pd.getCode(), pd.getVersion());
            User user = userMapper.selectById(processDefinitionLog.getOperator());
            pd.setModifyBy(user.getUserName());
        }

        processDefinitionIPage.setRecords(records);
        PageInfo<ProcessDefinition> pageInfo = new PageInfo<>(workflowFilterRequest.getPageNo(), workflowFilterRequest.getPageSize());
        pageInfo.setTotal((int) processDefinitionIPage.getTotal());
        pageInfo.setTotalList(processDefinitionIPage.getRecords());

        return pageInfo;
    }

    /**
     * query detail of process definition
     *
     * @param loginUser   login user
     * @param projectCode project code
     * @param code        process definition code
     * @return process definition detail
     */
    @Override
    public Map<String, Object> queryProcessDefinitionByCode(User loginUser, long projectCode, long code) {
        Project project = projectMapper.queryByCode(projectCode);
        // check user access for project
        Map<String, Object> result = projectService.checkProjectAndAuth(loginUser, project, projectCode, WORKFLOW_DEFINITION);
        if (result.get(STATUS) != Status.SUCCESS) {
            return result;
        }

        ProcessDefinition processDefinition = processDefinitionMapper.queryByCode(code);
        if (processDefinition == null || projectCode != processDefinition.getProjectCode()) {
            log.error("Process definition does not exist, processCode:{}.", code);
            putMsg(result, PROCESS_DEFINE_NOT_EXIST, String.valueOf(code));
        } else {
            DagData dagData = processService.genDagData(processDefinition);
            result.put(DATA_LIST, dagData);
            putMsg(result, Status.SUCCESS);
        }
        return result;
    }

    /**
     * query detail of process definition
     *
     * @param loginUser login user
     * @param code      process definition code
     * @return process definition detail
     */
    @Override
    public ProcessDefinition getProcessDefinition(User loginUser, long code) {
        ProcessDefinition processDefinition = processDefinitionMapper.queryByCode(code);
        if (processDefinition == null) {
            throw new ServiceException(PROCESS_DEFINE_NOT_EXIST, String.valueOf(code));
        }

        Project project = projectMapper.queryByCode(processDefinition.getProjectCode());
        // check user access for project
        projectService.checkProjectAndAuthThrowException(loginUser, project, WORKFLOW_DEFINITION);

        return processDefinition;
    }

    @Override
    public Optional<ProcessDefinition> queryWorkflowDefinition(long workflowDefinitionCode, int workflowDefinitionVersion) {
        ProcessDefinition workflowDefinition = processDefinitionDao.queryByCode(workflowDefinitionCode).orElse(null);
        if (workflowDefinition == null || workflowDefinition.getVersion() != workflowDefinitionVersion) {
            workflowDefinition = processDefinitionLogDao.queryByDefinitionCodeAndVersion(workflowDefinitionCode, workflowDefinitionVersion);
        }
        return Optional.ofNullable(workflowDefinition);
    }

    @Override
    public ProcessDefinition queryWorkflowDefinitionThrowExceptionIfNotFound(long workflowDefinitionCode, int workflowDefinitionVersion) {
        return queryWorkflowDefinition(workflowDefinitionCode, workflowDefinitionVersion).orElseThrow(() -> new ServiceException(PROCESS_DEFINE_NOT_EXIST, String.valueOf(workflowDefinitionCode)));
    }

    @Override
    public Map<String, Object> queryProcessDefinitionByName(User loginUser, long projectCode, String name) {
        Project project = projectMapper.queryByCode(projectCode);
        // check user access for project
        Map<String, Object> result = projectService.checkProjectAndAuth(loginUser, project, projectCode, WORKFLOW_DEFINITION);
        if (result.get(STATUS) != Status.SUCCESS) {
            return result;
        }
        ProcessDefinition processDefinition = processDefinitionMapper.queryByDefineName(projectCode, name);

        if (processDefinition == null) {
            log.error("Process definition does not exist, projectCode:{}.", projectCode);
            putMsg(result, PROCESS_DEFINE_NOT_EXIST, name);
        } else {
            DagData dagData = processService.genDagData(processDefinition);
            result.put(DATA_LIST, dagData);
            putMsg(result, Status.SUCCESS);
        }
        return result;
    }

    /**
     * update  process definition
     *
     * @param loginUser          login user
     * @param projectCode        project code
     * @param name               process definition name
     * @param code               process definition code
     * @param description        description
     * @param globalParams       global params
     * @param locations          locations for nodes
     * @param timeout            timeout
     * @param taskRelationJson   relation json for nodes
     * @param taskDefinitionJson taskDefinitionJson
     * @return update result code
     */
    @Override
    @Transactional
    public Map<String, Object> updateProcessDefinition(User loginUser, long projectCode, String name, long code, String description, String globalParams, String locations, int timeout, String taskRelationJson, String taskDefinitionJson, ProcessExecutionTypeEnum executionType) {
        Project project = projectMapper.queryByCode(projectCode);
        // check if user have write perm for project
        Map<String, Object> result = new HashMap<>();
        boolean hasProjectAndWritePerm = projectService.hasProjectAndWritePerm(loginUser, project, result);
        if (!hasProjectAndWritePerm) {
            return result;
        }

        if (checkDescriptionLength(description)) {
            log.warn("Parameter description is too long.");
            putMsg(result, Status.DESCRIPTION_TOO_LONG_ERROR);
            return result;
        }
        List<TaskDefinitionLog> taskDefinitionLogs = generateTaskDefinitionList(taskDefinitionJson);
        List<ProcessTaskRelationLog> taskRelationList = generateTaskRelationList(taskRelationJson, taskDefinitionLogs);

        ProcessDefinition processDefinition = processDefinitionMapper.queryByCode(code);
        // check process definition exists
        if (processDefinition == null || projectCode != processDefinition.getProjectCode()) {
            log.error("Process definition does not exist, processCode:{}.", code);
            putMsg(result, PROCESS_DEFINE_NOT_EXIST, String.valueOf(code));
            return result;
        }
        if (processDefinition.getReleaseState() == ReleaseState.ONLINE) {
            // online can not permit edit
            log.warn("Process definition is not allowed to be modified due to {}, processDefinitionCode:{}.", ReleaseState.ONLINE.getDescp(), processDefinition.getCode());
            putMsg(result, Status.PROCESS_DEFINE_NOT_ALLOWED_EDIT, processDefinition.getName());
            return result;
        }
        if (!name.equals(processDefinition.getName())) {
            // check whether the new process define name exist
            ProcessDefinition definition = processDefinitionMapper.verifyByDefineName(project.getCode(), name);
            if (definition != null) {
                log.warn("Process definition with the same name already exists, processDefinitionCode:{}.", definition.getCode());
                putMsg(result, Status.PROCESS_DEFINITION_NAME_EXIST, name);
                return result;
            }
        }
        ProcessDefinition processDefinitionDeepCopy = JSONUtils.parseObject(JSONUtils.toJsonString(processDefinition), ProcessDefinition.class);
        processDefinition.set(projectCode, name, description, globalParams, locations, timeout);
        processDefinition.setExecutionType(executionType);
        result = updateDagDefine(loginUser, taskRelationList, processDefinition, processDefinitionDeepCopy, taskDefinitionLogs);
        if (result.get(STATUS) == Status.SUCCESS) {
            listenerEventAlertManager.publishProcessDefinitionUpdatedListenerEvent(loginUser, processDefinition, taskDefinitionLogs, taskRelationList);
        }
        return result;
    }

    /**
     * Task want to delete whether used in other task, should throw exception when have be used.
     * <p>
     * This function avoid delete task already dependencies by other tasks by accident.
     *
     * @param processDefinition ProcessDefinition you change task definition and task relation
     * @param taskRelationList  All the latest task relation list from process definition
     */
    private void taskUsedInOtherTaskValid(ProcessDefinition processDefinition, List<ProcessTaskRelationLog> taskRelationList) {
        List<ProcessTaskRelation> oldProcessTaskRelationList = processTaskRelationMapper.queryByProcessCode(processDefinition.getCode());
        Set<ProcessTaskRelationLog> oldProcessTaskRelationSet = oldProcessTaskRelationList.stream().map(ProcessTaskRelationLog::new).collect(toSet());
        StringBuilder sb = new StringBuilder();
        for (ProcessTaskRelationLog oldProcessTaskRelation : oldProcessTaskRelationSet) {
            boolean oldTaskExists = taskRelationList.stream().anyMatch(relation -> oldProcessTaskRelation.getPostTaskCode() == relation.getPostTaskCode());
            if (!oldTaskExists) {
                Optional<String> taskDepMsg = workFlowLineageService.taskDepOnTaskMsg(processDefinition.getProjectCode(), oldProcessTaskRelation.getProcessDefinitionCode(), oldProcessTaskRelation.getPostTaskCode());
                taskDepMsg.ifPresent(sb::append);
            }
            if (sb.length() != 0) {
                log.error("Task cannot be deleted because it is dependent");
                throw new ServiceException(sb.toString());
            }
        }
    }

    protected Map<String, Object> updateDagDefine(User loginUser, List<ProcessTaskRelationLog> taskRelationList, ProcessDefinition processDefinition, ProcessDefinition processDefinitionDeepCopy, List<TaskDefinitionLog> taskDefinitionLogs) {
        Map<String, Object> result = new HashMap<>();
        int saveTaskResult = processService.saveTaskDefine(loginUser, processDefinition.getProjectCode(), taskDefinitionLogs, Boolean.TRUE);
        if (saveTaskResult == Constants.EXIT_CODE_SUCCESS) {
            log.info("The task has not changed, so skip");
        }
        if (saveTaskResult == DEFINITION_FAILURE) {
            log.error("Update task definitions error, projectCode:{}, processCode:{}.", processDefinition.getProjectCode(), processDefinition.getCode());
            putMsg(result, Status.UPDATE_TASK_DEFINITION_ERROR);
            throw new ServiceException(Status.UPDATE_TASK_DEFINITION_ERROR);
        }
        boolean isChange = false;
        if (processDefinition.equals(processDefinitionDeepCopy) && saveTaskResult == Constants.EXIT_CODE_SUCCESS) {
            List<ProcessTaskRelationLog> processTaskRelationLogList = processTaskRelationLogMapper.queryByProcessCodeAndVersion(processDefinition.getCode(), processDefinition.getVersion());
            if (taskRelationList.size() == processTaskRelationLogList.size()) {
                Set<ProcessTaskRelationLog> taskRelationSet = new HashSet<>(taskRelationList);
                Set<ProcessTaskRelationLog> processTaskRelationLogSet = new HashSet<>(processTaskRelationLogList);
                if (taskRelationSet.size() == processTaskRelationLogSet.size()) {
                    taskRelationSet.removeAll(processTaskRelationLogSet);
                    if (!taskRelationSet.isEmpty()) {
                        isChange = true;
                    }
                } else {
                    isChange = true;
                }
            } else {
                isChange = true;
            }
        } else {
            isChange = true;
        }
        if (isChange) {
            log.info("Process definition needs to be updated, projectCode:{}, processCode:{}, processVersion:{}.", processDefinition.getProjectCode(), processDefinition.getCode(), processDefinition.getVersion());
            processDefinition.setUpdateTime(new Date());
            int insertVersion = processService.saveProcessDefine(loginUser, processDefinition, Boolean.TRUE, Boolean.TRUE);
            if (insertVersion <= 0) {
                log.error("Update process definition error, processCode:{}.", processDefinition.getCode());
                putMsg(result, Status.UPDATE_PROCESS_DEFINITION_ERROR);
                throw new ServiceException(Status.UPDATE_PROCESS_DEFINITION_ERROR);
            } else {
                log.info("Update process definition complete, processCode:{}, processVersion:{}.", processDefinition.getCode(), insertVersion);
            }

            taskUsedInOtherTaskValid(processDefinition, taskRelationList);
            int insertResult = processService.saveTaskRelation(loginUser, processDefinition.getProjectCode(), processDefinition.getCode(), insertVersion, taskRelationList, taskDefinitionLogs, Boolean.TRUE);
            if (insertResult == Constants.EXIT_CODE_SUCCESS) {
                log.info("Update process task relations complete, projectCode:{}, processCode:{}, processVersion:{}.", processDefinition.getProjectCode(), processDefinition.getCode(), insertVersion);
                putMsg(result, Status.SUCCESS);
                result.put(DATA_LIST, processDefinition);
            } else {
                log.error("Update process task relations error, projectCode:{}, processCode:{}, processVersion:{}.", processDefinition.getProjectCode(), processDefinition.getCode(), insertVersion);
                putMsg(result, Status.UPDATE_PROCESS_DEFINITION_ERROR);
                throw new ServiceException(Status.UPDATE_PROCESS_DEFINITION_ERROR);
            }
        } else {
            log.info("Process definition does not need to be updated because there is no change, projectCode:{}, processCode:{}, processVersion:{}.", processDefinition.getProjectCode(), processDefinition.getCode(), processDefinition.getVersion());
            putMsg(result, Status.SUCCESS);
            result.put(DATA_LIST, processDefinition);
        }
        return result;
    }

    /**
     * verify process definition name unique
     *
     * @param loginUser   login user
     * @param projectCode project code
     * @param name        name
     * @return true if process definition name not exists, otherwise false
     */
    @Override
    public Map<String, Object> verifyProcessDefinitionName(User loginUser, long projectCode, String name, long processDefinitionCode) {
        Project project = projectMapper.queryByCode(projectCode);
        // check user access for project
        Map<String, Object> result = projectService.checkProjectAndAuth(loginUser, project, projectCode, WORKFLOW_CREATE);
        if (result.get(STATUS) != Status.SUCCESS) {
            return result;
        }
        ProcessDefinition processDefinition = processDefinitionMapper.verifyByDefineName(project.getCode(), name.trim());
        if (processDefinition == null) {
            putMsg(result, Status.SUCCESS);
            return result;
        }
        if (processDefinitionCode != 0 && processDefinitionCode == processDefinition.getCode()) {
            putMsg(result, Status.SUCCESS);
            return result;
        }
        log.warn("Process definition with the same name {} already exists, processDefinitionCode:{}.", processDefinition.getName(), processDefinition.getCode());
        putMsg(result, Status.PROCESS_DEFINITION_NAME_EXIST, name.trim());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> batchDeleteProcessDefinitionByCodes(User loginUser, long projectCode, String codes) {
        Map<String, Object> result = new HashMap<>();
        if (StringUtils.isEmpty(codes)) {
            log.error("Parameter processDefinitionCodes is empty, projectCode is {}.", projectCode);
            putMsg(result, Status.PROCESS_DEFINITION_CODES_IS_EMPTY);
            return result;
        }

        Set<Long> definitionCodes = Lists.newArrayList(codes.split(Constants.COMMA)).stream().map(Long::parseLong).collect(toSet());
        List<ProcessDefinition> processDefinitionList = processDefinitionMapper.queryByCodes(definitionCodes);
        Set<Long> queryCodes = processDefinitionList.stream().map(ProcessDefinition::getCode).collect(toSet());
        // definitionCodes - queryCodes
        Set<Long> diffCode = definitionCodes.stream().filter(code -> !queryCodes.contains(code)).collect(toSet());

        if (CollectionUtils.isNotEmpty(diffCode)) {
            log.error("Process definition does not exist, processCodes:{}.", diffCode.stream().map(String::valueOf).collect(Collectors.joining(Constants.COMMA)));
            throw new ServiceException(Status.BATCH_DELETE_PROCESS_DEFINE_BY_CODES_ERROR, diffCode.stream().map(code -> code + "[process definition not exist]").collect(Collectors.joining(Constants.COMMA)));
        }

        for (ProcessDefinition process : processDefinitionList) {
            try {
                this.deleteProcessDefinitionByCode(loginUser, process.getCode());
                metricsCleanUpService.cleanUpWorkflowMetricsByDefinitionCode(process.getCode());
            } catch (Exception e) {
                throw new ServiceException(Status.DELETE_PROCESS_DEFINE_ERROR, process.getName(), e.getMessage());
            }
        }
        putMsg(result, Status.SUCCESS);
        return result;
    }

    /**
     * Process definition want to delete whether used in other task, should throw exception when have be used.
     * <p>
     * This function avoid delete process definition already dependencies by other tasks by accident.
     *
     * @param processDefinition ProcessDefinition you change task definition and task relation
     */
    private void processDefinitionUsedInOtherTaskValid(ProcessDefinition processDefinition) {
        // check process definition is already online
        if (processDefinition.getReleaseState() == ReleaseState.ONLINE) {
            throw new ServiceException(Status.PROCESS_DEFINE_STATE_ONLINE, processDefinition.getName());
        }

        // check process instances is already running
        List<ProcessInstance> processInstances = processInstanceService.queryByProcessDefineCodeAndStatus(processDefinition.getCode(), org.apache.dolphinscheduler.service.utils.Constants.NOT_TERMINATED_STATES);
        if (CollectionUtils.isNotEmpty(processInstances)) {
            throw new ServiceException(Status.DELETE_PROCESS_DEFINITION_EXECUTING_FAIL, processInstances.size());
        }

        // check process used by other task, including subprocess and dependent task type
        Set<TaskMainInfo> taskDepOnProcess = workFlowLineageService.queryTaskDepOnProcess(processDefinition.getProjectCode(), processDefinition.getCode());
        if (CollectionUtils.isNotEmpty(taskDepOnProcess)) {
            String taskDepDetail = taskDepOnProcess.stream().map(task -> String.format(FORMAT_S_S_COLON, task.getProcessDefinitionName(), task.getTaskName())).collect(Collectors.joining(Constants.COMMA));
            throw new ServiceException(Status.DELETE_PROCESS_DEFINITION_USE_BY_OTHER_FAIL, taskDepDetail);
        }
    }

    public void deleteProcessDefinitionByCode(User loginUser, long code) {
        ProcessDefinition processDefinition = processDefinitionDao.queryByCode(code).orElseThrow(() -> new ServiceException(PROCESS_DEFINE_NOT_EXIST, String.valueOf(code)));

        Project project = projectMapper.queryByCode(processDefinition.getProjectCode());
        // check user access for project
        projectService.checkProjectAndAuthThrowException(loginUser, project, WORKFLOW_DEFINITION_DELETE);

        // Determine if the login user is the owner of the process definition
        if (loginUser.getId() != processDefinition.getUserId() && loginUser.getUserType() != UserType.ADMIN_USER) {
            throw new ServiceException(Status.USER_NO_OPERATION_PERM);
        }

        processDefinitionUsedInOtherTaskValid(processDefinition);

        // get the timing according to the process definition
        Schedule scheduleObj = scheduleMapper.queryByProcessDefinitionCode(code);
        if (scheduleObj != null) {
            if (scheduleObj.getReleaseState() == ReleaseState.OFFLINE) {
                int delete = scheduleMapper.deleteById(scheduleObj.getId());
                if (delete == 0) {
                    throw new ServiceException(Status.DELETE_SCHEDULE_BY_ID_ERROR);
                }
            }
            if (scheduleObj.getReleaseState() == ReleaseState.ONLINE) {
                throw new ServiceException(Status.SCHEDULE_STATE_ONLINE, scheduleObj.getId());
            }
        }

        // delete workflow instance, will delete workflow instance, sub workflow instance, task instance, alert
        processInstanceService.deleteProcessInstanceByWorkflowDefinitionCode(processDefinition.getCode());
        // delete task definition
        taskDefinitionService.deleteTaskByWorkflowDefinitionCode(processDefinition.getCode(), processDefinition.getVersion());
        // delete task definition log
        taskDefinitionLogService.deleteTaskByWorkflowDefinitionCode(processDefinition.getCode());
        // delete workflow definition log
        processDefinitionLogDao.deleteByWorkflowDefinitionCode(processDefinition.getCode());

        // we delete the workflow definition at last to avoid using transaction here.
        // If delete error, we can call this interface again.
        processDefinitionDao.deleteByWorkflowDefinitionCode(processDefinition.getCode());
        metricsCleanUpService.cleanUpWorkflowMetricsByDefinitionCode(code);
        log.info("Success delete workflow definition workflowDefinitionCode: {}", code);
        listenerEventAlertManager.publishProcessDefinitionDeletedListenerEvent(loginUser, project, processDefinition);
    }

    /**
     * batch export process definition by codes
     */
    @Override
    public void batchExportProcessDefinitionByCodes(User loginUser, long projectCode, String codes, HttpServletResponse response) {
        if (StringUtils.isEmpty(codes)) {
            log.warn("Process definition codes to be exported is empty.");
            return;
        }
        Project project = projectMapper.queryByCode(projectCode);
        // check user access for project
        Map<String, Object> result = projectService.checkProjectAndAuth(loginUser, project, projectCode, WORKFLOW_DEFINITION_EXPORT);
        if (result.get(STATUS) != Status.SUCCESS) {
            return;
        }
        Set<Long> defineCodeSet = Lists.newArrayList(codes.split(Constants.COMMA)).stream().map(Long::parseLong).collect(toSet());
        List<ProcessDefinition> processDefinitionList = processDefinitionMapper.queryByCodes(defineCodeSet);
        if (CollectionUtils.isEmpty(processDefinitionList)) {
            log.error("Process definitions to be exported do not exist, processDefinitionCodes:{}.", defineCodeSet);
            return;
        }
        // check processDefinition exist in project
        List<ProcessDefinition> processDefinitionListInProject = processDefinitionList.stream().filter(o -> projectCode == o.getProjectCode()).collect(Collectors.toList());
        List<DagDataSchedule> dagDataSchedules = processDefinitionListInProject.stream().map(this::exportProcessDagData).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(dagDataSchedules)) {
            log.info("Start download process definition file, processDefinitionCodes:{}.", defineCodeSet);
            downloadProcessDefinitionFile(response, dagDataSchedules);
        } else {
            log.error("There is no exported process dag data.");
        }
    }

    /**
     * download the process definition file
     */
    protected void downloadProcessDefinitionFile(HttpServletResponse response, List<DagDataSchedule> dagDataSchedules) {
        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        BufferedOutputStream buff = null;
        ServletOutputStream out = null;
        try {
            out = response.getOutputStream();
            buff = new BufferedOutputStream(out);
            buff.write(JSONUtils.toPrettyJsonString(dagDataSchedules).getBytes(StandardCharsets.UTF_8));
            buff.flush();
            buff.close();
        } catch (IOException e) {
            log.warn("Export process definition fail", e);
        } finally {
            if (null != buff) {
                try {
                    buff.close();
                } catch (Exception e) {
                    log.warn("Buffer does not close", e);
                }
            }
            if (null != out) {
                try {
                    out.close();
                } catch (Exception e) {
                    log.warn("Output stream does not close", e);
                }
            }
        }
    }

    /**
     * get export process dag data
     *
     * @param processDefinition process definition
     * @return DagDataSchedule
     */
    public DagDataSchedule exportProcessDagData(ProcessDefinition processDefinition) {
        Schedule scheduleObj = scheduleMapper.queryByProcessDefinitionCode(processDefinition.getCode());
        DagDataSchedule dagDataSchedule = new DagDataSchedule(processService.genDagData(processDefinition));
        if (scheduleObj != null) {
            scheduleObj.setReleaseState(ReleaseState.OFFLINE);
            dagDataSchedule.setSchedule(scheduleObj);
        }
        return dagDataSchedule;
    }

    /**
     * import process definition
     *
     * @param loginUser   login user
     * @param projectCode project code
     * @param file        process metadata json file
     * @return import process
     */
    @Override
    @Transactional
    public Map<String, Object> importProcessDefinition(User loginUser, long projectCode, MultipartFile file) {
        Map<String, Object> result;
        String dagDataScheduleJson = FileUtils.file2String(file);
        List<DagDataSchedule> dagDataScheduleList = JSONUtils.toList(dagDataScheduleJson, DagDataSchedule.class);
        Project project = projectMapper.queryByCode(projectCode);
        result = projectService.checkProjectAndAuth(loginUser, project, projectCode, WORKFLOW_IMPORT);
        if (result.get(STATUS) != Status.SUCCESS) {
            return result;
        }
        // check file content
        if (CollectionUtils.isEmpty(dagDataScheduleList)) {
            log.warn("Process definition file content is empty.");
            putMsg(result, Status.DATA_IS_NULL, "fileContent");
            return result;
        }
        for (DagDataSchedule dagDataSchedule : dagDataScheduleList) {
            if (!checkAndImport(loginUser, projectCode, result, dagDataSchedule)) {
                return result;
            }
        }
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> importSqlProcessDefinition(User loginUser, long projectCode, MultipartFile file) {
        Map<String, Object> result;
        Project project = projectMapper.queryByCode(projectCode);
        result = projectService.checkProjectAndAuth(loginUser, project, projectCode, WORKFLOW_IMPORT);
        if (result.get(STATUS) != Status.SUCCESS) {
            return result;
        }
        String processDefinitionName = file.getOriginalFilename() == null ? file.getName() : file.getOriginalFilename();
        int index = processDefinitionName.lastIndexOf(".");
        if (index > 0) {
            processDefinitionName = processDefinitionName.substring(0, index);
        }
        processDefinitionName = getNewName(processDefinitionName, IMPORT_SUFFIX);

        ProcessDefinition processDefinition;
        List<TaskDefinitionLog> taskDefinitionList = new ArrayList<>();
        List<ProcessTaskRelationLog> processTaskRelationList = new ArrayList<>();

        // for Zip Bomb Attack
        final int THRESHOLD_ENTRIES = 10000;
        final int THRESHOLD_SIZE = 1000000000; // 1 GB
        final double THRESHOLD_RATIO = 10;
        int totalEntryArchive = 0;
        int totalSizeEntry = 0;
        // In most cases, there will be only one data source
        Map<String, DataSource> dataSourceCache = new HashMap<>(1);
        Map<String, Long> taskNameToCode = new HashMap<>(16);
        Map<String, List<String>> taskNameToUpstream = new HashMap<>(16);
        try (ZipInputStream zIn = new ZipInputStream(file.getInputStream()); BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(zIn))) {
            // build process definition
            processDefinition = new ProcessDefinition(projectCode, processDefinitionName, CodeGenerateUtils.genCode(), "", "[]", null, 0, loginUser.getId());

            ZipEntry entry;
            while ((entry = zIn.getNextEntry()) != null) {
                totalEntryArchive++;
                int totalSizeArchive = 0;
                if (!entry.isDirectory()) {
                    StringBuilder sql = new StringBuilder();
                    String taskName = null;
                    String datasourceName = null;
                    List<String> upstreams = Collections.emptyList();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        int nBytes = line.getBytes(StandardCharsets.UTF_8).length;
                        totalSizeEntry += nBytes;
                        totalSizeArchive += nBytes;
                        long compressionRatio = totalSizeEntry / entry.getCompressedSize();
                        if (compressionRatio > THRESHOLD_RATIO) {
                            throw new IllegalStateException("Ratio between compressed and uncompressed data is highly suspicious, looks like a Zip Bomb Attack.");
                        }
                        int commentIndex = line.indexOf("-- ");
                        if (commentIndex >= 0) {
                            int colonIndex = line.indexOf(":", commentIndex);
                            if (colonIndex > 0) {
                                String key = line.substring(commentIndex + 3, colonIndex).trim().toLowerCase();
                                String value = line.substring(colonIndex + 1).trim();
                                switch (key) {
                                    case "name":
                                        taskName = value;
                                        line = line.substring(0, commentIndex);
                                        break;
                                    case "upstream":
                                        upstreams = Arrays.stream(value.split(",")).map(String::trim).filter(s -> !"".equals(s)).collect(Collectors.toList());
                                        line = line.substring(0, commentIndex);
                                        break;
                                    case "datasource":
                                        datasourceName = value;
                                        line = line.substring(0, commentIndex);
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }
                        if (!"".equals(line)) {
                            sql.append(line).append("\n");
                        }
                    }
                    // import/sql1.sql -> sql1
                    if (taskName == null) {
                        taskName = entry.getName();
                        index = taskName.indexOf("/");
                        if (index > 0) {
                            taskName = taskName.substring(index + 1);
                        }
                        index = taskName.lastIndexOf(".");
                        if (index > 0) {
                            taskName = taskName.substring(0, index);
                        }
                    }
                    DataSource dataSource = dataSourceCache.get(datasourceName);
                    if (dataSource == null) {
                        dataSource = queryDatasourceByNameAndUser(datasourceName, loginUser);
                    }
                    if (dataSource == null) {
                        log.error("Datasource does not found, may be its name is illegal.");
                        putMsg(result, Status.DATASOURCE_NAME_ILLEGAL);
                        return result;
                    }
                    dataSourceCache.put(datasourceName, dataSource);

                    TaskDefinitionLog taskDefinition = buildNormalSqlTaskDefinition(taskName, dataSource, sql.substring(0, sql.length() - 1));

                    taskDefinitionList.add(taskDefinition);
                    taskNameToCode.put(taskDefinition.getName(), taskDefinition.getCode());
                    taskNameToUpstream.put(taskDefinition.getName(), upstreams);
                }

                if (totalSizeArchive > THRESHOLD_SIZE) {
                    throw new IllegalStateException("the uncompressed data size is too much for the application resource capacity");
                }

                if (totalEntryArchive > THRESHOLD_ENTRIES) {
                    throw new IllegalStateException("too much entries in this archive, can lead to inodes exhaustion of the system");
                }
            }
        } catch (Exception e) {
            log.error("Import process definition error.", e);
            putMsg(result, Status.IMPORT_PROCESS_DEFINE_ERROR);
            return result;
        }

        // build task relation
        for (Map.Entry<String, Long> entry : taskNameToCode.entrySet()) {
            List<String> upstreams = taskNameToUpstream.get(entry.getKey());
            if (CollectionUtils.isEmpty(upstreams) || (upstreams.size() == 1 && upstreams.contains("root") && !taskNameToCode.containsKey("root"))) {
                ProcessTaskRelationLog processTaskRelation = buildNormalTaskRelation(0, entry.getValue());
                processTaskRelationList.add(processTaskRelation);
                continue;
            }
            for (String upstream : upstreams) {
                ProcessTaskRelationLog processTaskRelation = buildNormalTaskRelation(taskNameToCode.get(upstream), entry.getValue());
                processTaskRelationList.add(processTaskRelation);
            }
        }

        return createDagDefine(loginUser, processTaskRelationList, processDefinition, taskDefinitionList);
    }

    private ProcessTaskRelationLog buildNormalTaskRelation(long preTaskCode, long postTaskCode) {
        ProcessTaskRelationLog processTaskRelation = new ProcessTaskRelationLog();
        processTaskRelation.setPreTaskCode(preTaskCode);
        processTaskRelation.setPreTaskVersion(0);
        processTaskRelation.setPostTaskCode(postTaskCode);
        processTaskRelation.setPostTaskVersion(0);
        processTaskRelation.setConditionType(ConditionType.NONE);
        processTaskRelation.setName("");
        return processTaskRelation;
    }

    private DataSource queryDatasourceByNameAndUser(String datasourceName, User loginUser) {
        if (isAdmin(loginUser)) {
            List<DataSource> dataSources = dataSourceMapper.queryDataSourceByName(datasourceName);
            if (CollectionUtils.isNotEmpty(dataSources)) {
                return dataSources.get(0);
            }
        } else {
            return dataSourceMapper.queryDataSourceByNameAndUserId(loginUser.getId(), datasourceName);
        }
        return null;
    }

    private TaskDefinitionLog buildNormalSqlTaskDefinition(String taskName, DataSource dataSource, String sql) throws CodeGenerateException {
        TaskDefinitionLog taskDefinition = new TaskDefinitionLog();
        taskDefinition.setName(taskName);
        taskDefinition.setFlag(Flag.YES);
        SqlParameters sqlParameters = new SqlParameters();
        sqlParameters.setType(dataSource.getType().name());
        sqlParameters.setDatasource(dataSource.getId());
        sqlParameters.setSql(sql.substring(0, sql.length() - 1));
        // it may be a query type, but it can only be determined by parsing SQL
        sqlParameters.setSqlType(SqlType.NON_QUERY.ordinal());
        sqlParameters.setLocalParams(Collections.emptyList());
        taskDefinition.setTaskParams(JSONUtils.toJsonString(sqlParameters));
        taskDefinition.setCode(CodeGenerateUtils.genCode());
        taskDefinition.setTaskType(TASK_TYPE_SQL);
        taskDefinition.setFailRetryTimes(0);
        taskDefinition.setFailRetryInterval(0);
        taskDefinition.setTimeoutFlag(TimeoutFlag.CLOSE);
        taskDefinition.setWorkerGroup(WorkerGroupUtils.getDefaultWorkerGroup());
        taskDefinition.setTaskPriority(Priority.MEDIUM);
        taskDefinition.setEnvironmentCode(-1);
        taskDefinition.setTimeout(0);
        taskDefinition.setDelayTime(0);
        taskDefinition.setTimeoutNotifyStrategy(TaskTimeoutStrategy.WARN);
        taskDefinition.setVersion(0);
        taskDefinition.setResourceIds("");
        return taskDefinition;
    }

    /**
     * check and import
     */
    protected boolean checkAndImport(User loginUser, long projectCode, Map<String, Object> result, DagDataSchedule dagDataSchedule) {
        if (!checkImportanceParams(dagDataSchedule, result)) {
            return false;
        }
        ProcessDefinition processDefinition = dagDataSchedule.getProcessDefinition();

        // generate import processDefinitionName
        String processDefinitionName = recursionProcessDefinitionName(projectCode, processDefinition.getName(), 1);
        String importProcessDefinitionName = getNewName(processDefinitionName, IMPORT_SUFFIX);
        // unique check
        Map<String, Object> checkResult = verifyProcessDefinitionName(loginUser, projectCode, importProcessDefinitionName, 0);
        if (Status.SUCCESS.equals(checkResult.get(STATUS))) {
            putMsg(result, Status.SUCCESS);
        } else {
            result.putAll(checkResult);
            return false;
        }
        processDefinition.setName(importProcessDefinitionName);
        processDefinition.setId(null);
        processDefinition.setProjectCode(projectCode);
        processDefinition.setUserId(loginUser.getId());
        try {
            processDefinition.setCode(CodeGenerateUtils.genCode());
        } catch (CodeGenerateException e) {
            log.error("Save process definition error because generate process definition code error, projectCode:{}.", projectCode, e);
            putMsg(result, Status.CREATE_PROCESS_DEFINITION_ERROR);
            return false;
        }
        List<TaskDefinition> taskDefinitionList = dagDataSchedule.getTaskDefinitionList();
        Map<Long, Long> taskCodeMap = new HashMap<>();
        Date now = new Date();
        List<TaskDefinitionLog> taskDefinitionLogList = new ArrayList<>();
        for (TaskDefinition taskDefinition : taskDefinitionList) {
            TaskDefinitionLog taskDefinitionLog = new TaskDefinitionLog(taskDefinition);
            taskDefinitionLog.setName(taskDefinitionLog.getName());
            taskDefinitionLog.setProjectCode(projectCode);
            taskDefinitionLog.setUserId(loginUser.getId());
            taskDefinitionLog.setVersion(VERSION_FIRST);
            taskDefinitionLog.setCreateTime(now);
            taskDefinitionLog.setUpdateTime(now);
            taskDefinitionLog.setOperator(loginUser.getId());
            taskDefinitionLog.setOperateTime(now);
            try {
                long code = CodeGenerateUtils.genCode();
                taskCodeMap.put(taskDefinitionLog.getCode(), code);
                taskDefinitionLog.setCode(code);
            } catch (CodeGenerateException e) {
                log.error("Generate task definition code error, projectCode:{}, processDefinitionCode:{}", projectCode, processDefinition.getCode(), e);
                putMsg(result, Status.INTERNAL_SERVER_ERROR_ARGS, "Error generating task definition code");
                return false;
            }
            taskDefinitionLogList.add(taskDefinitionLog);
        }
        int insert = taskDefinitionMapper.batchInsert(taskDefinitionLogList);
        int logInsert = taskDefinitionLogMapper.batchInsert(taskDefinitionLogList);
        if ((logInsert & insert) == 0) {
            log.error("Save task definition error, projectCode:{}, processDefinitionCode:{}", projectCode, processDefinition.getCode());
            putMsg(result, Status.CREATE_TASK_DEFINITION_ERROR);
            throw new ServiceException(Status.CREATE_TASK_DEFINITION_ERROR);
        }

        List<ProcessTaskRelation> taskRelationList = dagDataSchedule.getProcessTaskRelationList();
        List<ProcessTaskRelationLog> taskRelationLogList = new ArrayList<>();
        for (ProcessTaskRelation processTaskRelation : taskRelationList) {
            ProcessTaskRelationLog processTaskRelationLog = new ProcessTaskRelationLog(processTaskRelation);
            if (taskCodeMap.containsKey(processTaskRelationLog.getPreTaskCode())) {
                processTaskRelationLog.setPreTaskCode(taskCodeMap.get(processTaskRelationLog.getPreTaskCode()));
            }
            if (taskCodeMap.containsKey(processTaskRelationLog.getPostTaskCode())) {
                processTaskRelationLog.setPostTaskCode(taskCodeMap.get(processTaskRelationLog.getPostTaskCode()));
            }
            processTaskRelationLog.setPreTaskVersion(VERSION_FIRST);
            processTaskRelationLog.setPostTaskVersion(VERSION_FIRST);
            taskRelationLogList.add(processTaskRelationLog);
        }
        if (StringUtils.isNotEmpty(processDefinition.getLocations()) && JSONUtils.checkJsonValid(processDefinition.getLocations())) {
            ArrayNode arrayNode = JSONUtils.parseArray(processDefinition.getLocations());
            ArrayNode newArrayNode = JSONUtils.createArrayNode();

            //处理seatunnel子算子的taskcode为null问题，原来location保留
            for (int i = 0; i < arrayNode.size(); i++) {
                ObjectNode oldNode = (ObjectNode) arrayNode.path(i);
                newArrayNode.add(oldNode);
            }

            ArrayNode jsonNodes = JSONUtils.parseArray(processDefinition.getLocations());
            for (int i = 0; i < jsonNodes.size(); i++) {
                ObjectNode node = (ObjectNode) jsonNodes.path(i);
                node.put("taskCode", taskCodeMap.get(node.get("taskCode").asLong()));
//                        jsonNodes.set(i, node);
                newArrayNode.add(node);
            }

            processDefinition.setLocations(newArrayNode.toString());
        }
        processDefinition.setCreateTime(new Date());
        processDefinition.setUpdateTime(new Date());
        Map<String, Object> createDagResult = createDagDefine(loginUser, taskRelationLogList, processDefinition, Lists.newArrayList());
        if (Status.SUCCESS.equals(createDagResult.get(STATUS))) {
            putMsg(createDagResult, Status.SUCCESS);
        } else {
            result.putAll(createDagResult);
            log.error("Import process definition error, projectCode:{}, processDefinitionCode:{}.", projectCode, processDefinition.getCode());
            throw new ServiceException(Status.IMPORT_PROCESS_DEFINE_ERROR);
        }

        Schedule schedule = dagDataSchedule.getSchedule();
        if (null != schedule) {
            ProcessDefinition newProcessDefinition = processDefinitionMapper.queryByCode(processDefinition.getCode());
            schedule.setProcessDefinitionCode(newProcessDefinition.getCode());
            schedule.setId(null);
            schedule.setUserId(loginUser.getId());
            schedule.setCreateTime(now);
            schedule.setUpdateTime(now);
            int scheduleInsert = scheduleMapper.insert(schedule);
            if (0 == scheduleInsert) {
                log.error("Import process definition error due to save schedule fail, projectCode:{}, processDefinitionCode:{}.", projectCode, processDefinition.getCode());
                putMsg(result, Status.IMPORT_PROCESS_DEFINE_ERROR);
                throw new ServiceException(Status.IMPORT_PROCESS_DEFINE_ERROR);
            }
        }

        result.put(DATA_LIST, processDefinition);
        log.info("Import process definition complete, projectCode:{}, processDefinitionCode:{}.", projectCode, processDefinition.getCode());
        return true;
    }

    /**
     * check importance params
     */
    private boolean checkImportanceParams(DagDataSchedule dagDataSchedule, Map<String, Object> result) {
        if (dagDataSchedule.getProcessDefinition() == null) {
            log.warn("Process definition is null.");
            putMsg(result, Status.DATA_IS_NULL, "ProcessDefinition");
            return false;
        }
        if (CollectionUtils.isEmpty(dagDataSchedule.getTaskDefinitionList())) {
            log.warn("Task definition list is null.");
            putMsg(result, Status.DATA_IS_NULL, "TaskDefinitionList");
            return false;
        }
        if (CollectionUtils.isEmpty(dagDataSchedule.getProcessTaskRelationList())) {
            log.warn("Process task relation list is null.");
            putMsg(result, Status.DATA_IS_NULL, "ProcessTaskRelationList");
            return false;
        }
        return true;
    }

    private String recursionProcessDefinitionName(long projectCode, String processDefinitionName, int num) {
        ProcessDefinition processDefinition = processDefinitionMapper.queryByDefineName(projectCode, processDefinitionName);
        if (processDefinition != null) {
            if (num > 1) {
                String str = processDefinitionName.substring(0, processDefinitionName.length() - 3);
                processDefinitionName = str + "(" + num + ")";
            } else {
                processDefinitionName = processDefinition.getName() + "(" + num + ")";
            }
        } else {
            return processDefinitionName;
        }
        return recursionProcessDefinitionName(projectCode, processDefinitionName, num + 1);
    }

    /**
     * check the process task relation json
     *
     * @param processTaskRelationJson process task relation json
     * @return check result code
     */
    @Override
    public Map<String, Object> checkProcessNodeList(String processTaskRelationJson, List<TaskDefinitionLog> taskDefinitionLogsList) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (processTaskRelationJson == null) {
                log.error("Process task relation data is null.");
                putMsg(result, Status.DATA_IS_NOT_VALID, processTaskRelationJson);
                return result;
            }

            List<ProcessTaskRelation> taskRelationList = JSONUtils.toList(processTaskRelationJson, ProcessTaskRelation.class);
            // Check whether the task node is normal
            List<TaskNode> taskNodes = processService.transformTask(taskRelationList, taskDefinitionLogsList);

            if (CollectionUtils.isEmpty(taskNodes)) {
                log.error("Task node data is empty.");
                putMsg(result, Status.PROCESS_DAG_IS_EMPTY);
                return result;
            }

            // check has cycle
            if (graphHasCycle(taskNodes)) {
                log.error("Process DAG has cycle.");
                putMsg(result, Status.PROCESS_NODE_HAS_CYCLE);
                return result;
            }

            // check whether the process definition json is normal
            for (TaskNode taskNode : taskNodes) {
                if (!TaskPluginManager.checkTaskParameters(ParametersNode.builder().taskType(taskNode.getType()).taskParams(taskNode.getTaskParams()).dependence(taskNode.getDependence()).switchResult(taskNode.getSwitchResult()).build())) {
                    log.error("Task node {} parameter invalid.", taskNode.getName());
                    putMsg(result, Status.PROCESS_NODE_S_PARAMETER_INVALID, taskNode.getName());
                    return result;
                }

                // check extra params
                CheckUtils.checkOtherParams(taskNode.getExtras());
            }
            putMsg(result, Status.SUCCESS);
        } catch (Exception e) {
            result.put(STATUS, Status.INTERNAL_SERVER_ERROR_ARGS);
            putMsg(result, Status.INTERNAL_SERVER_ERROR_ARGS, e.getMessage());
            log.error(Status.INTERNAL_SERVER_ERROR_ARGS.getMsg(), e);
        }
        return result;
    }

    /**
     * get task node details based on process definition
     *
     * @param loginUser   loginUser
     * @param projectCode project code
     * @param code        process definition code
     * @return task node list
     */
    @Override
    public Map<String, Object> getTaskNodeListByDefinitionCode(User loginUser, long projectCode, long code) {
        Project project = projectMapper.queryByCode(projectCode);
        // check user access for project
        Map<String, Object> result = projectService.checkProjectAndAuth(loginUser, project, projectCode, null);
        if (result.get(STATUS) != Status.SUCCESS) {
            return result;
        }
        ProcessDefinition processDefinition = processDefinitionMapper.queryByCode(code);
        if (processDefinition == null || projectCode != processDefinition.getProjectCode()) {
            log.error("Process definition does not exist, processDefinitionCode:{}.", code);
            putMsg(result, PROCESS_DEFINE_NOT_EXIST, String.valueOf(code));
            return result;
        }
        DagData dagData = processService.genDagData(processDefinition);
        result.put(DATA_LIST, dagData.getTaskDefinitionList());
        putMsg(result, Status.SUCCESS);

        return result;
    }

    /**
     * get task node details map based on process definition
     *
     * @param loginUser   loginUser
     * @param projectCode project code
     * @param codes       define codes
     * @return task node list
     */
    @Override
    public Map<String, Object> getNodeListMapByDefinitionCodes(User loginUser, long projectCode, String codes) {
        Project project = projectMapper.queryByCode(projectCode);
        // check user access for project
        Map<String, Object> result = projectService.checkProjectAndAuth(loginUser, project, projectCode, null);
        if (result.get(STATUS) != Status.SUCCESS) {
            return result;
        }

        Set<Long> defineCodeSet = Lists.newArrayList(codes.split(Constants.COMMA)).stream().map(Long::parseLong).collect(toSet());
        List<ProcessDefinition> processDefinitionList = processDefinitionMapper.queryByCodes(defineCodeSet);
        if (CollectionUtils.isEmpty(processDefinitionList)) {
            log.error("Process definitions do not exist, codes:{}.", defineCodeSet);
            putMsg(result, PROCESS_DEFINE_NOT_EXIST, codes);
            return result;
        }
        HashMap<Long, Project> userProjects = new HashMap<>(DEFAULT_HASH_MAP_SIZE);
        projectMapper.queryProjectCreatedAndAuthorizedByUserId(loginUser.getId()).forEach(userProject -> userProjects.put(userProject.getCode(), userProject));

        // check processDefinition exist in project
        List<ProcessDefinition> processDefinitionListInProject = processDefinitionList.stream().filter(o -> userProjects.containsKey(o.getProjectCode())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(processDefinitionListInProject)) {
            Set<Long> codesInProject = processDefinitionListInProject.stream().map(ProcessDefinition::getCode).collect(toSet());
            log.error("Process definitions do not exist in project, projectCode:{}, processDefinitionsCodes:{}.", processDefinitionListInProject.get(0).getProjectCode(), codesInProject);
            putMsg(result, PROCESS_DEFINE_NOT_EXIST, codes);
            return result;
        }
        Map<Long, List<TaskDefinition>> taskNodeMap = new HashMap<>();
        for (ProcessDefinition processDefinition : processDefinitionListInProject) {
            DagData dagData = processService.genDagData(processDefinition);
            taskNodeMap.put(processDefinition.getCode(), dagData.getTaskDefinitionList());
        }

        result.put(DATA_LIST, taskNodeMap);
        putMsg(result, Status.SUCCESS);

        return result;

    }

    /**
     * query process definition all by project code
     *
     * @param loginUser   loginUser
     * @param projectCode project code
     * @return process definitions in the project
     */
    @Override
    public Map<String, Object> queryAllProcessDefinitionByProjectCode(User loginUser, long projectCode) {
        Project project = projectMapper.queryByCode(projectCode);
        // check user access for project
        Map<String, Object> result = projectService.checkProjectAndAuth(loginUser, project, projectCode, WORKFLOW_DEFINITION);
        if (result.get(STATUS) != Status.SUCCESS) {
            return result;
        }
        List<ProcessDefinition> processDefinitions = processDefinitionMapper.queryAllDefinitionList(projectCode);
        List<DagData> dagDataList = processDefinitions.stream().map(processService::genDagData).collect(Collectors.toList());
        result.put(DATA_LIST, dagDataList);
        putMsg(result, Status.SUCCESS);
        return result;
    }

    /**
     * query process definition list by project code
     *
     * @param projectCode project code
     * @return process definition list in the project
     */
    @Override
    public Map<String, Object> queryProcessDefinitionListByProjectCode(long projectCode) {
        Map<String, Object> result = new HashMap<>();
        List<DependentSimplifyDefinition> processDefinitions = processDefinitionMapper.queryDefinitionListByProjectCodeAndProcessDefinitionCodes(projectCode, null);
        result.put(DATA_LIST, processDefinitions);
        putMsg(result, Status.SUCCESS);
        return result;
    }

    /**
     * query process definition list by process definition code
     *
     * @param projectCode           project code
     * @param processDefinitionCode process definition code
     * @return task definition list in the process definition
     */
    @Override
    public Map<String, Object> queryTaskDefinitionListByProcessDefinitionCode(long projectCode, Long processDefinitionCode) {
        Map<String, Object> result = new HashMap<>();

        Set<Long> definitionCodesSet = new HashSet<>();
        definitionCodesSet.add(processDefinitionCode);
        List<DependentSimplifyDefinition> processDefinitions = processDefinitionMapper.queryDefinitionListByProjectCodeAndProcessDefinitionCodes(projectCode, definitionCodesSet);

        // query task definition log
        List<TaskDefinitionLog> taskDefinitionLogsList = taskDefinitionLogDao.queryByWorkflowDefinitionCodeAndVersion(processDefinitions.get(0).getCode(), processDefinitions.get(0).getVersion());

        List<DependentSimplifyDefinition> taskDefinitionList = new ArrayList<>();
        for (TaskDefinitionLog taskDefinitionLog : taskDefinitionLogsList) {
            DependentSimplifyDefinition dependentSimplifyDefinition = new DependentSimplifyDefinition();
            dependentSimplifyDefinition.setCode(taskDefinitionLog.getCode());
            dependentSimplifyDefinition.setName(taskDefinitionLog.getName());
            dependentSimplifyDefinition.setVersion(taskDefinitionLog.getVersion());
            taskDefinitionList.add(dependentSimplifyDefinition);
        }

        result.put(DATA_LIST, taskDefinitionList);
        putMsg(result, Status.SUCCESS);
        return result;
    }

    /**
     * Encapsulates the TreeView structure
     *
     * @param projectCode project code
     * @param code        process definition code
     * @param limit       limit
     * @return tree view json data
     */
    @Override
    public Map<String, Object> viewTree(User loginUser, long projectCode, long code, Integer limit) {
        Map<String, Object> result = new HashMap<>();
        Project project = projectMapper.queryByCode(projectCode);
        // check user access for project
        result = projectService.checkProjectAndAuth(loginUser, project, projectCode, WORKFLOW_TREE_VIEW);
        if (result.get(STATUS) != Status.SUCCESS) {
            return result;
        }
        ProcessDefinition processDefinition = processDefinitionMapper.queryByCode(code);
        if (null == processDefinition || projectCode != processDefinition.getProjectCode()) {
            log.error("Process definition does not exist, code:{}.", code);
            putMsg(result, PROCESS_DEFINE_NOT_EXIST, String.valueOf(code));
            return result;
        }
        DAG<Long, TaskNode, TaskNodeRelation> dag = processService.genDagGraph(processDefinition);
        // nodes that are running
        Map<Long, List<TreeViewDto>> runningNodeMap = new ConcurrentHashMap<>();

        // nodes that are waiting to run
        Map<Long, List<TreeViewDto>> waitingRunningNodeMap = new ConcurrentHashMap<>();

        // List of process instances
        List<ProcessInstance> processInstanceList = processInstanceService.queryByProcessDefineCode(code, limit);
        processInstanceList.forEach(processInstance -> processInstance.setDuration(DateUtils.format2Duration(processInstance.getStartTime(), processInstance.getEndTime())));
        List<TaskDefinitionLog> taskDefinitionList = taskDefinitionLogDao.queryByWorkflowDefinitionCodeAndVersion(processDefinition.getCode(), processDefinition.getVersion());
        Map<Long, TaskDefinitionLog> taskDefinitionMap = taskDefinitionList.stream().collect(Collectors.toMap(TaskDefinitionLog::getCode, taskDefinitionLog -> taskDefinitionLog));

        if (limit < 0) {
            putMsg(result, Status.REQUEST_PARAMS_NOT_VALID_ERROR);
            return result;
        }
        if (limit > processInstanceList.size()) {
            limit = processInstanceList.size();
        }

        TreeViewDto parentTreeViewDto = new TreeViewDto();
        parentTreeViewDto.setName("DAG");
        parentTreeViewDto.setType("");
        parentTreeViewDto.setCode(0L);
        // Specify the process definition, because it is a TreeView for a process definition
        for (int i = limit - 1; i >= 0; i--) {
            ProcessInstance processInstance = processInstanceList.get(i);
            Date endTime = processInstance.getEndTime() == null ? new Date() : processInstance.getEndTime();
            parentTreeViewDto.getInstances().add(new Instance(processInstance.getId(), processInstance.getName(), processInstance.getProcessDefinitionCode(), "", processInstance.getState().name(), processInstance.getStartTime(), endTime, processInstance.getHost(), DateUtils.format2Readable(endTime.getTime() - processInstance.getStartTime().getTime())));
        }

        List<TreeViewDto> parentTreeViewDtoList = new ArrayList<>();
        parentTreeViewDtoList.add(parentTreeViewDto);
        // Here is the encapsulation task instance
        for (Long startNode : dag.getBeginNode()) {
            runningNodeMap.put(startNode, parentTreeViewDtoList);
        }

        while (!ServerLifeCycleManager.isStopped()) {
            Set<Long> postNodeList;
            Iterator<Map.Entry<Long, List<TreeViewDto>>> iter = runningNodeMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<Long, List<TreeViewDto>> en = iter.next();
                Long nodeCode = en.getKey();
                parentTreeViewDtoList = en.getValue();

                TreeViewDto treeViewDto = new TreeViewDto();
                TaskNode taskNode = dag.getNode(nodeCode);
                treeViewDto.setType(taskNode.getType());
                treeViewDto.setCode(taskNode.getCode());
                treeViewDto.setName(taskNode.getName());
                // set treeViewDto instances
                for (int i = limit - 1; i >= 0; i--) {
                    ProcessInstance processInstance = processInstanceList.get(i);
                    TaskInstance taskInstance = taskInstanceMapper.queryByInstanceIdAndCode(processInstance.getId(), nodeCode);
                    if (taskInstance == null) {
                        treeViewDto.getInstances().add(new Instance(-1, "not running", 0, "null"));
                    } else {
                        Date startTime = taskInstance.getStartTime() == null ? new Date() : taskInstance.getStartTime();
                        Date endTime = taskInstance.getEndTime() == null ? new Date() : taskInstance.getEndTime();

                        long subProcessCode = 0L;
                        // if process is sub process, the return sub id, or sub id=0
                        if (taskInstance.isSubProcess()) {
                            TaskDefinition taskDefinition = taskDefinitionMap.get(taskInstance.getTaskCode());
                            subProcessCode = Long.parseLong(JSONUtils.parseObject(taskDefinition.getTaskParams()).path(CMD_PARAM_SUB_PROCESS_DEFINE_CODE).asText());
                        }
                        treeViewDto.getInstances().add(new Instance(taskInstance.getId(), taskInstance.getName(), taskInstance.getTaskCode(), taskInstance.getTaskType(), taskInstance.getState().name(), taskInstance.getStartTime(), taskInstance.getEndTime(), taskInstance.getHost(), DateUtils.format2Readable(endTime.getTime() - startTime.getTime()), subProcessCode));
                    }
                }
                for (TreeViewDto pTreeViewDto : parentTreeViewDtoList) {
                    pTreeViewDto.getChildren().add(treeViewDto);
                }
                postNodeList = dag.getSubsequentNodes(nodeCode);
                if (CollectionUtils.isNotEmpty(postNodeList)) {
                    for (Long nextNodeCode : postNodeList) {
                        List<TreeViewDto> treeViewDtoList = waitingRunningNodeMap.get(nextNodeCode);
                        if (CollectionUtils.isEmpty(treeViewDtoList)) {
                            treeViewDtoList = new ArrayList<>();
                        }
                        treeViewDtoList.add(treeViewDto);
                        waitingRunningNodeMap.put(nextNodeCode, treeViewDtoList);
                    }
                }
                runningNodeMap.remove(nodeCode);
            }
            if (waitingRunningNodeMap.size() == 0) {
                break;
            } else {
                runningNodeMap.putAll(waitingRunningNodeMap);
                waitingRunningNodeMap.clear();
            }
        }
        result.put(DATA_LIST, parentTreeViewDto);
        result.put(STATUS, Status.SUCCESS);
        result.put(MSG, Status.SUCCESS.getMsg());
        return result;
    }

    /**
     * whether the graph has a ring
     *
     * @param taskNodeResponseList task node response list
     * @return if graph has cycle flag
     */
    private boolean graphHasCycle(List<TaskNode> taskNodeResponseList) {
        DAG<String, TaskNode, String> graph = new DAG<>();
        // Fill the vertices
        for (TaskNode taskNodeResponse : taskNodeResponseList) {
            graph.addNode(Long.toString(taskNodeResponse.getCode()), taskNodeResponse);
        }
        // Fill edge relations
        for (TaskNode taskNodeResponse : taskNodeResponseList) {
            List<String> preTasks = JSONUtils.toList(taskNodeResponse.getPreTasks(), String.class);
            if (CollectionUtils.isNotEmpty(preTasks)) {
                for (String preTask : preTasks) {
                    if (!graph.addEdge(preTask, Long.toString(taskNodeResponse.getCode()))) {
                        return true;
                    }
                }
            }
        }
        return graph.hasCycle();
    }

    /**
     * batch copy process definition
     *
     * @param loginUser         loginUser
     * @param projectCode       projectCode
     * @param codes             processDefinitionCodes
     * @param targetProjectCode targetProjectCode
     */
    @Override
    @Transactional
    public Map<String, Object> batchCopyProcessDefinition(User loginUser, long projectCode, String codes, long targetProjectCode) {
        Map<String, Object> result = checkParams(loginUser, projectCode, codes, targetProjectCode, WORKFLOW_BATCH_COPY);
        if (result.get(STATUS) != Status.SUCCESS) {
            return result;
        }
        List<String> failedProcessList = new ArrayList<>();
        doBatchOperateProcessDefinition(loginUser, targetProjectCode, failedProcessList, codes, result, true);
        checkBatchOperateResult(projectCode, targetProjectCode, result, failedProcessList, true);
        return result;
    }

    /**
     * batch move process definition
     * Will be deleted
     *
     * @param loginUser         loginUser
     * @param projectCode       projectCode
     * @param codes             processDefinitionCodes
     * @param targetProjectCode targetProjectCode
     */
    @Override
    @Transactional
    public Map<String, Object> batchMoveProcessDefinition(User loginUser, long projectCode, String codes, long targetProjectCode) {
        Map<String, Object> result = checkParams(loginUser, projectCode, codes, targetProjectCode, TASK_DEFINITION_MOVE);
        if (result.get(STATUS) != Status.SUCCESS) {
            return result;
        }
        if (projectCode == targetProjectCode) {
            log.warn("Project code is same as target project code, projectCode:{}.", projectCode);
            return result;
        }

        List<String> failedProcessList = new ArrayList<>();
        doBatchOperateProcessDefinition(loginUser, targetProjectCode, failedProcessList, codes, result, false);
        checkBatchOperateResult(projectCode, targetProjectCode, result, failedProcessList, false);
        return result;
    }

    private Map<String, Object> checkParams(User loginUser, long projectCode, String processDefinitionCodes, long targetProjectCode, String perm) {
        Project project = projectMapper.queryByCode(projectCode);
        // check user access for project
        Map<String, Object> result = projectService.checkProjectAndAuth(loginUser, project, projectCode, perm);
        if (result.get(STATUS) != Status.SUCCESS) {
            return result;
        }

        if (StringUtils.isEmpty(processDefinitionCodes)) {
            log.error("Parameter processDefinitionCodes is empty, projectCode is {}.", projectCode);
            putMsg(result, Status.PROCESS_DEFINITION_CODES_IS_EMPTY, processDefinitionCodes);
            return result;
        }

        if (projectCode != targetProjectCode) {
            Project targetProject = projectMapper.queryByCode(targetProjectCode);
            // check user access for project
            Map<String, Object> targetResult = projectService.checkProjectAndAuth(loginUser, targetProject, targetProjectCode, perm);
            if (targetResult.get(STATUS) != Status.SUCCESS) {
                return targetResult;
            }
        }
        return result;
    }

    protected void doBatchOperateProcessDefinition(User loginUser, long targetProjectCode, List<String> failedProcessList, String processDefinitionCodes, Map<String, Object> result, boolean isCopy) {
        Set<Long> definitionCodes = Arrays.stream(processDefinitionCodes.split(Constants.COMMA)).map(Long::parseLong).collect(toSet());
        List<ProcessDefinition> processDefinitionList = processDefinitionMapper.queryByCodes(definitionCodes);
        Set<Long> queryCodes = processDefinitionList.stream().map(ProcessDefinition::getCode).collect(toSet());
        // definitionCodes - queryCodes
        Set<Long> diffCode = definitionCodes.stream().filter(code -> !queryCodes.contains(code)).collect(toSet());
        diffCode.forEach(code -> failedProcessList.add(code + "[null]"));
        for (ProcessDefinition processDefinition : processDefinitionList) {
            List<ProcessTaskRelation> processTaskRelations = processTaskRelationMapper.queryByProcessCode(processDefinition.getCode());
            List<ProcessTaskRelationLog> taskRelationList = processTaskRelations.stream().map(ProcessTaskRelationLog::new).collect(Collectors.toList());
            processDefinition.setProjectCode(targetProjectCode);
            if (isCopy) {
                log.info("Copy process definition...");
                List<TaskDefinitionLog> taskDefinitionLogs = taskDefinitionLogDao.queryTaskDefineLogList(processTaskRelations);
                Map<Long, Long> taskCodeMap = new HashMap<>();
                for (TaskDefinitionLog taskDefinitionLog : taskDefinitionLogs) {
                    try {
                        long taskCode = CodeGenerateUtils.genCode();
                        taskCodeMap.put(taskDefinitionLog.getCode(), taskCode);
                        taskDefinitionLog.setCode(taskCode);
                    } catch (CodeGenerateException e) {
                        log.error("Generate task definition code error, projectCode:{}.", targetProjectCode, e);
                        putMsg(result, Status.INTERNAL_SERVER_ERROR_ARGS);
                        throw new ServiceException(Status.INTERNAL_SERVER_ERROR_ARGS);
                    }
                    taskDefinitionLog.setProjectCode(targetProjectCode);
                    taskDefinitionLog.setVersion(0);
                    taskDefinitionLog.setName(taskDefinitionLog.getName());
                }
                for (ProcessTaskRelationLog processTaskRelationLog : taskRelationList) {
                    if (processTaskRelationLog.getPreTaskCode() > 0) {
                        processTaskRelationLog.setPreTaskCode(taskCodeMap.get(processTaskRelationLog.getPreTaskCode()));
                    }
                    if (processTaskRelationLog.getPostTaskCode() > 0) {
                        processTaskRelationLog.setPostTaskCode(taskCodeMap.get(processTaskRelationLog.getPostTaskCode()));
                    }
                }
                final long oldProcessDefinitionCode = processDefinition.getCode();
                try {
                    processDefinition.setCode(CodeGenerateUtils.genCode());
                } catch (CodeGenerateException e) {
                    log.error("Generate process definition code error, projectCode:{}.", targetProjectCode, e);
                    putMsg(result, Status.INTERNAL_SERVER_ERROR_ARGS);
                    throw new ServiceException(Status.INTERNAL_SERVER_ERROR_ARGS);
                }
                processDefinition.setId(null);
                processDefinition.setUserId(loginUser.getId());
                processDefinition.setName(getNewName(processDefinition.getName(), COPY_SUFFIX));
                final Date date = new Date();
                processDefinition.setCreateTime(date);
                processDefinition.setUpdateTime(date);
                processDefinition.setReleaseState(ReleaseState.OFFLINE);
                if (StringUtils.isNotBlank(processDefinition.getLocations())) {
                    ArrayNode oldJsonNodes = JSONUtils.parseArray(processDefinition.getLocations());
                    ArrayNode newJsonNodes = JSONUtils.createArrayNode();
                    //处理seatunnel子算子的taskcode为null问题，原来location保留
                    for (int i = 0; i < oldJsonNodes.size(); i++) {
                        ObjectNode oldNode = (ObjectNode) oldJsonNodes.path(i);
                        newJsonNodes.add(oldNode);
                    }

                    ArrayNode jsonNodes = JSONUtils.parseArray(processDefinition.getLocations());
                    for (int i = 0; i < jsonNodes.size(); i++) {
                        ObjectNode node = (ObjectNode) jsonNodes.path(i);
                        node.put("taskCode", taskCodeMap.get(node.get("taskCode").asLong()));
//                        jsonNodes.set(i, node);
                        newJsonNodes.add(node);
                    }
                    processDefinition.setLocations(JSONUtils.toJsonString(newJsonNodes));
                }
                // copy timing configuration
                Schedule scheduleObj = scheduleMapper.queryByProcessDefinitionCode(oldProcessDefinitionCode);
                if (scheduleObj != null) {
                    scheduleObj.setId(null);
                    scheduleObj.setUserId(loginUser.getId());
                    scheduleObj.setProcessDefinitionCode(processDefinition.getCode());
                    scheduleObj.setReleaseState(ReleaseState.OFFLINE);
                    scheduleObj.setCreateTime(date);
                    scheduleObj.setUpdateTime(date);
                    int insertResult = scheduleMapper.insert(scheduleObj);
                    if (insertResult != 1) {
                        log.error("Schedule create error, processDefinitionCode:{}.", processDefinition.getCode());
                        putMsg(result, Status.CREATE_SCHEDULE_ERROR);
                        throw new ServiceException(Status.CREATE_SCHEDULE_ERROR);
                    }
                }
                try {
                    result.putAll(createDagDefine(loginUser, taskRelationList, processDefinition, taskDefinitionLogs));
                } catch (Exception e) {
                    log.error("Copy process definition error, processDefinitionCode from {} to {}.", oldProcessDefinitionCode, processDefinition.getCode(), e);
                    putMsg(result, Status.COPY_PROCESS_DEFINITION_ERROR);
                    throw new ServiceException(Status.COPY_PROCESS_DEFINITION_ERROR);
                }
            } else {
                log.info("Move process definition...");
                try {
                    result.putAll(updateDagDefine(loginUser, taskRelationList, processDefinition, null, Lists.newArrayList()));
                } catch (Exception e) {
                    log.error("Move process definition error, processDefinitionCode:{}.", processDefinition.getCode(), e);
                    putMsg(result, Status.MOVE_PROCESS_DEFINITION_ERROR);
                    throw new ServiceException(Status.MOVE_PROCESS_DEFINITION_ERROR);
                }
            }
            if (result.get(STATUS) != Status.SUCCESS) {
                failedProcessList.add(processDefinition.getCode() + "[" + processDefinition.getName() + "]");
            }
        }
    }

    /**
     * get new Task name or Process name when copy or import operate
     *
     * @param originalName Task or Process original name
     * @param suffix       "_copy_" or "_import_"
     * @return new name
     */
    public String getNewName(String originalName, String suffix) {
        StringBuilder newName = new StringBuilder();
        String regex = String.format(".*%s\\d{17}$", suffix);
        if (originalName.matches(regex)) {
            // replace timestamp of originalName
            return newName.append(originalName, 0, originalName.lastIndexOf(suffix)).append(suffix).append(DateUtils.getCurrentTimeStamp()).toString();
        }
        return newName.append(originalName).append(suffix).append(DateUtils.getCurrentTimeStamp()).toString();
    }

    /**
     * switch the defined process definition version
     *
     * @param loginUser   login user
     * @param projectCode project code
     * @param code        process definition code
     * @param version     the version user want to switch
     * @return switch process definition version result code
     */
    @Override
    @Transactional
    public Map<String, Object> switchProcessDefinitionVersion(User loginUser, long projectCode, long code, int version) {
        Project project = projectMapper.queryByCode(projectCode);
        // check user access for project
        Map<String, Object> result = projectService.checkProjectAndAuth(loginUser, project, projectCode, WORKFLOW_SWITCH_TO_THIS_VERSION);
        if (result.get(STATUS) != Status.SUCCESS) {
            return result;
        }

        ProcessDefinition processDefinition = processDefinitionMapper.queryByCode(code);
        if (Objects.isNull(processDefinition) || projectCode != processDefinition.getProjectCode()) {
            log.error("Switch process definition error because it does not exist, projectCode:{}, processDefinitionCode:{}.", projectCode, code);
            putMsg(result, Status.SWITCH_PROCESS_DEFINITION_VERSION_NOT_EXIST_PROCESS_DEFINITION_ERROR, code);
            return result;
        }

        ProcessDefinitionLog processDefinitionLog = processDefinitionLogMapper.queryByDefinitionCodeAndVersion(code, version);
        if (Objects.isNull(processDefinitionLog)) {
            log.error("Switch process definition error because version does not exist, projectCode:{}, processDefinitionCode:{}, version:{}.", projectCode, code, version);
            putMsg(result, Status.SWITCH_PROCESS_DEFINITION_VERSION_NOT_EXIST_PROCESS_DEFINITION_VERSION_ERROR, processDefinition.getCode(), version);
            return result;
        }
        int switchVersion = processService.switchVersion(processDefinition, processDefinitionLog);
        if (switchVersion <= 0) {
            log.error("Switch process definition version error, projectCode:{}, processDefinitionCode:{}, version:{}.", projectCode, code, version);
            putMsg(result, Status.SWITCH_PROCESS_DEFINITION_VERSION_ERROR);
            throw new ServiceException(Status.SWITCH_PROCESS_DEFINITION_VERSION_ERROR);
        }
        log.info("Switch process definition version complete, projectCode:{}, processDefinitionCode:{}, version:{}.", projectCode, code, version);
        putMsg(result, Status.SUCCESS);
        return result;
    }

    /**
     * check batch operate result
     *
     * @param srcProjectCode    srcProjectCode
     * @param targetProjectCode targetProjectCode
     * @param result            result
     * @param failedProcessList failedProcessList
     * @param isCopy            isCopy
     */
    private void checkBatchOperateResult(long srcProjectCode, long targetProjectCode, Map<String, Object> result, List<String> failedProcessList, boolean isCopy) {
        if (!failedProcessList.isEmpty()) {
            String failedProcess = String.join(",", failedProcessList);
            if (isCopy) {
                log.error("Copy process definition error, srcProjectCode:{}, targetProjectCode:{}, failedProcessList:{}.", srcProjectCode, targetProjectCode, failedProcess);
                putMsg(result, Status.COPY_PROCESS_DEFINITION_ERROR, srcProjectCode, targetProjectCode, failedProcess);
            } else {
                log.error("Move process definition error, srcProjectCode:{}, targetProjectCode:{}, failedProcessList:{}.", srcProjectCode, targetProjectCode, failedProcess);
                putMsg(result, Status.MOVE_PROCESS_DEFINITION_ERROR, srcProjectCode, targetProjectCode, failedProcess);
            }
        } else {
            log.info("Batch {} process definition complete, srcProjectCode:{}, targetProjectCode:{}.", isCopy ? "copy" : "move", srcProjectCode, targetProjectCode);
            putMsg(result, Status.SUCCESS);
        }
    }

    /**
     * query the pagination versions info by one certain process definition code
     *
     * @param loginUser   login user info to check auth
     * @param projectCode project code
     * @param pageNo      page number
     * @param pageSize    page size
     * @param code        process definition code
     * @return the pagination process definition versions info of the certain process definition
     */
    @Override
    public Result queryProcessDefinitionVersions(User loginUser, long projectCode, int pageNo, int pageSize, long code) {
        Result result = new Result();
        Project project = projectMapper.queryByCode(projectCode);
        // check user access for project
        Map<String, Object> checkResult = projectService.checkProjectAndAuth(loginUser, project, projectCode, VERSION_LIST);
        Status resultStatus = (Status) checkResult.get(STATUS);
        if (resultStatus != Status.SUCCESS) {
            putMsg(result, resultStatus);
            return result;
        }
        PageInfo<ProcessDefinitionLog> pageInfo = new PageInfo<>(pageNo, pageSize);
        Page<ProcessDefinitionLog> page = new Page<>(pageNo, pageSize);
        IPage<ProcessDefinitionLog> processDefinitionVersionsPaging = processDefinitionLogMapper.queryProcessDefinitionVersionsPaging(page, code, projectCode);
        List<ProcessDefinitionLog> processDefinitionLogs = processDefinitionVersionsPaging.getRecords();

        pageInfo.setTotalList(processDefinitionLogs);
        pageInfo.setTotal((int) processDefinitionVersionsPaging.getTotal());
        result.setData(pageInfo);
        putMsg(result, Status.SUCCESS);
        return result;
    }

    /**
     * delete one certain process definition by version number and process definition code
     *
     * @param loginUser   login user info to check auth
     * @param projectCode project code
     * @param code        process definition code
     * @param version     version number
     */
    @Override
    @Transactional
    public void deleteProcessDefinitionVersion(User loginUser, long projectCode, long code, int version) {
        projectService.checkHasProjectWritePermissionThrowException(loginUser, projectCode);

        ProcessDefinition processDefinition = processDefinitionMapper.queryByCode(code);
        if (processDefinition == null || projectCode != processDefinition.getProjectCode()) {
            throw new ServiceException(PROCESS_DEFINE_NOT_EXIST, code);
        }
        if (processDefinition.getVersion() == version) {
            log.warn("This version: {} of workflow: {} is the main version cannot delete by version", code, version);
            throw new ServiceException(Status.MAIN_TABLE_USING_VERSION);
        }
        // check whether there exist running workflow instance under the process definition
        List<ProcessInstance> workflowInstances = processInstanceService.queryByWorkflowCodeVersionStatus(code, version, WorkflowExecutionStatus.getNotTerminalStatus());
        if (CollectionUtils.isNotEmpty(workflowInstances)) {
            throw new ServiceException(Status.DELETE_PROCESS_DEFINITION_EXECUTING_FAIL, workflowInstances.size());
        }

        int deleteLog = processDefinitionLogMapper.deleteByProcessDefinitionCodeAndVersion(code, version);
        int deleteRelationLog = processTaskRelationLogMapper.deleteByCode(code, version);
        if (deleteLog == 0 || deleteRelationLog == 0) {
            throw new ServiceException(Status.DELETE_PROCESS_DEFINE_BY_CODE_ERROR);
        }
        log.info("Delete version: {} of workflow: {}, projectCode:{}.", version, code, projectCode);
    }

    private void updateWorkflowValid(User user, ProcessDefinition oldProcessDefinition, ProcessDefinition newProcessDefinition) {
        // online can not permit edit
        if (oldProcessDefinition.getReleaseState() == ReleaseState.ONLINE) {
            throw new ServiceException(Status.PROCESS_DEFINE_NOT_ALLOWED_EDIT, oldProcessDefinition.getName());
        }

        Project project = projectMapper.queryByCode(oldProcessDefinition.getProjectCode());
        // check user access for project
        projectService.checkProjectAndAuthThrowException(user, project, WORKFLOW_UPDATE);

        if (checkDescriptionLength(newProcessDefinition.getDescription())) {
            throw new ServiceException(Status.DESCRIPTION_TOO_LONG_ERROR);
        }

        // check whether the new process define name exist
        if (!oldProcessDefinition.getName().equals(newProcessDefinition.getName())) {
            ProcessDefinition definition = processDefinitionMapper.verifyByDefineName(newProcessDefinition.getProjectCode(), newProcessDefinition.getName());
            if (definition != null) {
                throw new ServiceException(Status.PROCESS_DEFINITION_NAME_EXIST, newProcessDefinition.getName());
            }
        }
    }

    /**
     * update single resource workflow
     *
     * @param loginUser             login user
     * @param workflowCode          workflow resource code want to update
     * @param workflowUpdateRequest workflow update resource object
     * @return Process definition
     */
    @Override
    @Transactional
    public ProcessDefinition updateSingleProcessDefinition(User loginUser, long workflowCode, WorkflowUpdateRequest workflowUpdateRequest) {
        ProcessDefinition processDefinition = processDefinitionMapper.queryByCode(workflowCode);
        // check process definition exists
        if (processDefinition == null) {
            throw new ServiceException(PROCESS_DEFINE_NOT_EXIST, workflowCode);
        }

        ProcessDefinition processDefinitionUpdate = workflowUpdateRequest.mergeIntoProcessDefinition(processDefinition);
        this.updateWorkflowValid(loginUser, processDefinition, processDefinitionUpdate);

        int insertVersion = this.saveProcessDefine(loginUser, processDefinitionUpdate);
        if (insertVersion == 0) {
            log.error("Update process definition error, projectCode:{}, processDefinitionName:{}.", processDefinitionUpdate.getCode(), processDefinitionUpdate.getName());
            throw new ServiceException(Status.UPDATE_PROCESS_DEFINITION_ERROR);
        }

        int insertRelationVersion = this.saveTaskRelation(loginUser, processDefinitionUpdate, insertVersion);
        if (insertRelationVersion != Constants.EXIT_CODE_SUCCESS) {
            log.error("Save process task relations error, projectCode:{}, processCode:{}, processVersion:{}.", processDefinition.getProjectCode(), processDefinition.getCode(), insertVersion);
            throw new ServiceException(Status.CREATE_PROCESS_TASK_RELATION_ERROR);
        }
        log.info("Save process task relations complete, projectCode:{}, processCode:{}, processVersion:{}.", processDefinition.getProjectCode(), processDefinition.getCode(), insertVersion);
        processDefinitionUpdate.setVersion(insertVersion);
        return processDefinitionUpdate;
    }

    public int saveProcessDefine(User loginUser, ProcessDefinition processDefinition) {
        ProcessDefinitionLog processDefinitionLog = new ProcessDefinitionLog(processDefinition);
        Integer version = processDefinitionLogMapper.queryMaxVersionForDefinition(processDefinition.getCode());
        int insertVersion = version == null || version == 0 ? VERSION_FIRST : version + 1;
        processDefinitionLog.setVersion(insertVersion);
        processDefinition.setVersion(insertVersion);

        processDefinitionLog.setOperator(loginUser.getId());
        processDefinition.setUserId(loginUser.getId());
        processDefinitionLog.setOperateTime(processDefinition.getUpdateTime());
        processDefinition.setUpdateTime(processDefinition.getUpdateTime());
        processDefinitionLog.setId(null);
        int result = processDefinitionMapper.updateById(processDefinition);

        int insertLog = processDefinitionLogMapper.insert(processDefinitionLog);
        processDefinitionLog.setId(processDefinition.getId());
        return (insertLog & result) > 0 ? insertVersion : 0;
    }

    public int saveTaskRelation(User loginUser, ProcessDefinition processDefinition, int processDefinitionVersion) {
        long projectCode = processDefinition.getProjectCode();
        long processDefinitionCode = processDefinition.getCode();
        List<ProcessTaskRelation> taskRelations = processTaskRelationMapper.queryByProcessCode(processDefinitionCode);
        List<ProcessTaskRelationLog> taskRelationList = taskRelations.stream().map(ProcessTaskRelationLog::new).collect(Collectors.toList());

        List<Long> taskCodeList = taskRelations.stream().map(ProcessTaskRelation::getPostTaskCode).collect(Collectors.toList());
        List<TaskDefinition> taskDefinitions = taskDefinitionMapper.queryByCodeList(taskCodeList);
        List<TaskDefinitionLog> taskDefinitionLogs = taskDefinitions.stream().map(TaskDefinitionLog::new).collect(Collectors.toList());

        if (taskRelationList.isEmpty()) {
            return Constants.EXIT_CODE_SUCCESS;
        }
        Map<Long, TaskDefinitionLog> taskDefinitionLogMap = null;
        if (CollectionUtils.isNotEmpty(taskDefinitionLogs)) {
            taskDefinitionLogMap = taskDefinitionLogs.stream().collect(Collectors.toMap(TaskDefinition::getCode, taskDefinitionLog -> taskDefinitionLog));
        }
        Date now = new Date();
        for (ProcessTaskRelationLog processTaskRelationLog : taskRelationList) {
            processTaskRelationLog.setProjectCode(projectCode);
            processTaskRelationLog.setProcessDefinitionCode(processDefinitionCode);
            processTaskRelationLog.setProcessDefinitionVersion(processDefinitionVersion);
            if (taskDefinitionLogMap != null) {
                TaskDefinitionLog preTaskDefinitionLog = taskDefinitionLogMap.get(processTaskRelationLog.getPreTaskCode());
                if (preTaskDefinitionLog != null) {
                    processTaskRelationLog.setPreTaskVersion(preTaskDefinitionLog.getVersion());
                }
                TaskDefinitionLog postTaskDefinitionLog = taskDefinitionLogMap.get(processTaskRelationLog.getPostTaskCode());
                if (postTaskDefinitionLog != null) {
                    processTaskRelationLog.setPostTaskVersion(postTaskDefinitionLog.getVersion());
                }
            }
            processTaskRelationLog.setCreateTime(now);
            processTaskRelationLog.setUpdateTime(now);
            processTaskRelationLog.setOperator(loginUser.getId());
            processTaskRelationLog.setOperateTime(now);
        }
        if (!taskRelations.isEmpty()) {
            Set<Integer> processTaskRelationSet = taskRelations.stream().map(ProcessTaskRelation::hashCode).collect(toSet());
            Set<Integer> taskRelationSet = taskRelationList.stream().map(ProcessTaskRelationLog::hashCode).collect(toSet());
            boolean isSame = CollectionUtils.isEqualCollection(processTaskRelationSet, taskRelationSet);
            if (isSame) {
                log.info("process task relations is non-existent, projectCode:{}, processCode:{}.", processDefinition.getProjectCode(), processDefinition.getCode());
                return Constants.EXIT_CODE_SUCCESS;
            }
            processTaskRelationMapper.deleteByCode(projectCode, processDefinitionCode);
        }
        List<ProcessTaskRelation> processTaskRelations = taskRelationList.stream().map(ProcessTaskRelation::new).collect(Collectors.toList());
        int insert = processTaskRelationMapper.batchInsert(processTaskRelations);
        int resultLog = processTaskRelationLogMapper.batchInsert(taskRelationList);
        return (insert & resultLog) > 0 ? Constants.EXIT_CODE_SUCCESS : Constants.EXIT_CODE_FAILURE;
    }

    @Transactional
    @Override
    public void onlineWorkflowDefinition(User loginUser, Long projectCode, Long workflowDefinitionCode) {
        projectService.checkProjectAndAuthThrowException(loginUser, projectCode, WORKFLOW_ONLINE_OFFLINE);

        ProcessDefinition workflowDefinition = processDefinitionDao.queryByCode(workflowDefinitionCode).orElseThrow(() -> new ServiceException(PROCESS_DEFINE_NOT_EXIST, workflowDefinitionCode));

        if (ReleaseState.ONLINE.equals(workflowDefinition.getReleaseState())) {
            // do nothing if the workflow is already online
            return;
        }

        checkWorkflowDefinitionIsValidated(workflowDefinition.getCode());
        checkAllSubWorkflowDefinitionIsOnline(workflowDefinition.getCode());

        workflowDefinition.setReleaseState(ReleaseState.ONLINE);
        processDefinitionDao.updateById(workflowDefinition);
    }

    @Transactional
    @Override
    public void offlineWorkflowDefinition(User loginUser, Long projectCode, Long workflowDefinitionCode) {
        projectService.checkProjectAndAuthThrowException(loginUser, projectCode, WORKFLOW_ONLINE_OFFLINE);

        ProcessDefinition workflowDefinition = processDefinitionDao.queryByCode(workflowDefinitionCode).orElseThrow(() -> new ServiceException(PROCESS_DEFINE_NOT_EXIST, workflowDefinitionCode));

        if (ReleaseState.OFFLINE.equals(workflowDefinition.getReleaseState())) {
            // do nothing if the workflow is already offline
            return;
        }

        workflowDefinition.setReleaseState(ReleaseState.OFFLINE);
        processDefinitionDao.updateById(workflowDefinition);

        schedulerService.offlineSchedulerByWorkflowCode(workflowDefinitionCode);
    }

    /**
     * view process variables
     *
     * @param loginUser   login user
     * @param projectCode project code
     * @param code        process definition code
     * @return variables data
     */
    @Override
    public Map<String, Object> viewVariables(User loginUser, long projectCode, long code) {

        Project project = projectMapper.queryByCode(projectCode);

        // check user access for project
        Map<String, Object> result = projectService.checkProjectAndAuth(loginUser, project, projectCode, WORKFLOW_DEFINITION);
        if (result.get(STATUS) != Status.SUCCESS) {
            return result;
        }

        ProcessDefinition processDefinition = processDefinitionMapper.queryByCode(code);

        if (Objects.isNull(processDefinition) || projectCode != processDefinition.getProjectCode()) {
            log.error("Process definition does not exist, projectCode:{}, processDefinitionCode:{}.", projectCode, code);
            putMsg(result, PROCESS_DEFINE_NOT_EXIST, code);
            return result;
        }

        // global params
        List<Property> globalParams = processDefinition.getGlobalParamList();

        Map<String, Map<String, Object>> localUserDefParams = getLocalParams(processDefinition);

        Map<String, Object> resultMap = new HashMap<>();

        if (Objects.nonNull(globalParams)) {
            resultMap.put(GLOBAL_PARAMS, globalParams);
        }

        if (Objects.nonNull(localUserDefParams)) {
            resultMap.put(LOCAL_PARAMS, localUserDefParams);
        }

        result.put(DATA_LIST, resultMap);
        putMsg(result, Status.SUCCESS);
        return result;
    }

    /**
     * get local params
     */
    private Map<String, Map<String, Object>> getLocalParams(ProcessDefinition processDefinition) {
        Map<String, Map<String, Object>> localUserDefParams = new HashMap<>();

        Set<Long> taskCodeSet = new TreeSet<>();

        processTaskRelationMapper.queryByProcessCode(processDefinition.getCode()).forEach(processTaskRelation -> {
            if (processTaskRelation.getPreTaskCode() > 0) {
                taskCodeSet.add(processTaskRelation.getPreTaskCode());
            }
            if (processTaskRelation.getPostTaskCode() > 0) {
                taskCodeSet.add(processTaskRelation.getPostTaskCode());
            }
        });

        taskDefinitionMapper.queryByCodeList(taskCodeSet).stream().forEach(taskDefinition -> {
            Map<String, Object> localParamsMap = new HashMap<>();
            String localParams = JSONUtils.getNodeString(taskDefinition.getTaskParams(), LOCAL_PARAMS);
            if (!StringUtils.isEmpty(localParams)) {
                List<Property> localParamsList = JSONUtils.toList(localParams, Property.class);
                localParamsMap.put(TASK_TYPE, taskDefinition.getTaskType());
                localParamsMap.put(LOCAL_PARAMS_LIST, localParamsList);
                if (CollectionUtils.isNotEmpty(localParamsList)) {
                    localUserDefParams.put(taskDefinition.getName(), localParamsMap);
                }
            }
        });

        return localUserDefParams;
    }

    private void checkWorkflowDefinitionIsValidated(Long workflowDefinitionCode) {
        // todo: build dag check if the dag is validated
        List<ProcessTaskRelation> processTaskRelations = processTaskRelationMapper.queryByProcessCode(workflowDefinitionCode);
        if (CollectionUtils.isEmpty(processTaskRelations)) {
            throw new ServiceException(Status.PROCESS_DAG_IS_EMPTY);
        }
        // todo : check Workflow is validate
    }

    private void checkAllSubWorkflowDefinitionIsOnline(Long workflowDefinitionCode) {
        List<Long> allSubWorkflowDefinitionCodes = processService.findAllSubWorkflowDefinitionCode(workflowDefinitionCode);
        if (CollectionUtils.isEmpty(allSubWorkflowDefinitionCodes)) {
            return;
        }
        for (Long subWorkflowDefinitionCode : allSubWorkflowDefinitionCodes) {
            ProcessDefinition subWorkflowDefinition = processDefinitionDao.queryByCode(subWorkflowDefinitionCode).orElseThrow(() -> new ServiceException(PROCESS_DEFINE_NOT_EXIST, workflowDefinitionCode));
            if (!ReleaseState.ONLINE.equals(subWorkflowDefinition.getReleaseState())) {
                throw new ServiceException("SubWorkflowDefinition " + subWorkflowDefinition.getName() + " is not online");
            }
        }
    }
}
