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

package org.apache.dolphinscheduler.plugin.datasource.opengauss.param;

import com.alibaba.druid.sql.parser.SQLParserUtils;
import com.google.auto.service.AutoService;
import org.apache.commons.collections4.MapUtils;
import org.apache.dolphinscheduler.common.constants.Constants;
import org.apache.dolphinscheduler.common.constants.DataSourceConstants;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.AbstractDataSourceProcessor;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.BaseDataSourceParamDTO;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.DataSourceProcessor;
import org.apache.dolphinscheduler.plugin.datasource.api.utils.PasswordUtils;
import org.apache.dolphinscheduler.spi.datasource.BaseConnectionParam;
import org.apache.dolphinscheduler.spi.datasource.ConnectionParam;
import org.apache.dolphinscheduler.spi.enums.DbType;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@AutoService(DataSourceProcessor.class)
public class OpenGaussDataSourceProcessor extends AbstractDataSourceProcessor {

    @Override
    public BaseDataSourceParamDTO castDatasourceParamDTO(String paramJson) {
        return JSONUtils.parseObject(paramJson, OpenGaussDataSourceParamDTO.class);
    }

    @Override
    public BaseDataSourceParamDTO createDatasourceParamDTO(String connectionJson) {
        OpenGaussConnectionParam connectionParams = (OpenGaussConnectionParam) createConnectionParams(connectionJson);
        OpenGaussDataSourceParamDTO openGaussDatasourceParamDTO = new OpenGaussDataSourceParamDTO();
        openGaussDatasourceParamDTO.setDatabase(connectionParams.getDatabase());
        openGaussDatasourceParamDTO.setUserName(connectionParams.getUser());
        openGaussDatasourceParamDTO.setOther(connectionParams.getOther());

        String address = connectionParams.getAddress();
        String[] hostSeperator = address.split(Constants.DOUBLE_SLASH);
        String[] hostPortArray = hostSeperator[hostSeperator.length - 1].split(Constants.COMMA);
        openGaussDatasourceParamDTO.setHost(hostPortArray[0].split(Constants.COLON)[0]);
        openGaussDatasourceParamDTO.setPort(Integer.parseInt(hostPortArray[0].split(Constants.COLON)[1]));

        return openGaussDatasourceParamDTO;
    }

    @Override
    public BaseConnectionParam createConnectionParams(BaseDataSourceParamDTO dataSourceParam) {
        OpenGaussDataSourceParamDTO openGaussParam = (OpenGaussDataSourceParamDTO) dataSourceParam;
        String address = String.format("%s%s:%s", DataSourceConstants.JDBC_OPENGAUSS, openGaussParam.getHost(),
                openGaussParam.getPort());
        String jdbcUrl = String.format("%s/%s", address, openGaussParam.getDatabase());

        OpenGaussConnectionParam openGaussConnectionParam = new OpenGaussConnectionParam();
        openGaussConnectionParam.setJdbcUrl(jdbcUrl);
        openGaussConnectionParam.setAddress(address);
        openGaussConnectionParam.setDatabase(openGaussParam.getDatabase());
        openGaussConnectionParam.setUser(openGaussParam.getUserName());
        openGaussConnectionParam.setPassword(PasswordUtils.encodePassword(openGaussParam.getPassword()));
        openGaussConnectionParam.setDriverClassName(getDatasourceDriver());
        openGaussConnectionParam.setValidationQuery(getValidationQuery());
        openGaussConnectionParam.setOther(openGaussParam.getOther());
        openGaussConnectionParam.setHost(openGaussParam.getHost());
        openGaussConnectionParam.setPort(openGaussParam.getPort());

        return openGaussConnectionParam;
    }

    @Override
    public ConnectionParam createConnectionParams(String connectionJson) {
        return JSONUtils.parseObject(connectionJson, OpenGaussConnectionParam.class);
    }

    @Override
    public String getDatasourceDriver() {
        return DataSourceConstants.ORG_OPENGAUSS_DRIVER;
    }

    @Override
    public String getValidationQuery() {
        return DataSourceConstants.OPENGAUSS_VALIDATION_QUERY;
    }

    @Override
    public String getJdbcUrl(ConnectionParam connectionParam) {
        OpenGaussConnectionParam openGaussConnectionParam = (OpenGaussConnectionParam) connectionParam;
        if (MapUtils.isNotEmpty(openGaussConnectionParam.getOther())) {
            return String.format("%s?%s", openGaussConnectionParam.getJdbcUrl(),
                    transformOther(openGaussConnectionParam.getOther()));
        }
        return openGaussConnectionParam.getJdbcUrl();
    }

    @Override
    public Connection getConnection(ConnectionParam connectionParam) throws ClassNotFoundException, SQLException {
        OpenGaussConnectionParam openGaussConnectionParam = (OpenGaussConnectionParam) connectionParam;
        Class.forName(getDatasourceDriver());
        return DriverManager.getConnection(getJdbcUrl(openGaussConnectionParam),
                openGaussConnectionParam.getUser(),
                PasswordUtils.decodePassword(openGaussConnectionParam.getPassword()));
    }

    @Override
    public DbType getDbType() {
        return DbType.OPENGAUSS;
    }

    @Override
    public DataSourceProcessor create() {
        return new OpenGaussDataSourceProcessor();
    }

    @Override
    public List<String> splitAndRemoveComment(String sql) {
        String cleanSQL = SQLParserUtils.removeComment(sql, com.alibaba.druid.DbType.gaussdb);
        return SQLParserUtils.split(cleanSQL, com.alibaba.druid.DbType.gaussdb);
    }

    private String transformOther(Map<String, String> otherMap) {
        if (MapUtils.isEmpty(otherMap)) {
            return null;
        }
        List<String> otherList = new ArrayList<>();
        otherMap.forEach((key, value) -> otherList.add(String.format("%s=%s", key, value)));
        return String.join("&", otherList);
    }

}
