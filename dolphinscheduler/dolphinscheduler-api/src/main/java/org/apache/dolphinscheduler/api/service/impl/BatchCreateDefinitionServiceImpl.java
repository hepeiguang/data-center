package org.apache.dolphinscheduler.api.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.api.dto.TableCondition;
import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.exceptions.ServiceException;
import org.apache.dolphinscheduler.api.service.BatchCreateDefinitionService;
import org.apache.dolphinscheduler.api.service.DataSourceService;
import org.apache.dolphinscheduler.api.service.ProcessDefinitionService;
import org.apache.dolphinscheduler.common.enums.*;
import org.apache.dolphinscheduler.common.utils.CodeGenerateUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.dao.entity.*;
import org.apache.dolphinscheduler.dao.utils.WorkerGroupUtils;
import org.apache.dolphinscheduler.plugin.datasource.api.utils.DataSourceUtils;
import org.apache.dolphinscheduler.plugin.task.api.enums.DataType;
import org.apache.dolphinscheduler.plugin.task.api.enums.Direct;
import org.apache.dolphinscheduler.plugin.task.api.enums.TaskTimeoutStrategy;
import org.apache.dolphinscheduler.plugin.task.api.model.Property;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.spi.datasource.BaseConnectionParam;
import org.apache.dolphinscheduler.spi.enums.DbType;
import org.apache.dolphinscheduler.spi.params.base.ParamsOptions;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;
import static org.apache.dolphinscheduler.common.constants.Constants.DATA_LIST;
import static org.apache.dolphinscheduler.common.constants.Constants.STATUS;

@Service
@Slf4j
public class BatchCreateDefinitionServiceImpl implements BatchCreateDefinitionService {
    private static Map<String, List<Map<String, Object>>> tableColumns = new HashMap<>();

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private ProcessDefinitionService processDefinitionService;


    @Override
    public Map<String, Object> createProcessDefinitionByDatasourceTables(User loginUser, long projectCode, String name,
                                                                         Integer sourceDatasourceId,
                                                                         Integer targetDatasourceId,
                                                                         int serialTaskNum, String targetTablePrefix,
                                                                         String etlTechnology, String deployMode,
                                                                         String filterType, String filterContentJson,
                                                                         boolean enableIncrement,
                                                                         boolean isSplitProcess,
                                                                         String incrementFieldJson,
                                                                         String incrementParam,
                                                                         String primaryKeyJson,
                                                                         String sqlTaskContent, String globalParams,
                                                                         String preCustomSql,
                                                                         String dataSaveModel,
                                                                         String tableUniqueKeyJson) {
        // 1. 验证源数据源和目标数据源是否存在
        DataSource sourceDataSource = dataSourceService.getDataSourceById(sourceDatasourceId);
        if (sourceDataSource == null) {
            throw new ServiceException(Status.DATASOURCE_NOT_EXIST);
        }

        DataSource targetDataSource = dataSourceService.getDataSourceById(targetDatasourceId);
        if (targetDataSource == null) {
            throw new ServiceException(Status.DATASOURCE_NOT_EXIST);
        }

        BaseConnectionParam sourceConnectionParam = (BaseConnectionParam) DataSourceUtils.buildConnectionParams(sourceDataSource.getType(), sourceDataSource.getConnectionParams());
        String sourceDatabase = sourceConnectionParam.getDatabase();
        String sourceSchema = sourceConnectionParam.getOther().get("schema");
        if (StringUtils.isEmpty(sourceSchema)) {
            sourceSchema = sourceConnectionParam.getOther().get("currentSchema");
        }
        if (StringUtils.isEmpty(sourceSchema) && DbType.SQLSERVER.equals(sourceDataSource.getType())) {
            sourceSchema = "dbo";
        }
        // 2. 获取源数据源的所有表
        List<ParamsOptions> tables = dataSourceService.getTables(sourceDatasourceId, sourceDatabase);
        if (tables == null || tables.isEmpty()) {
            throw new ServiceException(Status.TABLE_NOT_FOUND);
        }
        List<String> incrementFields = new ArrayList<>();
        if (enableIncrement) {
            if (incrementFieldJson == null || incrementFieldJson.isEmpty()) {
                throw new ServiceException(Status.INCREMENT_FIELD_EMPTY);
            }
            if (StringUtils.isEmpty(incrementParam)) {
                throw new ServiceException(Status.INCREMENT_PARAM_EMPTY);
            }
            incrementFields = JSONUtils.toList(incrementFieldJson, String.class);
        }

        List<String> tableNames = tables.stream().map(v -> v.getValue().toString()).collect(Collectors.toList());
        List<String> filterTableNames = new ArrayList<>();
        List<String> regexps = JSONUtils.toList(filterContentJson, String.class);
        String description = String.format("数据源%s中的表的工作流", sourceDatabase);
        Map<String, String> tableUniqueKeys = JSONUtils.toMap(tableUniqueKeyJson);
        if ("all".equalsIgnoreCase(filterType)) {
            filterTableNames = tableNames;
        } else if ("include".equalsIgnoreCase(filterType)) {
            if (regexps != null && !regexps.isEmpty()) {
                for (String regex : regexps) {
                    filterTableNames.addAll(tableNames.stream().filter(v -> v.matches(regex)).collect(Collectors.toList()));
                }
            }
            description = description + String.format("包含表的规则个数:%s", regexps == null ? 0 : regexps.size());
        } else if ("exclude".equalsIgnoreCase(filterType)) {
            Set<String> excludedTables = new HashSet<>();
            if (regexps != null && !regexps.isEmpty()) {
                for (String regex : regexps) {
                    excludedTables.addAll(tableNames.stream().filter(v -> v.matches(regex)).collect(toSet()));
                }
            }
            filterTableNames = tableNames.stream().filter(t -> !excludedTables.contains(t)).collect(Collectors.toList());
            description = description + String.format(",不包含表的规则个数:%s", regexps == null ? 0 : regexps.size());
        }
//        else if ("notemptytable".equalsIgnoreCase(filterType)) {
//            // TODO 待完善动态根据数据类型获取表
//            List<String> includeTableNames = JSONUtils.toList(filterContentJson, String.class);
//            filterTableNames = SqlServerUtils.getNotEmptyTableList(sourceConnectionParam, includeTableNames);
//            tableUniqueKeys = SqlServerUtils.getTableUniqueKeyMapWithNotEmptyTables(sourceConnectionParam, filterTableNames);
//        }

        if (StringUtils.isEmpty(name)) {
            name = String.format("%s数据库数据同步工作流", sourceDatabase);
        }

        if (StringUtils.isEmpty(targetTablePrefix)) {
            targetTablePrefix = "ods_".concat(sourceDatabase);
        } else if (targetTablePrefix.endsWith("_")) {
            targetTablePrefix = targetTablePrefix.substring(0, targetTablePrefix.length() - 1);
        }
        List<String> commonPrimaryKeys = new ArrayList<>();
        if (StringUtils.isNotEmpty(primaryKeyJson)) {
            commonPrimaryKeys = JSONUtils.toList(primaryKeyJson, String.class);
        }
        List<TableCondition> incrementEtlTables = new ArrayList<>();
        List<TableCondition> fullEtlTables = new ArrayList<>();
        classifyTables(enableIncrement, sourceDataSource, sourceDatabase, sourceSchema, filterTableNames, incrementFields,
                incrementEtlTables, fullEtlTables, incrementParam, tableUniqueKeys, commonPrimaryKeys, isSplitProcess);

        Map<String, Object> result = new HashMap<>();
        result.put(STATUS, Status.SUCCESS);
        Map<String, Object> data = new HashMap<>();
        result.put(DATA_LIST, data);
        if (!incrementEtlTables.isEmpty()) {
            String incrementDesc = "增量同步，" + description + String.format("，共计%s个表。", incrementEtlTables.size());
            Long incrementTaskCode = generateTaskDefinition(loginUser, projectCode, "增量-" + name, serialTaskNum,
                    targetTablePrefix, etlTechnology, deployMode, incrementEtlTables, targetDataSource,
                    globalParams, incrementDesc, sqlTaskContent, preCustomSql, dataSaveModel);
            data.put("incrementTaskCode", incrementTaskCode);
        }
        if (!fullEtlTables.isEmpty()) {
            String fullDesc = (isSplitProcess ? "全量同步，" : "") + description + String.format("，共计%s个表。", fullEtlTables.size());
            Long fullTaskCode = generateTaskDefinition(loginUser, projectCode, (isSplitProcess ? "全量-" : "") + name, serialTaskNum,
                    targetTablePrefix, etlTechnology, deployMode, fullEtlTables, targetDataSource,
                    globalParams, fullDesc, sqlTaskContent, preCustomSql, dataSaveModel);
            data.put("fullTaskCode", fullTaskCode);
        }
        return result;
    }


    private Long generateTaskDefinition(User loginUser, long projectCode, String name, int serialTaskNum,
                                        String targetTablePrefix, String etlTechnology, String deployMode,
                                        List<TableCondition> etlTables, DataSource targetDataSource,
                                        String globalParams, String description, String sqlTaskContent,
                                        String preCustomSql, String dataSaveModel) {
        // 3. 创建任务定义、关系和位置
        List<TaskDefinitionLog> taskDefinitions = new ArrayList<>();
        List<Map<String, Object>> taskRelationList = new ArrayList<>();
        List<Map<String, Object>> locationsList = new ArrayList<>();

        int x = 50;
        int y = 50;
        long preTaskCode = 0L;
        int preTaskVersion = 0;
        int serialTaskIndex = 0;
        for (TableCondition table : etlTables) {
            TaskDefinitionLog taskDefinition = new TaskDefinitionLog();
            taskDefinitions.add(taskDefinition);
            // 生成任务代码
            long sourceTaskCode = CodeGenerateUtils.genCode();
            long sqlTransformTaskCode = CodeGenerateUtils.genCode();
            long sinkTaskCode = CodeGenerateUtils.genCode();
            long taskCode = CodeGenerateUtils.genCode();
            String etlTechnologyUpper = etlTechnology.toUpperCase(Locale.ROOT);
            taskDefinition.setCode(taskCode);
            taskDefinition.setTaskType(etlTechnologyUpper);
            taskDefinition.setUserId(loginUser.getId());
            taskDefinition.setProjectCode(projectCode);
            taskDefinition.setOperator(loginUser.getId());
            // 任务位置
            if (StringUtils.isNotEmpty(sqlTaskContent)) {
                locationsList.add(createTaskLocation(taskCode, x, y, 280, 260));
            } else {
                locationsList.add(createTaskLocation(taskCode, x, y, 280, 170));
            }
            // 任务关系
            taskRelationList.add(createTaskRelation(preTaskCode, taskCode, preTaskVersion));
            switch (etlTechnologyUpper) {
                case "SEATUNNEL":
                    processSeatunnelTaskDefinition(taskDefinition, table, targetDataSource, targetTablePrefix,
                            deployMode, sourceTaskCode, sinkTaskCode, sqlTransformTaskCode, sqlTaskContent, preCustomSql,
                            dataSaveModel);
                    int taskX = x + 20;
                    int taskY = y + 40;
                    // seatunnel source子节点位置
                    locationsList.add(createTaskLocation(sourceTaskCode, taskX, taskY, 220, 50));
                    if (StringUtils.isNotEmpty(sqlTaskContent)) {
                        // seatunnel sql transform子节点位置
                        locationsList.add(createTaskLocation(sqlTransformTaskCode, taskX, taskY += 70, 220, 50));
                    }
                    // seatunnel sink子节点位置
                    locationsList.add(createTaskLocation(sinkTaskCode, taskX, taskY + 70, 220, 50));
                    break;
                case "DATAX":
                    break;
            }
            preTaskCode = taskCode;
            if (++serialTaskIndex >= serialTaskNum) {
                x = 50;
                y += 250;
                if (StringUtils.isNotEmpty(sqlTaskContent)) {
                    y += 90;
                }
                preTaskVersion = 0;
                preTaskCode = 0L;
                serialTaskIndex = 0;
            } else {
                // 更新下一组任务的位置
                x += 320;
                preTaskVersion = 1;
            }
        }

        // 5. 转换为JSON
        String taskDefinitionJson = JSONUtils.toJsonString(taskDefinitions);
        String taskRelationJson = JSONUtils.toJsonString(taskRelationList);
        String locations = JSONUtils.toJsonString(locationsList);

        // 6. 创建工作流定义
        Map<String, Object> res = processDefinitionService.createProcessDefinition(loginUser, projectCode, name, description, globalParams,
                locations, 0, taskRelationJson, taskDefinitionJson, null, ProcessExecutionTypeEnum.PARALLEL);
        if (!res.get("status").equals(Status.SUCCESS)) {
            throw new RuntimeException("创建工作流定义失败, " + res.get("msg"));
        }
        ProcessDefinition processDefinition = (ProcessDefinition) res.get("data");
        return processDefinition.getCode();
    }

    private void processSeatunnelTaskDefinition(TaskDefinitionLog taskDefinition, TableCondition table,
                                                DataSource targetDataSource, String targetTablePrefix,
                                                String deployMode, long sourceTaskCode, long sinkTaskCode,
                                                long sqlTransformTaskCode, String sqlTaskContent,
                                                String preCustomSql, String dataSaveModel) {

        String targetTableName = StringUtils.isEmpty(targetTablePrefix) ? table.getTableName() : targetTablePrefix.concat("_").concat(table.getTableName());
        targetTableName = targetTableName.toLowerCase(Locale.ROOT);
        taskDefinition.setName(targetTableName);
        taskDefinition.setVersion(1);
        taskDefinition.setDescription(String.format("同步%s数据到表%s中", table.getTableName(), taskDefinition.getName()));
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
        taskDefinition.setTaskParams(getSeatunnelTaskParams(table, targetDataSource, targetTablePrefix, deployMode,
                sourceTaskCode, sinkTaskCode, sqlTransformTaskCode, sqlTaskContent, targetTableName, preCustomSql, dataSaveModel));
    }

    public String getSeatunnelTaskParams(TableCondition table, DataSource targetDataSource, String targetTablePrefix,
                                         String deployMode, long sourceTaskCode, long sinkTaskCode,
                                         long sqlTransformTaskCode, String sqlTaskContent, String targetTableName,
                                         String preCustomSql, String dataSaveModel) {
        Map<String, Object> taskParams = new HashMap<>();
        List<Property> properties = new ArrayList<>();
        List<ProcessTaskRelation> relations = new ArrayList<>();
        relations.add(new ProcessTaskRelation(0L, sourceTaskCode));
        if (StringUtils.isNotEmpty(sqlTaskContent)) {
            relations.add(new ProcessTaskRelation(sourceTaskCode, sqlTransformTaskCode));
            relations.add(new ProcessTaskRelation(sqlTransformTaskCode, sinkTaskCode));
        } else {
            relations.add(new ProcessTaskRelation(sourceTaskCode, sinkTaskCode));
        }
        properties.add(new Property("parallelism", Direct.IN, DataType.VARCHAR, "2"));
        properties.add(new Property("job.mode", Direct.IN, DataType.VARCHAR, "BATCH"));
        properties.add(new Property("checkpoint.interval", Direct.OUT, DataType.VARCHAR, "10000"));
        taskParams.put("localParams", properties);
        taskParams.put("resourceList", new ArrayList<>());
        taskParams.put("startupScript", "seatunnel.sh");
        taskParams.put("useCustom", true);
        taskParams.put("scriptType", "visualize");
        taskParams.put("deployMode", deployMode);
        taskParams.put("relation", JSONUtils.toJsonString(relations));
        taskParams.put("rawScript", getSeatunnelTaskParamRawScript(table, targetDataSource,
                sourceTaskCode, sinkTaskCode, sqlTransformTaskCode, sqlTaskContent, targetTableName,
                preCustomSql, dataSaveModel));
        return JSONUtils.toJsonString(taskParams);
    }

    private String getSeatunnelTaskParamRawScript(TableCondition table, DataSource targetDataSource,
                                                  long sourceTaskCode, long sinkTaskCode,
                                                  long sqlTransformTaskCode, String sqlTaskContent,
                                                  String targetTableName, String preCustomSql, String dataSaveModel) {
        log.info("generator seatunnel task raw script, table: {}", JSONUtils.toJsonString(table));
        List<SeatunnelTaskNodeVO> rawScripts = new ArrayList<>();
        boolean isSqlTransform = StringUtils.isNotEmpty(sqlTaskContent);
        SeatunnelTaskNodeVO sourceDTO = createSourceDTO(table, sourceTaskCode, sinkTaskCode, sqlTransformTaskCode, isSqlTransform);
        rawScripts.add(sourceDTO);
        if (isSqlTransform) {
            SeatunnelTaskNodeVO sqlTransformDTO = createSqlTransformDTO(sqlTaskContent, sourceTaskCode,
                    sinkTaskCode, sqlTransformTaskCode);
            rawScripts.add(sqlTransformDTO);
        }
//        List<String> primaryKeys = table.getPrimaryKey() == null ? Collections.emptyList() : Arrays.asList(table.getPrimaryKey().split(","));
        SeatunnelTaskNodeVO sinkDTO = createSinkDTO(targetDataSource, targetTableName,
                sourceTaskCode, sinkTaskCode, sqlTransformTaskCode, isSqlTransform, preCustomSql, table, dataSaveModel);
        rawScripts.add(sinkDTO);
        return JSONUtils.toJsonString(rawScripts);
    }

    private SeatunnelTaskNodeVO createSqlTransformDTO(String sqlTaskContent, long sourceTaskCode, long sinkTaskCode,
                                                      long sqlTransformTaskCode) {
        SeatunnelTaskNodeVO taskNode = new SeatunnelTaskNodeVO();
        taskNode.setType("TRANSFORM");
        taskNode.setPluginName("SQL");
        taskNode.setCode(String.valueOf(sqlTransformTaskCode));
        taskNode.setName("SQL语句");
        taskNode.setPreTaskcode(String.valueOf(sourceTaskCode));
        taskNode.setPostTaskcode(String.valueOf(sinkTaskCode));
        Map<String, Object> params = new HashMap<>();
        params.put("query", sqlTaskContent);
        params.put("type", "TRANSFORM");
        params.put("flag", "YES");
        taskNode.setParams(params);
        return taskNode;
    }

    private SeatunnelTaskNodeVO createSinkDTO(DataSource targetDataSource, String targetTableName,
                                              long sourceTaskCode, long sinkTaskCode,
                                              long sqlTransformTaskCode, boolean isSqlTransform,
                                              String preCustomSql, TableCondition table,
                                              String dataSaveModel) {
        BaseConnectionParam connectionParam = (BaseConnectionParam) DataSourceUtils.buildConnectionParams(targetDataSource.getType(), targetDataSource.getConnectionParams());
        SeatunnelTaskNodeVO taskNode = new SeatunnelTaskNodeVO();
        taskNode.setType("SINK");
        taskNode.setPluginName(targetDataSource.getType().name());
        taskNode.setCode(String.valueOf(sinkTaskCode));
        taskNode.setName(targetTableName);
        if (isSqlTransform) {
            taskNode.setPreTaskcode(String.valueOf(sqlTransformTaskCode));
        } else {
            taskNode.setPreTaskcode(String.valueOf(sourceTaskCode));
        }
        Map<String, Object> params = generatorSinkParams(targetDataSource, connectionParam, targetTableName,
                preCustomSql, table, dataSaveModel);
        taskNode.setParams(params);
        return taskNode;
    }

    @NotNull
    private static Map<String, Object> generatorSinkParams(DataSource targetDataSource,
                                                           BaseConnectionParam connectionParam,
                                                           String targetTableName,
                                                           String preCustomSql, TableCondition table,
                                                           String dataSaveModel) {
        switch (targetDataSource.getType()) {
            case DORIS:
                return generatorDorisSinkParams(targetDataSource, connectionParam, targetTableName,
                        preCustomSql, table, dataSaveModel);
            case MYSQL:
            default:
                return generatorMysqlSinkParams(targetDataSource, connectionParam, targetTableName, table);

        }
    }

    @NotNull
    private static Map<String, Object> generatorDorisSinkParams(DataSource targetDataSource,
                                                                BaseConnectionParam connectionParam,
                                                                String targetTableName,
                                                                String preCustomSql, TableCondition table,
                                                                String dataSaveModel) {
        Map<String, Object> params = new HashMap<>();
        params.put("datasourceId", targetDataSource.getId());
        params.put("database", connectionParam.getDatabase());
        params.put("table", targetTableName);
        params.put("schemaSaveMode", "CREATE_SCHEMA_WHEN_NOT_EXIST");
        params.put("dataSaveMode", "APPEND_DATA");
        params.put("format", "json");
        params.put("readJsonByLine", "true");
        params.put("columnSeparator", "");
        params.put("sinkEnableDelete", false);
        params.put("needsUnsupportedTypeCasting", false);
        params.put("sink", false);
        params.put("sinkEnable2pc", false);
        params.put("sinkCheckInterval", 10000);
        params.put("sinkMaxRetries", 3);
        params.put("sinkBufferSize", 262144);
        params.put("sinkBufferCount", 3);
        params.put("dorisBatchSize", 1024);
        params.put("batch_size", 1000);
        params.put("transaction_timeout_sec", -1);
        params.put("field_ide", "ORIGINAL");
        params.put("enable_upsert", true);
        params.put("use_copy_statement", false);
        params.put("schema_save_mode", "CREATE_SCHEMA_WHEN_NOT_EXIST");
        params.put("data_save_mode", dataSaveModel);
        if (StringUtils.isNotEmpty(table.getPrimaryKey())) {
            params.put("primary_keys", table.getPrimaryKeys());
        }
        if (StringUtils.isNotEmpty(table.getDupKey())) {
            params.put("dup_keys", table.getDupKeys());
        }
        if (StringUtils.isNotEmpty(preCustomSql)) {
            String customSql;
            if (StringUtils.isNotEmpty(table.getCondition())) {
                String conditionSql = table.getCondition();
                if (table.getCondition().toUpperCase(Locale.ROOT).startsWith("WHERE")) {
                    conditionSql = table.getCondition().substring(5);
                }
                if (preCustomSql.toUpperCase(Locale.ROOT).contains(" WHERE ")) {
                    customSql = preCustomSql.concat(" AND ").concat(conditionSql);
                } else {
                    customSql = preCustomSql.concat(" WHERE ").concat(conditionSql);
                }
            } else {
                customSql = preCustomSql;
            }
            customSql = customSql.replace("${tableName}",
                    connectionParam.getDatabase().concat(".").concat(targetTableName));
            params.put("custom_sql", customSql);
        }
        return params;
    }

    @NotNull
    private static Map<String, Object> generatorMysqlSinkParams(DataSource targetDataSource,
                                                                BaseConnectionParam connectionParam,
                                                                String targetTableName,
                                                                TableCondition table) {
        Map<String, Object> params = new HashMap<>();
        params.put("datasourceId", targetDataSource.getId());
        params.put("database", connectionParam.getDatabase());
        params.put("table", targetTableName);
        params.put("batch_size", 1000);
        params.put("is_exactly_once", false);
        params.put("transaction_timeout_sec", -1);
        params.put("transaction_timeout_sec", "ORIGINAL");
        params.put("enable_upsert", true);
        params.put("use_copy_statement", false);
        params.put("schema_save_mode", "CREATE_SCHEMA_WHEN_NOT_EXIST");
        params.put("data_save_mode", "APPEND_DATA");
        params.put("format", "json");
        if (StringUtils.isNotEmpty(table.getPrimaryKey())) {
            params.put("primary_keys", table.getPrimaryKeys());
        }
        return params;
    }

    private SeatunnelTaskNodeVO createSourceDTO(TableCondition table, long sourceTaskCode, long sinkTaskCode,
                                                long sqlTransformTaskCode, boolean isSqlTransform) {
        SeatunnelTaskNodeVO taskNode = new SeatunnelTaskNodeVO();
        taskNode.setType("SOURCE");
        if (table.getDsType() != DbType.MYSQL && table.getDsType() != DbType.DORIS) {
            taskNode.setPluginName("JDBC");
        } else {
            taskNode.setPluginName(table.getDsType().name());
        }
        taskNode.setCode(String.valueOf(sourceTaskCode));
        taskNode.setName(table.getTableName());
        if (isSqlTransform) {
            taskNode.setPostTaskcode(String.valueOf(sqlTransformTaskCode));
        } else {
            taskNode.setPostTaskcode(String.valueOf(sinkTaskCode));
        }
        Map<String, Object> params = new HashMap<>();
        taskNode.setParams(params);
        params.put("datasourceId", table.getDsId());
        params.put("fetch_size", 0);
        params.put("parallelism", 2);
        if (StringUtils.isNotEmpty(table.getCondition())) {
            params.put("where_condition", table.getCondition());
        }
        if (StringUtils.isNotEmpty(table.getPartitionColumn())) {
            params.put("partition_column", table.getPartitionColumn());
        }
        if (StringUtils.isNotEmpty(table.getQuerySql())) {
            params.put("is_custom_sql", true);
            params.put("query", table.getQuerySql());
        } else {
            params.put("is_custom_sql", false);
            params.put("table_path", table.getTablePath());
        }

//        switch (sourceDataSource.getType()) {
//            case MYSQL:
//                params.put("datasourceId", sourceDataSource.getId());
//                params.put("fetch_size", 0);
//                params.put("table_path", connectionParam.getDatabase().concat(".").concat(tableName));
//                params.put("is_custom_sql", false);
//                params.put("parallelism", 6);
//                break;
//            case POSTGRESQL:
//                break;
//            case ORACLE:
//                break;
//        }
        return taskNode;
    }


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

    private void classifyTables(boolean enableIncrement, DataSource ds, String sourceDatabase, String sourceSchema,
                                List<String> tableNames, List<String> incrementFields,
                                List<TableCondition> incrementEtlTables, List<TableCondition> fullEtlTables,
                                String incrementVariable, Map<String, String> tableUniqueKeys,
                                List<String> commonPrimaryKeys, boolean isSplitProcess) {
        List<String> lowerIncrementFields = incrementFields.stream().map(String::toLowerCase).collect(Collectors.toList());
        for (String tableName : tableNames) {
            // 获取表的列信息
            List<Map<String, Object>> columns = tableColumns.get(tableName);
            if (columns == null || columns.isEmpty()) {
                columns = dataSourceService.getTableColumnDetails(ds.getId(), sourceDatabase, tableName, null);
                tableColumns.put(tableName, columns);
            }
            TableCondition tableCondition = new TableCondition().setDsType(ds.getType()).setDsId(ds.getId())
                    .setDatabase(sourceDatabase).setTableName(tableName).setSchemaName(sourceSchema);
            String partitionColumn = null;
            Set<String> uniqueidentifierFields = new HashSet<>();
            Set<String> timestampFields = new HashSet<>();
            List<String> columnNames = new ArrayList<>();
            List<String> tabPrimaryKeys = new ArrayList<>();
            String condition = "";
            for (Map<String, Object> column : columns) {
                String typeName = column.get("type_name").toString();
                String columnName = column.get("column_name").toString();
                boolean isPrimary = (Boolean) column.get("is_primary_key");
                columnNames.add(columnName);
                if (isPrimary) {
                    tabPrimaryKeys.add(columnName);
                }
                if ("uniqueidentifier".equalsIgnoreCase(typeName)) {
                    uniqueidentifierFields.add(columnName);
                } else if ("timestamp".equalsIgnoreCase(typeName)) {
                    timestampFields.add(columnName);
                } else if (isSupportSplitColumn(typeName) && partitionColumn == null) {
                    partitionColumn = columnName;
                }
                if (enableIncrement && lowerIncrementFields.contains(columnName.toLowerCase())) {
                    // TODO 判断当前字段是否是时间类型字段，有些子算不是时间类型，则不支持
                    if (StringUtils.isEmpty(condition)) {
                        condition = String.format("where %s >= '${%s}'", columnName, incrementVariable);
                    } else {
                        condition = String.format("%s or %s >= '${%s}'", condition, columnName, incrementVariable);
                    }
                }
            }
            if (!uniqueidentifierFields.isEmpty() || !timestampFields.isEmpty()) {
                String columnStr = "";
                for (String field : columnNames) {
                    if (uniqueidentifierFields.contains(field)) {
                        columnStr = columnStr.concat("CAST([" + field + "] AS VARCHAR(36)) AS [" + field).concat("], ");
                    } else if (timestampFields.contains(field)) {
                        columnStr = columnStr.concat("CAST(CONVERT(BIGINT, [" + field + "]) AS VARCHAR(32)) AS [" + field).concat("], ");
                    } else {
                        columnStr = columnStr.concat("[" + field + "], ");
                    }
                }
                columnStr = columnStr.substring(0, columnStr.length() - 2);
                String querySql = String.format("SELECT %s FROM %s", columnStr, tableName);
                tableCondition.setQuerySql(querySql);
            } else {
                // 没有特殊字段，则置为空，seatunnel会默认使用所有列
                partitionColumn = null;
            }
            if (partitionColumn != null) {
                tableCondition.setPartitionColumn(partitionColumn);
            }
            if (StringUtils.isNotEmpty(condition)) {
                // 如果表包含增量字段，则加入增量同步列表
                tableCondition.setCondition(condition);
                if (isSplitProcess) {
                    incrementEtlTables.add(tableCondition);
                } else {
                    fullEtlTables.add(tableCondition);
                }
            } else {
                // 否则加入全量同步列表
                fullEtlTables.add(tableCondition);
            }
            setTablePrimaryKeyOrDumpKeys(tableCondition, commonPrimaryKeys, tabPrimaryKeys, tableUniqueKeys.get(tableName), columnNames.get(0));
        }
    }

    private void setTablePrimaryKeyOrDumpKeys(TableCondition tableCondition, List<String> commonPrimaryKeys,
                                              List<String> tabPrimaryKeys, String uniqueKeys, String firstColumn) {
        // 如果表有明确指定主键，则不需要指定主键seatunnel支持包含主键表业务处理逻辑
        if (CollectionUtils.isNotEmpty(tabPrimaryKeys) && CollectionUtils.isEmpty(commonPrimaryKeys)) {
            if (StringUtils.isNotEmpty(tableCondition.getQuerySql())) {
                tableCondition.setPrimaryKey(String.join(",", tabPrimaryKeys));
            }
            return;
        }

        if (CollectionUtils.isNotEmpty(tabPrimaryKeys)) {
            String primaryKeys;
            if (CollectionUtils.isEmpty(commonPrimaryKeys)) {
                primaryKeys = String.join(",", tabPrimaryKeys);
            } else {
                primaryKeys = String.join(",", commonPrimaryKeys).concat(",").concat(String.join(",", tabPrimaryKeys));
            }
            tableCondition.setPrimaryKey(primaryKeys);
        } else if (StringUtils.isNotEmpty(uniqueKeys)) {
            String primaryKeys;
            if (CollectionUtils.isEmpty(commonPrimaryKeys)) {
                primaryKeys = String.join(",", uniqueKeys);
            } else {
                primaryKeys = String.join(",", commonPrimaryKeys).concat(",").concat(String.join(",", uniqueKeys));
            }
            tableCondition.setPrimaryKey(primaryKeys);
        } else if (CollectionUtils.isNotEmpty(commonPrimaryKeys)) {
            tableCondition.setDupKey(String.join(",", commonPrimaryKeys));
        } else {
            tableCondition.setDupKey(firstColumn);
        }
    }

    private String getTablePrimaryKey(String tableName, Map<String, String> tablePrimaryKeys, List<String> primaryKeys) {
        if (CollectionUtils.isEmpty(primaryKeys) && (tablePrimaryKeys == null || tablePrimaryKeys.isEmpty())) {
            return "";
        }

        String tablePrimaryKey;

        // 从tablePrimaryKeys中获取该表的主键信息
        String tableSpecificPrimaryKey = tablePrimaryKeys.get(tableName);

        if (CollectionUtils.isNotEmpty(primaryKeys)) {
            // 如果primaryKeys不为空
            List<String> resultPrimaryKeys = new ArrayList<>(primaryKeys);

            if (StringUtils.isNotEmpty(tableSpecificPrimaryKey)) {
                // 将tableSpecificPrimaryKey按逗号分割为列表
                List<String> tableSpecificKeys = Arrays.asList(tableSpecificPrimaryKey.split(","));

                // 遍历tableSpecificKeys中的每个主键
                for (String tableKey : tableSpecificKeys) {
                    if (StringUtils.isNotEmpty(tableKey)) {
                        String tableKeyTrimmed = tableKey.trim();
                        boolean found = false;

                        // 在resultPrimaryKeys中查找是否有不区分大小写相同的键
                        for (int i = 0; i < resultPrimaryKeys.size(); i++) {
                            String existingKey = resultPrimaryKeys.get(i);
                            if (StringUtils.isNotEmpty(existingKey) &&
                                    existingKey.trim().equalsIgnoreCase(tableKeyTrimmed)) {
                                // 如果找到，用tablePrimaryKeys中的值替换primaryKeys中的值
                                resultPrimaryKeys.set(i, tableKeyTrimmed);
                                found = true;
                                break;
                            }
                        }

                        // 如果在primaryKeys中没有找到该键，则添加到结果中
                        if (!found) {
                            resultPrimaryKeys.add(tableKeyTrimmed);
                        }
                    }
                }

                tablePrimaryKey = String.join(",", resultPrimaryKeys);
            } else {
                // 如果tableSpecificPrimaryKey为空，直接使用primaryKeys
                tablePrimaryKey = String.join(",", primaryKeys);
            }
        } else {
            // 如果primaryKeys为空，直接使用tablePrimaryKeys中的主键
            tablePrimaryKey = tableSpecificPrimaryKey;
        }

        return tablePrimaryKey;
    }


    private boolean isSupportSplitColumn(String typeName) {
        String upperTypeName = typeName.toUpperCase();
        switch (upperTypeName) {
            case "TINYINT":
            case "SMALLINT":
            case "INT":
            case "BIGINT":
            case "DOUBLE":
            case "FLOAT":
            case "DECIMAL":
            case "TIMESTAMP":
            case "VARCHAR":
            case "NVARCHAR":
            case "CHAR":
            case "INTEGER":
            case "INT_IDENTITY":
            case "DATETIME2":
            case "DATE":
                return true;
            default:
                return false;
        }
    }


}
