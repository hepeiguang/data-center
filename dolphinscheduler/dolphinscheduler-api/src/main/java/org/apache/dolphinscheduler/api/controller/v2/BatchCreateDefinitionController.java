package org.apache.dolphinscheduler.api.controller.v2;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.api.audit.OperatorLog;
import org.apache.dolphinscheduler.api.audit.enums.AuditType;
import org.apache.dolphinscheduler.api.controller.BaseController;
import org.apache.dolphinscheduler.api.exceptions.ApiException;
import org.apache.dolphinscheduler.api.service.BatchCreateDefinitionService;
import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.common.constants.Constants;
import org.apache.dolphinscheduler.dao.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

import static org.apache.dolphinscheduler.api.enums.Status.CREATE_PROCESS_DEFINITION_ERROR;

@Tag(name = "PROCESS_DEFINITION_TAG")
@RestController
@RequestMapping("projects/{projectCode}/batch-create-definition")
@Slf4j
public class BatchCreateDefinitionController extends BaseController {

    @Resource
    private BatchCreateDefinitionService batchCreateDefinitionService;

    /**
     * create process definition by datasource tables
     *
     * @param loginUser          login user
     * @param projectCode        project code
     * @param name               process definition name
     * @param sourceDatasourceId source datasource id
     * @param targetDatasourceId target datasource id
     * @param serialTaskNum      workflow serialTaskNum (all/include/exclude)
     * @param targetTablePrefix  target table prefix
     * @param etlTechnology      etl technology (SEATUNNEL/DATAX)
     * @return create result
     */
    @Operation(summary = "createByDatasourceTables", description = "CREATE_PROCESS_DEFINITION_BY_DATASOURCE_TABLES_NOTES")
    @Parameters({
            @Parameter(name = "name", description = "PROCESS_DEFINITION_NAME", required = true, schema = @Schema(implementation = String.class)),
            @Parameter(name = "sourceDatasourceId", description = "SOURCE_DATASOURCE_ID", required = true, schema = @Schema(implementation = int.class, example = "1")),
            @Parameter(name = "targetDatasourceId", description = "TARGET_DATASOURCE_ID", required = true, schema = @Schema(implementation = int.class, example = "1")),
            @Parameter(name = "serialTaskNum", description = "WORKFLOW_PARALLELISM", required = false, schema = @Schema(implementation = int.class, example = "2")),
            @Parameter(name = "targetTablePrefix", description = "TARGET_TABLE_PREFIX", required = false, schema = @Schema(implementation = String.class)),
            @Parameter(name = "etlTechnology", description = "ETL_TECHNOLOGY", required = false, schema = @Schema(implementation = String.class, example = "SEATUNNEL/DATAX")),
            @Parameter(name = "deployMode", description = "DEPLOY_MODE", required = false, schema = @Schema(implementation = String.class, example = "cluster/local")),
            @Parameter(name = "filterType", description = "FILTER_TYPE", required = false, schema = @Schema(implementation = String.class, example = "all/include/exclude/notemptytable")),
            @Parameter(name = "filterContentJson", description = "FILTER_CONTENT_JSON", required = false, schema = @Schema(implementation = String.class, example = "[]")),
            @Parameter(name = "enableIncrement", description = "ENABLE_INCREMENT", required = false, schema = @Schema(implementation = Boolean.class, example = "[]")),
            @Parameter(name = "incrementFieldJson", description = "INCREMENT_FIELD_JSON", required = false, schema = @Schema(implementation = String.class, example = "[]")),
            @Parameter(name = "incrementParamsJson", description = "INCREMENT_PARAMS_JSON", required = false, schema = @Schema(implementation = String.class, example = "[]")),
            @Parameter(name = "tableUniqueKeyJson", description = "INCREMENT_PARAMS_JSON", required = false, schema = @Schema(implementation = String.class, example = "{}"))
    })
    @PostMapping(value = "/create-by-datasource-tables")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiException(CREATE_PROCESS_DEFINITION_ERROR)
    @OperatorLog(auditType = AuditType.PROCESS_CREATE)
    public Result createByDatasourceTables(@Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                           @Parameter(name = "projectCode", description = "PROJECT_CODE", required = true) @PathVariable long projectCode,
                                           @RequestParam(value = "name", required = false) String name,
                                           @RequestParam(value = "sourceDatasourceId", required = true) Integer sourceDatasourceId,
                                           @RequestParam(value = "targetDatasourceId", required = true) Integer targetDatasourceId,
                                           @RequestParam(value = "targetTablePrefix", required = false, defaultValue = "") String targetTablePrefix,
                                           @RequestParam(value = "serialTaskNum", required = false, defaultValue = "20") int serialTaskNum,
                                           @RequestParam(value = "etlTechnology", required = false, defaultValue = "SEATUNNEL") String etlTechnology,
                                           @RequestParam(value = "deployMode", required = false, defaultValue = "cluster") String deployMode,
                                           @RequestParam(value = "filterType", required = false, defaultValue = "all") String filterType,
                                           @RequestParam(value = "filterContentJson", required = false, defaultValue = "[]") String filterContentJson,
                                           @RequestParam(value = "enableIncrement", required = false, defaultValue = "false") boolean enableIncrement,
                                           @RequestParam(value = "isSplitProcess", required = false, defaultValue = "false") boolean isSplitProcess,
                                           @RequestParam(value = "incrementFieldJson", required = false, defaultValue = "[]") String incrementFieldJson,
                                           @RequestParam(value = "incrementParam", required = false, defaultValue = "") String incrementParam,
                                           @RequestParam(value = "primaryKeyJson", required = false, defaultValue = "[]") String primaryKeyJson,
                                           @RequestParam(value = "sqlTaskContent", required = false) String sqlTaskContent,
                                           @RequestParam(value = "globalParams", required = false, defaultValue = "[]") String globalParams,
                                           @RequestParam(value = "preCustomSql", required = false, defaultValue = "") String preCustomSql,
                                           @RequestParam(value = "dataSaveModel", required = false, defaultValue = "APPEND_DATA") String dataSaveModel,
                                           @RequestParam(value = "tableUniqueKeyJson", required = false, defaultValue = "{}") String tableUniqueKeyJson
    ) {

        // 调用service层方法创建工作流
        Map<String, Object> result = batchCreateDefinitionService.createProcessDefinitionByDatasourceTables(
                loginUser,
                projectCode,
                name,
                sourceDatasourceId,
                targetDatasourceId,
                serialTaskNum,
                targetTablePrefix,
                etlTechnology, deployMode, filterType, filterContentJson, enableIncrement, isSplitProcess,
                incrementFieldJson, incrementParam, primaryKeyJson, sqlTaskContent, globalParams, preCustomSql,
                dataSaveModel, tableUniqueKeyJson
        );
        return returnDataList(result);
    }

}
