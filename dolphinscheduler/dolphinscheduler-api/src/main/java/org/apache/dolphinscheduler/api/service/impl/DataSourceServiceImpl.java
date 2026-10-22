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
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.api.constants.ApiFuncIdentificationConstant;
import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.exceptions.ServiceException;
import org.apache.dolphinscheduler.api.service.DataSourceService;
import org.apache.dolphinscheduler.api.utils.PageInfo;
import org.apache.dolphinscheduler.api.vo.DbTypeVo;
import org.apache.dolphinscheduler.common.constants.Constants;
import org.apache.dolphinscheduler.common.enums.AuthorizationType;
import org.apache.dolphinscheduler.common.enums.UserType;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.dao.entity.DataSource;
import org.apache.dolphinscheduler.dao.entity.User;
import org.apache.dolphinscheduler.dao.mapper.DataSourceMapper;
import org.apache.dolphinscheduler.dao.mapper.DataSourceUserMapper;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.BaseDataSourceParamDTO;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.DataSourceProcessor;
import org.apache.dolphinscheduler.plugin.datasource.api.utils.DataSourceUtils;
import org.apache.dolphinscheduler.spi.datasource.BaseConnectionParam;
import org.apache.dolphinscheduler.spi.datasource.ConnectionParam;
import org.apache.dolphinscheduler.spi.enums.DbType;
import org.apache.dolphinscheduler.spi.params.base.ParamsOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.dolphinscheduler.api.constants.ApiFuncIdentificationConstant.DATASOURCE_DELETE;
import static org.apache.dolphinscheduler.api.constants.ApiFuncIdentificationConstant.DATASOURCE_UPDATE;

/**
 * data source service impl
 */
@Service
@Slf4j
public class DataSourceServiceImpl extends BaseServiceImpl implements DataSourceService {

    @Autowired
    private DataSourceMapper dataSourceMapper;

    @Autowired
    private DataSourceUserMapper datasourceUserMapper;

    private static final String TABLE = "TABLE";
    private static final String VIEW = "VIEW";
    private static final String[] TABLE_TYPES = new String[]{TABLE, VIEW};
    private static final String TABLE_NAME = "TABLE_NAME";
    private static final String COLUMN_NAME = "COLUMN_NAME";
    private static final String TYPE_NAME = "TYPE_NAME";
    private static final String COLUMN_SIZE = "COLUMN_SIZE";
    private static final String REMARKS = "REMARKS";


    /**
     * create data source
     *
     * @param loginUser       login user
     * @param datasourceParam datasource parameters
     * @return create result code
     */
    @Override
    public DataSource createDataSource(User loginUser, BaseDataSourceParamDTO datasourceParam) {
        DataSourceUtils.checkDatasourceParam(datasourceParam);
        if (!canOperatorPermissions(loginUser, null, AuthorizationType.DATASOURCE,
                ApiFuncIdentificationConstant.DATASOURCE_CREATE_DATASOURCE)) {
            throw new ServiceException(Status.USER_NO_OPERATION_PERM);
        }
        // check name can use or not
        if (checkName(datasourceParam.getName())) {
            throw new ServiceException(Status.DATASOURCE_EXIST);
        }
        if (checkDescriptionLength(datasourceParam.getNote())) {
            throw new ServiceException(Status.DESCRIPTION_TOO_LONG_ERROR);
        }
        ConnectionParam connectionParam = DataSourceUtils.buildConnectionParams(datasourceParam);

        // build datasource
        DataSource dataSource = new DataSource();
        Date now = new Date();

        dataSource.setName(datasourceParam.getName().trim());
        dataSource.setNote(datasourceParam.getNote());
        dataSource.setUserId(loginUser.getId());
        dataSource.setUserName(loginUser.getUserName());
        dataSource.setType(datasourceParam.getType());
        dataSource.setServerType(datasourceParam.getType().getServerType());
        dataSource.setConnectionParams(JSONUtils.toJsonString(connectionParam));
        dataSource.setCreateTime(now);
        dataSource.setUpdateTime(now);
        try {
            dataSourceMapper.insert(dataSource);
            return dataSource;
        } catch (DuplicateKeyException ex) {
            log.error("The datasource id:{} already exists", dataSource.getId(), ex);
            throw new ServiceException(Status.DATASOURCE_EXIST);
        }
    }

    /**
     * updateProcessInstance datasource
     *
     * @param loginUser login user
     * @return update result code
     */
    @Override
    public DataSource updateDataSource(User loginUser, BaseDataSourceParamDTO dataSourceParam) {
        DataSourceUtils.checkDatasourceParam(dataSourceParam);
        // determine whether the data source exists
        DataSource dataSource = dataSourceMapper.selectById(dataSourceParam.getId());
        if (dataSource == null) {
            throw new ServiceException(Status.RESOURCE_NOT_EXIST);
        }

        if (!canOperatorPermissions(loginUser, new Object[]{dataSource.getId()}, AuthorizationType.DATASOURCE,
                DATASOURCE_UPDATE)) {
            throw new ServiceException(Status.USER_NO_OPERATION_PERM);
        }

        // check name can use or not
        if (!dataSourceParam.getName().trim().equals(dataSource.getName()) && checkName(dataSourceParam.getName())) {
            throw new ServiceException(Status.DATASOURCE_EXIST);
        }
        if (checkDescriptionLength(dataSourceParam.getNote())) {
            throw new ServiceException(Status.DESCRIPTION_TOO_LONG_ERROR);
        }
        // check password，if the password is not updated, set to the old password.
        ConnectionParam connectionParam = DataSourceUtils.buildConnectionParams(dataSourceParam);

        String password = connectionParam.getPassword();

        if (StringUtils.isBlank(password) || Constants.XXXXXX.equals(password)) {
            String oldConnectionParams = dataSource.getConnectionParams();
            ObjectNode oldParams = JSONUtils.parseObject(oldConnectionParams);
            connectionParam.setPassword(oldParams.path(Constants.PASSWORD).asText());
        }

        Date now = new Date();

        dataSource.setName(dataSourceParam.getName().trim());
        dataSource.setNote(dataSourceParam.getNote());
        dataSource.setUserName(loginUser.getUserName());
        dataSource.setType(dataSource.getType());
        dataSource.setServerType(dataSource.getType().getServerType());
        dataSource.setConnectionParams(JSONUtils.toJsonString(connectionParam));
        dataSource.setUpdateTime(now);
        try {
            dataSourceMapper.updateById(dataSource);
            return dataSource;
        } catch (DuplicateKeyException ex) {
            throw new ServiceException(Status.DATASOURCE_EXIST);
        }
    }

    private boolean checkName(String name) {
        List<DataSource> queryDataSource = dataSourceMapper.queryDataSourceByName(name.trim());
        return queryDataSource != null && !queryDataSource.isEmpty();
    }

    /**
     * updateProcessInstance datasource
     *
     * @param id datasource id
     * @return data source detail
     */
    @Override
    public BaseDataSourceParamDTO queryDataSource(int id, User loginUser) {
        DataSource dataSource = dataSourceMapper.selectById(id);
        if (dataSource == null) {
            log.error("Datasource does not exist, id:{}.", id);
            throw new ServiceException(Status.RESOURCE_NOT_EXIST);
        }

        if (!canOperatorPermissions(loginUser, new Object[]{dataSource.getId()}, AuthorizationType.DATASOURCE,
                ApiFuncIdentificationConstant.DATASOURCE)) {
            throw new ServiceException(Status.USER_NO_OPERATION_PERM);
        }

        // type
        BaseDataSourceParamDTO baseDataSourceParamDTO = DataSourceUtils.buildDatasourceParamDTO(
                dataSource.getType(), dataSource.getConnectionParams());
        baseDataSourceParamDTO.setId(dataSource.getId());
        baseDataSourceParamDTO.setName(dataSource.getName());
        baseDataSourceParamDTO.setNote(dataSource.getNote());
        baseDataSourceParamDTO.setPassword(getHiddenPassword());

        return baseDataSourceParamDTO;
    }

    /**
     * query datasource list by keyword
     *
     * @param loginUser login user
     * @param searchVal search value
     * @param pageNo    page number
     * @param pageSize  page size
     * @return data source list page
     */
    @Override
    public PageInfo<DataSource> queryDataSourceListPaging(User loginUser, String searchVal, Integer pageNo,
                                                          Integer pageSize, String type, String serverType) {
        IPage<DataSource> dataSourceList;
        Page<DataSource> dataSourcePage = new Page<>(pageNo, pageSize);
        PageInfo<DataSource> pageInfo = new PageInfo<>(pageNo, pageSize);
        if (loginUser.getUserType().equals(UserType.ADMIN_USER)) {
            dataSourceList = dataSourceMapper.selectPaging(dataSourcePage, "", searchVal, type, serverType);
        } else {
            Set<Integer> ids = resourcePermissionCheckService
                    .userOwnedResourceIdsAcquisition(AuthorizationType.DATASOURCE, loginUser.getId(), log);
            if (ids.isEmpty()) {
                return pageInfo;
            }
            dataSourceList = dataSourceMapper.selectPagingByIds(dataSourcePage, new ArrayList<>(ids), searchVal, type, serverType);
        }

        List<DataSource> dataSources = dataSourceList != null ? dataSourceList.getRecords() : new ArrayList<>();
        handlePasswd(dataSources);
        pageInfo.setTotal((int) (dataSourceList != null ? dataSourceList.getTotal() : 0L));
        pageInfo.setTotalList(dataSources);
        return pageInfo;
    }

    /**
     * handle datasource connection password for safety
     */
    private void handlePasswd(List<DataSource> dataSourceList) {
        for (DataSource dataSource : dataSourceList) {
            String connectionParams = dataSource.getConnectionParams();
            ObjectNode object = JSONUtils.parseObject(connectionParams);
            object.put(Constants.PASSWORD, getHiddenPassword());
            dataSource.setConnectionParams(object.toString());
        }
    }

    /**
     * get hidden password (resolve the security hotspot)
     *
     * @return hidden password
     */
    private String getHiddenPassword() {
        return Constants.XXXXXX;
    }

    /**
     * query data resource list
     *
     * @param loginUser login user
     * @param type      data source type
     * @return data source list page
     */
    @Override
    public List<DataSource> queryDataSourceList(User loginUser, Integer type) {

        List<DataSource> datasourceList;
        if (loginUser.getUserType().equals(UserType.ADMIN_USER)) {
            datasourceList = dataSourceMapper.queryDataSourceByType("", type);
        } else {
            Set<Integer> ids = resourcePermissionCheckService
                    .userOwnedResourceIdsAcquisition(AuthorizationType.DATASOURCE, loginUser.getId(), log);
            if (ids.isEmpty()) {
                return Collections.emptyList();
            }
            datasourceList = dataSourceMapper.selectBatchIds(ids).stream()
                    .filter(dataSource -> dataSource.getType().getCode() == type).collect(Collectors.toList());
        }

        return datasourceList;
    }

    /**
     * verify datasource exists
     *
     * @param name datasource name
     * @return true if data datasource not exists, otherwise return false
     */
    @Override
    public void verifyDataSourceName(String name) {
        List<DataSource> dataSourceList = dataSourceMapper.queryDataSourceByName(name);
        if (dataSourceList != null && !dataSourceList.isEmpty()) {
            throw new ServiceException(Status.DATASOURCE_EXIST);
        }
    }

    /**
     * check connection
     *
     * @param type            data source type
     * @param connectionParam connectionParam
     * @return true if connect successfully, otherwise false
     * @return true if connect successfully, otherwise false
     */
    @Override
    public boolean checkConnection(DbType type, ConnectionParam connectionParam) {
        try {
            DataSourceProcessor sshDataSourceProcessor = DataSourceUtils.getDatasourceProcessor(type);
            return sshDataSourceProcessor.checkDataSourceConnectivity(connectionParam);
        } catch (Exception e) {
            throw new ServiceException(Status.CONNECTION_TEST_FAILURE, e.getMessage());
        }
    }

    /**
     * test connection
     *
     * @param id datasource id
     * @return connect result code
     */
    @Override
    public void connectionTest(int id) {
        DataSource dataSource = dataSourceMapper.selectById(id);
        if (dataSource == null) {
            throw new ServiceException(Status.RESOURCE_NOT_EXIST);
        }
        checkConnection(dataSource.getType(),
                DataSourceUtils.buildConnectionParams(dataSource.getType(), dataSource.getConnectionParams()));
    }

    /**
     * delete datasource
     *
     * @param loginUser    login user
     * @param datasourceId data source id
     * @return delete result code
     */
    @Override
    @Transactional
    public void delete(User loginUser, int datasourceId) {
        // query datasource by id
        DataSource dataSource = dataSourceMapper.selectById(datasourceId);
        if (dataSource == null) {
            throw new ServiceException(Status.RESOURCE_NOT_EXIST);
        }
        if (!canOperatorPermissions(loginUser, new Object[]{dataSource.getId()}, AuthorizationType.DATASOURCE,
                DATASOURCE_DELETE)) {
            throw new ServiceException(Status.USER_NO_OPERATION_PERM);
        }
        dataSourceMapper.deleteById(datasourceId);
        datasourceUserMapper.deleteByDatasourceId(datasourceId);
    }

    /**
     * unauthorized datasource
     *
     * @param loginUser login user
     * @param userId    user id
     * @return unauthed data source result code
     */
    @Override
    public List<DataSource> unAuthDatasource(User loginUser, String userId) {
        List<DataSource> datasourceList;
        if (canOperatorPermissions(loginUser, null, AuthorizationType.DATASOURCE, null)) {
            // admin gets all data sources except userId
            datasourceList = dataSourceMapper.queryDatasourceExceptUserId(userId);
        } else {
            // non-admins users get their own data sources
            datasourceList = dataSourceMapper.selectByMap(Collections.singletonMap("user_id", loginUser.getId()));
        }
        List<DataSource> resultList = new ArrayList<>();
        Set<DataSource> datasourceSet;
        if (datasourceList != null && !datasourceList.isEmpty()) {
            datasourceSet = new HashSet<>(datasourceList);

            List<DataSource> authedDataSourceList = dataSourceMapper.queryAuthedDatasource(userId);

            Set<DataSource> authedDataSourceSet;
            if (authedDataSourceList != null && !authedDataSourceList.isEmpty()) {
                authedDataSourceSet = new HashSet<>(authedDataSourceList);
                datasourceSet.removeAll(authedDataSourceSet);
            }
            resultList = new ArrayList<>(datasourceSet);
        }
        return resultList;
    }

    /**
     * authorized datasource
     *
     * @param loginUser login user
     * @param userId    user id
     * @return authorized result code
     */
    @Override
    public List<DataSource> authedDatasource(User loginUser, String userId) {
        List<DataSource> authedDatasourceList = dataSourceMapper.queryAuthedDatasource(userId);
        return authedDatasourceList;
    }

    @Override
    public List<ParamsOptions> getTables(Integer datasourceId, String database) {
        DataSource dataSource = dataSourceMapper.selectById(datasourceId);

        List<String> tableList;
        BaseConnectionParam connectionParam =
                (BaseConnectionParam) DataSourceUtils.buildConnectionParams(
                        dataSource.getType(),
                        dataSource.getConnectionParams());

        if (null == connectionParam) {
            throw new ServiceException(Status.DATASOURCE_CONNECT_FAILED);
        }

        Connection connection =
                DataSourceUtils.getConnection(dataSource.getType(), connectionParam);
        ResultSet tables = null;

        try {

            if (null == connection) {
                throw new ServiceException(Status.DATASOURCE_CONNECT_FAILED);
            }

            DatabaseMetaData metaData = connection.getMetaData();
            String schema;
            try {
                schema = metaData.getConnection().getSchema();
            } catch (SQLException e) {
                log.error("Cant not get the schema, datasourceId:{}.", datasourceId, e);
                throw new ServiceException(Status.GET_DATASOURCE_TABLES_ERROR);
            }

            tables = metaData.getTables(
                    database,
                    getDbSchemaPattern(dataSource.getType(), schema, connectionParam),
                    "%", TABLE_TYPES);
            if (null == tables) {
                log.error("Get datasource tables error, datasourceId:{}.", datasourceId);
                throw new ServiceException(Status.GET_DATASOURCE_TABLES_ERROR);
            }

            tableList = new ArrayList<>();
            while (tables.next()) {
                String name = tables.getString(TABLE_NAME);
                tableList.add(name);
            }

        } catch (Exception e) {
            log.error("Get datasource tables error, datasourceId:{}.", datasourceId, e);
            throw new ServiceException(Status.GET_DATASOURCE_TABLES_ERROR);
        } finally {
            closeResult(tables);
            releaseConnection(connection);
        }

        List<ParamsOptions> options = getParamsOptions(tableList);
        return options;
    }

    @Override
    public List<ParamsOptions> getTableColumns(Integer datasourceId, String database, String tableName) {
        DataSource dataSource = dataSourceMapper.selectById(datasourceId);
        BaseConnectionParam connectionParam =
                (BaseConnectionParam) DataSourceUtils.buildConnectionParams(
                        dataSource.getType(),
                        dataSource.getConnectionParams());

        if (null == connectionParam) {
            throw new ServiceException(Status.DATASOURCE_CONNECT_FAILED);
        }

        Connection connection =
                DataSourceUtils.getConnection(dataSource.getType(), connectionParam);
        List<String> columnList = new ArrayList<>();
        ResultSet rs = null;

        try {
            if (null == connection) {
                throw new ServiceException(Status.DATASOURCE_CONNECT_FAILED);
            }

            DatabaseMetaData metaData = connection.getMetaData();

            if (dataSource.getType() == DbType.ORACLE) {
                database = null;
            }
            rs = metaData.getColumns(database, null, tableName, "%");
            if (rs == null) {
                throw new ServiceException(Status.DATASOURCE_CONNECT_FAILED);
            }
            while (rs.next()) {
                columnList.add(rs.getString(COLUMN_NAME));
            }
        } catch (Exception e) {
            log.error("Get datasource table columns error, datasourceId:{}.", dataSource.getId(), e);
            throw new ServiceException(Status.DATASOURCE_CONNECT_FAILED);
        } finally {
            closeResult(rs);
            releaseConnection(connection);
        }

        List<ParamsOptions> options = getParamsOptions(columnList);
        return options;
    }

    @Override
    public List<Map<String, Object>> getTableColumnDetails(Integer datasourceId, String database, String tableName, String sql) {
        DataSource dataSource = dataSourceMapper.selectById(datasourceId);
        BaseConnectionParam connectionParam =
                (BaseConnectionParam) DataSourceUtils.buildConnectionParams(
                        dataSource.getType(),
                        dataSource.getConnectionParams());

        if (null == connectionParam) {
            throw new ServiceException(Status.DATASOURCE_CONNECT_FAILED);
        }
        Connection connection =
                DataSourceUtils.getConnection(dataSource.getType(), connectionParam);
        List<Map<String, Object>> columnList = new ArrayList<>();
        ResultSet rs = null;
        ResultSet primaryKeyRs = null;
        if (StringUtils.isEmpty(tableName)) {
            Statement statement = null;
            ResultSet resultSet = null;
            try {
                connection = DataSourceUtils.getConnection(dataSource.getType(), connectionParam);
                statement = connection.createStatement();
                resultSet = statement.executeQuery(sql);
                if (resultSet == null) {
                    throw new IllegalArgumentException("ResultSet 不能为 null");
                }
                ResultSetMetaData metaData = resultSet.getMetaData();
                if (metaData == null) {
                    throw new IllegalStateException("ResultSetMetaData 获取失败");
                }
                int columnCount = metaData.getColumnCount();

                // 只取字段名称
                for (int i = 1; i <= columnCount; i++) {
                    System.out.println(JSONUtils.toJsonNode(metaData));
                    Map<String, Object> m1 = new HashMap<>();
                    m1.put("table_name", metaData.getTableName(i));
                    ;
                    m1.put("column_name", metaData.getColumnName(i));
                    m1.put("type_name", metaData.getTableName( i));
                    m1.put("column_size", metaData.getColumnDisplaySize(i));
                    m1.put("remarks", "");
                    columnList.add(m1);
                }
            } catch (SQLException e) {
                log.error("Error executing SQL or retrieving metadata: {}", sql, e);
                throw new RuntimeException("EXECUTE_SQL_META_FAILED:" + e.getMessage());
                // 处理异常
            } finally {
                // 关闭资源
                if (resultSet != null) try {
                    resultSet.close();
                } catch (SQLException e) {
                }
                if (statement != null) try {
                    statement.close();
                } catch (SQLException e) {
                }
                if (connection != null) try {
                    connection.close();
                } catch (SQLException e) {
                }
            }
        } else {
            try {
                if (null == connection) {
                    throw new ServiceException(Status.DATASOURCE_CONNECT_FAILED);
                }

                DatabaseMetaData metaData = connection.getMetaData();

                if (dataSource.getType() == DbType.ORACLE) {
                    database = null;
                }
                String schema = null;
                if (dataSource.getType() == DbType.OPENGAUSS || dataSource.getType() == DbType.POSTGRESQL) {
                    if (tableName.contains(".")) {
                        tableName = tableName.substring(tableName.indexOf(".") + 1);
                    }
                    schema = connectionParam.getOther() == null ? null : connectionParam.getOther().get("currentSchema");
                }
                rs = metaData.getColumns(database, schema, tableName, "%");
                if (rs == null) {
                    throw new ServiceException(Status.DATASOURCE_CONNECT_FAILED);
                }

                primaryKeyRs = metaData.getPrimaryKeys(database, schema, tableName);
                List<String> primaryKeys = new ArrayList<>();
                while (primaryKeyRs.next()) {
                    primaryKeys.add(primaryKeyRs.getString(COLUMN_NAME));
                }
                while (rs.next()) {
                    Map<String, Object> m1 = new HashMap<>();
                    m1.put("table_name", tableName);
                    m1.put("column_name", rs.getString(COLUMN_NAME));
                    m1.put("type_name", rs.getString(TYPE_NAME));
                    m1.put("column_size", rs.getString(COLUMN_SIZE));
                    m1.put("remarks", rs.getString(REMARKS));
                    m1.put("is_primary_key", primaryKeys.contains(rs.getString(COLUMN_NAME)));
                    columnList.add(m1);
                }
            } catch (Exception e) {
                log.error("Get datasource table columns error, datasourceId:{}.", dataSource.getId(), e);
                throw new ServiceException(Status.DATASOURCE_CONNECT_FAILED);
            } finally {
                closeResult(rs);
                closeResult(primaryKeyRs);
                releaseConnection(connection);
            }
        }
        return columnList;
    }

    @Override
    public List<ParamsOptions> getDatabases(Integer datasourceId) {

        DataSource dataSource = dataSourceMapper.selectById(datasourceId);

        if (dataSource == null) {
            throw new ServiceException(Status.QUERY_DATASOURCE_ERROR);
        }

        List<String> tableList;
        BaseConnectionParam connectionParam =
                (BaseConnectionParam) DataSourceUtils.buildConnectionParams(
                        dataSource.getType(),
                        dataSource.getConnectionParams());

        if (null == connectionParam) {
            throw new ServiceException(Status.DATASOURCE_CONNECT_FAILED);
        }

        Connection connection =
                DataSourceUtils.getConnection(dataSource.getType(), connectionParam);
        ResultSet rs = null;

        try {
            if (null == connection) {
                throw new ServiceException(Status.DATASOURCE_CONNECT_FAILED);
            }
            if (dataSource.getType() == DbType.POSTGRESQL) {
                rs = connection.createStatement().executeQuery(Constants.DATABASES_QUERY_PG);
            } else {
                rs = connection.createStatement().executeQuery(Constants.DATABASES_QUERY);
            }
            tableList = new ArrayList<>();
            while (rs.next()) {
                String name = rs.getString(1);
                tableList.add(name);
            }
        } catch (Exception e) {
            log.error("Get databases error, datasourceId:{}.", datasourceId, e);
            throw new ServiceException(Status.GET_DATASOURCE_TABLES_ERROR);
        } finally {
            closeResult(rs);
            releaseConnection(connection);
        }

        List<ParamsOptions> options = getParamsOptions(tableList);
        return options;
    }

    @Override
    public DataSource getDataSourceById(Integer datasourceId) {
        return dataSourceMapper.selectById(datasourceId);
    }

    @Override
    public List<ParamsOptions> getItems(Integer datasourceId) {
        //获取数据源
        DataSource dataSource = dataSourceMapper.selectById(datasourceId);
        //转化连接参数
        ConnectionParam connectionParam = DataSourceUtils.buildConnectionParams(dataSource.getType(), dataSource.getConnectionParams());
        //获取
        List<String> itemList = DataSourceUtils.getItems(dataSource.getType(), connectionParam);
        List<ParamsOptions> optionsList = new ArrayList<>();
        if (ObjectUtils.isNotEmpty(itemList)) {
            itemList.forEach(item -> optionsList.add(new ParamsOptions(item, item, false)));
        }
        return optionsList;
    }

    @Override
    public Map<String, List<DbTypeVo>> getDbType() {
        return Arrays.stream(DbType.values())
                .filter(e -> e.getServerType() != null && !e.getServerType().isEmpty())
                .collect(Collectors.groupingBy(
                        DbType::getServerType,
                        Collectors.mapping(
                                dbType -> new DbTypeVo(
                                        dbType.getCode(),
                                        dbType.getName(),
                                        dbType.getDescp(),
                                        dbType.getServerType()
                                ),
                                Collectors.toList()
                        )
                ));
    }

    @Override
    public <T> List<T> executeQuery(int datasourceId, String sql, RowMapper<T> rowMapper) {
        DataSource dataSource = getDataSourceById(datasourceId);
        if (dataSource == null) {
            throw new ServiceException(Status.DATASOURCE_NOT_EXIST);
        }
        try {
            BaseConnectionParam connectionParam = (BaseConnectionParam) DataSourceUtils.buildConnectionParams(
                    dataSource.getType(),
                    dataSource.getConnectionParams()
            );
            try (Connection connection = DataSourceUtils.getConnection(dataSource.getType(), connectionParam);
                 PreparedStatement ps = connection.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                List<T> results = new ArrayList<>();
                int rowNum = 0;
                while (rs.next()) {
//                    results.add(rowMapper.mapRow(rs, rowNum++));
                    try {
                        T result = rowMapper.mapRow(rs, rowNum);
                        if (result != null) {
                            results.add(result);
                        }
                        rowNum++;
                    } catch (Exception e) {
                        log.warn("处理第{}行数据时出错，跳过此行。错误: {}", rowNum, e.getMessage());
                        rowNum++;
                    }
                }
                return results;
            }
        } catch (Exception e) {
            log.error("执行Sql查询失败, datasourceId: {}, sql: {}", datasourceId, sql, e);
            throw new ServiceException("Doris查询失败: " + e.getMessage());
        }
    }

    @Override
    public int executeUpdate(int datasourceId, String sql) {
        DataSource dataSource = getDataSourceById(datasourceId);
        if (dataSource == null) {
            throw new ServiceException(Status.DATASOURCE_NOT_EXIST);
        }
        try {
            BaseConnectionParam connectionParam = (BaseConnectionParam) DataSourceUtils.buildConnectionParams(
                    dataSource.getType(),
                    dataSource.getConnectionParams()
            );
            try (Connection connection = DataSourceUtils.getConnection(dataSource.getType(), connectionParam);
                 Statement stmt = connection.createStatement()) {
                return stmt.executeUpdate(sql);
            }
        } catch (Exception e) {
            log.error("执行Sql更新失败, datasourceId: {}, sql: {}", datasourceId, sql, e);
            throw new ServiceException("Doris更新失败: " + e.getMessage());
        }
    }

    private List<ParamsOptions> getParamsOptions(List<String> columnList) {
        List<ParamsOptions> options = null;
        if (CollectionUtils.isNotEmpty(columnList)) {
            options = new ArrayList<>();

            for (String column : columnList) {
                ParamsOptions childrenOption =
                        new ParamsOptions(column, column, false);
                options.add(childrenOption);
            }
        }
        return options;
    }

    private String getDbSchemaPattern(DbType dbType, String schema, BaseConnectionParam connectionParam) {
        if (dbType == null) {
            return null;
        }
        String schemaPattern = null;
        switch (dbType) {
            case HIVE:
                schemaPattern = connectionParam.getDatabase();
                break;
            case ORACLE:
                schemaPattern = connectionParam.getUser();
                if (null != schemaPattern) {
                    schemaPattern = schemaPattern.toUpperCase();
                }
                break;
            case SQLSERVER:
                schemaPattern = "dbo";
                break;
            case CLICKHOUSE:
            case DATABEND:
            case PRESTO:
                break;
            default:
                schemaPattern = schema;
                break;
        }
        return schemaPattern;
    }

    private static void releaseConnection(Connection connection) {
        if (null != connection) {
            try {
                connection.close();
            } catch (Exception e) {
                log.error("Connection release error", e);
            }
        }
    }

    private static void closeResult(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception e) {
                log.error("ResultSet close error", e);
            }
        }
    }

}
