package org.apache.dolphinscheduler.api.service;

import org.apache.dolphinscheduler.dao.entity.User;

import java.util.Map;

public interface BatchCreateDefinitionService {



    /**
     * create process definition by datasource tables
     *
     * @param loginUser          login user
     * @param projectCode        project code
     * @param name               process definition name
     * @param sourceDatasourceId source datasource id
     * @param targetDatasourceId target datasource id
     * @param serialTaskNum      workflow serialTaskNum
     * @param targetTablePrefix  target table prefix
     * @param etlTechnology      etl technology (Seatunnel/datax)
     * @param deployMode         deploy mode (cluster/local)
     * @return create result
     */
    Map<String, Object> createProcessDefinitionByDatasourceTables(User loginUser,
                                                                  long projectCode,
                                                                  String name,
                                                                  Integer sourceDatasourceId,
                                                                  Integer targetDatasourceId,
                                                                  int serialTaskNum,
                                                                  String targetTablePrefix,
                                                                  String etlTechnology, String deployMode,
                                                                  String filterType, String filterContentJson,
                                                                  boolean enableIncrement, boolean isSplitProcess,
                                                                  String incrementFieldJson,
                                                                  String incrementParamsJson,
                                                                  String primaryKeyJson, String sqlTaskContent,
                                                                  String globalParams, String preCustomSql,
                                                                  String dataSaveModel, String tableUniqueKeyJson);


}
