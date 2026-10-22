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

package org.apache.dolphinscheduler.plugin.task.yuenhezi;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.*;
import org.apache.dolphinscheduler.plugin.task.api.enums.ResourceType;
import org.apache.dolphinscheduler.plugin.task.api.parameters.AbstractParameters;
import org.apache.dolphinscheduler.plugin.task.api.parameters.resource.DataSourceParameters;
import org.apache.dolphinscheduler.plugin.task.yuenhezi.dto.*;
import org.apache.dolphinscheduler.spi.datasource.DefaultConnectionParam;
import org.apache.dolphinscheduler.spi.enums.YunheziVersionType;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.EXIT_CODE_FAILURE;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.EXIT_CODE_SUCCESS;

@Slf4j
public class YunheziTask extends AbstractTask {
    // 默认批处理大小
    private static final int DEFAULT_BATCH_SIZE = 500;
    // 默认并行度
    private static final int DEFAULT_PARALLELISM = 4;

    private YunheziParameters yunheziParameters;

    /**
     * taskExecutionContext
     */
    protected TaskExecutionContext taskExecutionContext;
    // 默认并行度
    private ExecutorService executorService;

    private Connection conn;

    /**
     * 生成雪花id
     */
    private Snowflake snowflake = IdUtil.getSnowflake(1, 1);

    // 配置日期格式
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            // 忽略null值字段
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
            // 忽略空数组/集合
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY)
            .setDateFormat(dateFormat)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String FILE_UPDATE_SQL =
            "  UPDATE metadata_file SET parent_id =?,  ref_fld_id  = ?, ref_parent_id =?, file_name = ?, file_path = ?, " +
                    "file_id_path =? ,fld_type =? ,structure_type = ?, file_type = ?, " +
                    "             charset = ?, source_id = ?, last_update_time = ?, " +
                    "             update_by = ?, update_time = ? , file_size = ? WHERE id = ? ";

//    private static final String FILE_UPDATE_SQL =
//            "  UPDATE metadata_file_yunhezi SET parent_id =?,  ref_fld_id  = ?, ref_parent_id =?, file_name = ?, file_path = ?, " +
//                    "file_id_path =? ,fld_type =? ,structure_type = ?, file_type = ?, " +
//                    "             charset = ?, source_id = ?, last_update_time = ?, " +
//                    "             update_by = ?, update_time = ?  WHERE id = ? ";

    private static final String FILE_DELETE_SQL = "DELETE FROM metadata_file WHERE id = ?";

    //    private static final String FILE_DELETE_SQL = "DELETE FROM metadata_file_yunhezi WHERE id = ?";
    private static final String FILE_INSERT_SQL =
            "  INSERT INTO metadata_file (id,parent_id,ref_fld_id,ref_parent_id,file_name, file_path, file_id_path, fld_type,structure_type, file_type, charset, source_id, " +
                    " last_update_time, create_by, create_time, update_by, update_time, file_size) " +
                    "  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";

//    private static final String FILE_INSERT_SQL =
//            "  INSERT INTO metadata_file_yunhezi (id,parent_id,ref_fld_id,ref_parent_id,file_name, file_path, file_id_path, fld_type,structure_type, file_type, charset, source_id, " +
//                    " last_update_time, create_by, create_time, update_by, update_time) " +
//                    "  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";

    public YunheziTask(TaskExecutionContext taskExecutionContext) {
        super(taskExecutionContext);
        this.taskExecutionContext = taskExecutionContext;
    }

    @Override
    public void init() {
        yunheziParameters = JSONUtils.parseObject(taskExecutionContext.getTaskParams(), YunheziParameters.class);
        log.info("Initializing yunhezi task with params: {}, lastestStartTime :{} , userId :{} ", JSONUtils.toPrettyJsonString(yunheziParameters),
                taskExecutionContext.getLatestHistoryStartTime(), taskExecutionContext.getUserId());

        if (yunheziParameters == null || !yunheziParameters.checkParameters()) {
            throw new TaskException("yunhezi task params is not valid");
        }

        // 初始化线程池
        int parallelism = Math.min(
                Runtime.getRuntime().availableProcessors() * 2,
                DEFAULT_PARALLELISM
        );
        this.executorService = Executors.newFixedThreadPool(parallelism);
    }

    @Override
    public void handle(TaskCallBack taskCallBack) throws TaskException {
        try {
            log.error("云盒子开始执行");
            setExitStatusCode(EXIT_CODE_SUCCESS);

            initializeDatabaseConnection();

            //todo 增加获取token功能

            try {
                // 开启事务
                conn.setAutoCommit(false);

                // 获取token
                // 处理目录信息并实时入库
                processBucketsAndSaveToDatabase();

                // 提交事务
                conn.commit();
                log.info("The YUNHEZI synchronization task has been completed, and the data has been real-time stored in the database.");

            } catch (Exception e) {
                // 回滚事务
//                if (conn != null) {
//                    conn.rollback();
//                }
                throw new TaskException("Database operation failed", e);
            }

        } catch (Exception e) {
            log.error("YUNHEZI task execution failed", e);
            setExitStatusCode(EXIT_CODE_FAILURE);
            throw new TaskException("Execute YUNHEZI task failed", e);
        } finally {
            cleanup();
        }
    }

    private void initializeDatabaseConnection() throws SQLException, ClassNotFoundException, UnsupportedEncodingException {
        conn = getConnection();
        log.info("YUNHEZI connection initialization succeeded");
    }

    public void processBucketsAndSaveToDatabase() {
        // 获取列表
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            //-1:公司文档根目录
            ConnectParamDTO connectParamDTO = getDataSource();
            String version = connectParamDTO.getVersion();
            YunheziVersionType yunheziVersionType = YunheziVersionType.getByName(version);

            //获取token
            String apiToken = getYunheziToken(httpClient, connectParamDTO, yunheziVersionType);
            log.info("get yunhezi Token: {}", apiToken);
            List<YunheziRootFile> rootFiles = yunheziParameters.getRootFiles();
            String baseUrl = buildRequestUrl(connectParamDTO.getProtocol(), connectParamDTO.getHost(), connectParamDTO.getPort(), yunheziVersionType.getGetDirListUrl());
            for (YunheziRootFile rootFile : rootFiles) {
                queryFoldersRecursively(httpClient, rootFile, apiToken, baseUrl);
            }

        } catch (IOException e) {
            log.error("Failed to initialize database connection", e);
            throw new TaskException("Failed to initialize database connection", e);
        }
    }

    private String getYunheziToken(CloseableHttpClient httpClient,
                                   ConnectParamDTO httpParam,
                                   YunheziVersionType yunheziVersionType) {
        String tokenUrl = buildRequestUrl(httpParam.getProtocol(), httpParam.getHost(), httpParam.getPort(), yunheziVersionType.getTokenUrl());
        Long timestamp = System.currentTimeMillis();
        String tokenParam = tokenUrl + "?appId=" + httpParam.getAppId() +
                "&account=" + httpParam.getAccount() +
                "&timestamp=" + timestamp +
                "&sign=" + generateSignature(httpParam.getAppId(), httpParam.getAccount(), timestamp, httpParam.getAppSecret());
        HttpPost post = new HttpPost(tokenParam);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 200) {
                String responseBody = EntityUtils.toString(response.getEntity());
//                log.info("获取云盒子返回的值: {}", responseBody);

                // 直接获取 accessToken
                Integer isSuccess = JsonPath.read(responseBody, yunheziVersionType.getResultCondition());
                log.info("HTTP connectivity check - Host: {}, Status: {}, resultCondition:{}",
                        httpParam.getHost(),
                        statusCode, isSuccess);
                if (-1 != isSuccess) {
                    throw new TaskException("Failed to get access token");
                }

                String accessToken = JsonPath.read(responseBody, yunheziVersionType.getTokenPattern());
                return accessToken;
            }
            return null;
        } catch (IOException e) {
            log.error("Failed to yunhezi connection", e);
            throw new RuntimeException(e);
        } finally {
            post.releaseConnection();
        }
    }

    /**
     * 生成签名
     *
     * @param appId     应用ID
     * @param account   账号
     * @param timestamp 时间戳（毫秒）
     * @param appSecret 应用密钥
     * @return MD5签名字符串（32位小写）
     */
    public String generateSignature(String appId, String account, long timestamp, String appSecret) {
        // 拼接字符串
        String content = appId + account + timestamp + appSecret;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(content.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    /**
     * 字节数组转16进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String buildRequestUrl(String protocol, String host, Integer port, String endpoint) {
        if (StringUtils.isBlank(host)) {
            throw new IllegalArgumentException("Host cannot be null or empty");
        }
        if (StringUtils.isEmpty(protocol)) {
            protocol = "http";
        }
        // 处理端口
        String portPart = (port != null && port != 80 && port != 443) ? ":" + port : "";
        // 处理端点路径
        String path = StringUtils.defaultString(endpoint, "");
        path = path.startsWith("/") ? path : "/" + path;
        // 构建完整URL
        return protocol.toLowerCase(Locale.ROOT) + "://" + host + portPart + path;
    }


    private ConnectParamDTO getDataSource() {
        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                .getResourceParameters(ResourceType.DATASOURCE, yunheziParameters.getDatasourceId());

        log.debug("Connect YUNHEZI datasource: {}", JSONUtils.toJsonString(dbSource));
        String connectionParams = dbSource.getConnectionParams();
        ConnectParamDTO connectParamDTO = JSONUtils.parseObject(connectionParams, ConnectParamDTO.class);
        // 构建完整URL
        return connectParamDTO;
    }

    private void queryFoldersRecursively(CloseableHttpClient httpClient, YunheziRootFile rootFile, String apiToken, String baseUrl) {
        // 使用队列进行迭代处理，避免递归
        FileInfoDto fileInfoEntity = initFileIdNameMap(rootFile);
        Queue<QueueFile> queue = new LinkedList<>();
        QueueFile queueStartFile = new QueueFile();
        // 采集数据库ref_file_id
        queueStartFile.setRefFileId(rootFile.getFldId());
        // 数据库的parent_id
        queueStartFile.setId(fileInfoEntity.getId());
        queueStartFile.setFilePath("/" + fileInfoEntity.getFileName());
        queueStartFile.setFileIdPath("/" + fileInfoEntity.getId());
        queue.offer(queueStartFile);


        List<FileInfoDto> addFileInfos = new ArrayList<>();
        List<FileInfoDto> updateFileInfos = new ArrayList<>();
        List<Long> deleteIds = null;
        while (!queue.isEmpty()) {
            QueueFile queueFile = queue.poll();

            try {
                Long currentRefFileId = queueFile.getRefFileId();
                String url = String.format(baseUrl, currentRefFileId);
                HttpGet request = new HttpGet(url);

                // 添加Authorization请求头
                request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken);
                request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    String responseBody = EntityUtils.toString(response.getEntity());
//                    log.info("获取云盒子返回的值: {}", responseBody);
                    YunheziResponse yunheziResponse = objectMapper.readValue(responseBody, YunheziResponse.class);

                    // 检查result字段
                    ResultStatus resultNode = yunheziResponse.getResult();
                    if (resultNode.getResult() != -1) {
                        log.info("Failed to query files from YUNHezi ，fid: " + currentRefFileId + ", response: " + resultNode.getResult());
                        continue;
                    }

                    // 处理list数组
                    List<YunheziFileInfoDto> yunheziFileInfoDtoList = yunheziResponse.getList();
                    if (CollectionUtils.isEmpty(yunheziFileInfoDtoList)) {
                        continue;
                    }

//                    log.info("get files from YUNHEzi. currentFid： {} ，size: {}", currentRefFileId, yunheziFileInfoDtoList.size());
                    Map<Long, ExitFileDto> exitFiles = checkExistenceFileBatch(conn, queueFile.getId());
                    List<Long> updateFileIds = new ArrayList<>();
                    // 过滤添加和更新
                    for (YunheziFileInfoDto fileInfoDto : yunheziFileInfoDtoList) {
                        Long fileId = null;
                        Long updateFileId = null;
                        if (!MapUtils.isEmpty(exitFiles) && exitFiles.get(fileInfoDto.getFid()) != null) {
                            // 更新操作
                            ExitFileDto exitFile = exitFiles.get(fileInfoDto.getFid());
                            fileId = exitFile.getId();
                            updateFileId = fileId;

                            boolean isUpdateOrAdd = true;
                            if (exitFile.getLastUpdateTime() != null && fileInfoDto.getLastOperateTime() != null) {
                                LocalDateTime synsStartTime = fileInfoDto.getLastOperateTime()
                                        .toInstant()
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDateTime();
                                // 采集回来数据库时间 大于 数据库时间
                                if (synsStartTime.compareTo(exitFile.getLastUpdateTime()) >= 0) {
//                                    log.error("synsStartTime: " + synsStartTime + "lastUpdateTime" + exitFile.getLastUpdateTime()+ " (id: " + fileInfoDto.getFid() + ") has not been updated");
                                    isUpdateOrAdd = false;
                                }
                            }
                            if(isUpdateOrAdd){
                                FileInfoDto fileInfo = createFileInfoDto(fileInfoDto, exitFile.getId(), queueFile.getId(), queueFile.getFilePath(),queueFile.getFileIdPath());
                                if (null == fileInfo) {
                                    continue;
                                }

                                if (updateFileInfos.size() < DEFAULT_BATCH_SIZE) {
                                    updateFileInfos.add(fileInfo);
                                } else {
                                    saveUpdateRealTime(updateFileInfos,currentRefFileId);
                                }
                            }
                        } else {
                            fileId = snowflake.nextId();

                            FileInfoDto fileInfo = createFileInfoDto(fileInfoDto, fileId, queueFile.getId(), queueFile.getFilePath(),queueFile.getFileIdPath());
                            if (null == fileInfo) {
                                continue;
                            }
                            if (addFileInfos.size() < DEFAULT_BATCH_SIZE) {
                                addFileInfos.add(fileInfo);
                            } else {
                                saveAddRealTime(addFileInfos,currentRefFileId);
                            }
                        }
                        // 如果fType=2，添加到队列继续处理
                        int fType = fileInfoDto.getFType();
                        if (fType == 2) {
                            Long subFid = fileInfoDto.getFid();
                            QueueFile queueFileRes = new QueueFile();
                            queueFileRes.setId(fileId);
                            queueFileRes.setRefFileId(subFid);
                            queueFileRes.setFilePath(queueFile.getFilePath() + "/" + fileInfoDto.getFName());
                            queueFileRes.setFileIdPath(queueFile.getFileIdPath() + "/" + fileId);
                            queue.offer(queueFileRes);
                        }

                        updateFileIds.add(updateFileId);
                    }

                    // 过滤删除,数据库中有，但云盒子没有
                    if (!MapUtils.isEmpty(exitFiles) && !CollectionUtils.isEmpty(updateFileIds)) {
                        deleteIds = new ArrayList<>();
                        for (ExitFileDto exitFile : exitFiles.values()) {
//                            log.info("exitFile: {}", JSONUtils.toJsonString(exitFile));
                            if (!updateFileIds.contains(exitFile.getId())) {
                                deleteIds.add(exitFile.getId());
                            }
                        }
                        saveDeleteRealTime(deleteIds,currentRefFileId);
                    }
                }
            } catch (IOException | SQLException e) {
                log.error("Failed to query files. fid " + queueFile.getRefFileId() + ": ", e);
            }
        }
        try {
            // 处理剩余的
            if (CollectionUtils.isNotEmpty(addFileInfos)) {
                saveAddRealTime(addFileInfos,null);
            }
            if (CollectionUtils.isNotEmpty(updateFileInfos)) {
                saveUpdateRealTime(updateFileInfos,null);
            }

            if (CollectionUtils.isNotEmpty(deleteIds)) {
                saveDeleteRealTime(deleteIds,null);
            }
        } catch (SQLException e) {
            log.error("Last files input to database. fid ", e);
        }

    }

    private FileInfoDto initFileIdNameMap(YunheziRootFile startFid) {
        FileInfoDto rootFile = null;
        try {
            Long fileId = snowflake.nextId();
            // 根目录入库
            rootFile = new FileInfoDto(
                    fileId,
                    0L,
                    startFid.getFldId(),
                    -1L,
                    startFid.getFldName(),
                    "/",
                    "/",
                    2,
                    "",
                    "",
                    null,
                    String.valueOf(yunheziParameters.getDatasourceId()),
                    LocalDateTime.now(),
                    taskExecutionContext.getUserId(),null);
            List<FileInfoDto> fileInfoDtos = new ArrayList<>();
            fileInfoDtos.add(rootFile);
            ExitFileDto exitFile = isExistFile(startFid.getFldId());
            if (null == exitFile) {
                saveAddRealTime(fileInfoDtos,startFid.getFldId());
            } else {
                rootFile.setId(exitFile.getId());
            }

        } catch (SQLException e) {
            log.error("Failed to initialize fileIdNameMap. startFid " + startFid + ": ", e);
            throw new TaskException("Failed to initialize fileIdNameMap", e);
        }
        return rootFile;
    }

    private ExitFileDto isExistFile(Long refFldId) throws SQLException {
        Map<Long, ExitFileDto> exitFiles = checkExistenceFileBatch(conn, 0L);
        if (MapUtils.isEmpty(exitFiles)) {
            return null;
        }
        for (Long databaseRefFldId : exitFiles.keySet()) {
            if (Objects.equals(databaseRefFldId, refFldId)) {
                return exitFiles.get(refFldId);
            }
        }
        return null;
    }

    private void saveAddRealTime(List<FileInfoDto> addFileInfoDtos, Long refFileId) throws SQLException {
        // 准备插入和更新语句
        try (PreparedStatement insertStmt = conn.prepareStatement(FILE_INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            for (FileInfoDto entity : addFileInfoDtos) {
                // 插入操作
                setFileInsertParameters(insertStmt, entity);
                insertStmt.addBatch();
            }
            int[] insertResults = insertStmt.executeBatch();
            insertStmt.clearBatch();
            conn.commit(); // 需要手动提交
            addFileInfoDtos.clear();
            // 清空md5Hash列表
//            log.debug("Current :{} Batch operation completed: {} records add", refFileId,updateResults.length);
        }

    }

    private void saveUpdateRealTime(List<FileInfoDto> updateFileInfoDtos, Long refFileId) throws SQLException {

        // 准备插入和更新语句
        try (PreparedStatement updateStmt = conn.prepareStatement(FILE_UPDATE_SQL)) {

            // 过滤添加和更新
            for (FileInfoDto entity : updateFileInfoDtos) {
                // 更新操作
                setFileUpdateParameters(updateStmt, entity);
                updateStmt.addBatch();
            }
            int[] insertResults = updateStmt.executeBatch();
            updateStmt.clearBatch();
            conn.commit(); // 需要手动提交
            updateFileInfoDtos.clear();
//            log.debug("Current :{} Batch operation completed: {} records update", refFileId,insertResults.length);
        }

    }

    private void saveDeleteRealTime(List<Long> deleteIds, Long refFileId) throws SQLException {
        if (CollectionUtils.isEmpty(deleteIds)) {
            return;
        }
        // 准备插入和更新语句
        try (PreparedStatement deleteStmt = conn.prepareStatement(FILE_DELETE_SQL)) {
            // 过滤删除
            if (CollectionUtils.isNotEmpty(deleteIds)) {
                for (Long id : deleteIds) {
                    deleteStmt.setLong(1, id);
                    deleteStmt.addBatch();
                }
                int[] deleteResults = deleteStmt.executeBatch();
                deleteStmt.clearBatch();
                deleteIds.clear();
                log.debug("Current :{} Batch operation completed: {} records delete", refFileId,deleteResults.length);
            }
        }

    }


    private Map<Long, ExitFileDto> checkExistenceFileBatch(Connection conn, Long parentId) throws SQLException {
        String sql = "SELECT id, ref_fld_id, last_update_time FROM metadata_file WHERE parent_id = ? and source_id = ?";
//        String sql = "SELECT id, ref_fld_id, last_update_time FROM metadata_file_yunhezi WHERE parent_id = ? and source_id = ?";
//        log.info("数据库存在文件: parent_id: {},----source_id:{}", parentId, yunheziParameters.getDatasourceId());
        Map<Long, ExitFileDto> result;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, String.valueOf(parentId));
            stmt.setString(2, String.valueOf(yunheziParameters.getDatasourceId()));
            try (ResultSet rs = stmt.executeQuery()) {
                result = new HashMap<>();
                while (rs.next()) {
                    ExitFileDto fileInfoDto = new ExitFileDto();
                    fileInfoDto.setId(rs.getLong("id"));
                    fileInfoDto.setRefFileId(rs.getLong("ref_fld_id"));
                    fileInfoDto.setLastUpdateTime(rs.getTimestamp("last_update_time").toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDateTime());

                    result.put(rs.getLong("ref_fld_id"), fileInfoDto);
                }
            }
        }
//        log.info("dbMetadataFile: parent_id: {},----source_id:{}, -----result:{}", parentId, yunheziParameters.getDatasourceId(), result.size());
        return result;
    }

    // 以下为辅助方法，保持原有逻辑不变
    private FileInfoDto createFileInfoDto(YunheziFileInfoDto yunheziFileInfoDto, Long fileId, Long parentId, String filePath,String fileIdPath) {
        String extension = yunheziFileInfoDto.getExtName();
        LocalDateTime lastModified = yunheziFileInfoDto.getLastOperateTime() != null ?
                yunheziFileInfoDto.getLastOperateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null;
        FileTypeEnum fileType = FileTypeEnum.getFileType(extension);


        String fileName = yunheziFileInfoDto.getFName();

        // todo 增加云盒子数据源
        FileInfoDto fileInfoDto = new FileInfoDto(
                fileId,
                parentId,
                yunheziFileInfoDto.getFid(),
                yunheziFileInfoDto.getParentFolderId(),
                fileName,
                filePath,
                fileIdPath,
                yunheziFileInfoDto.getFType(),
                fileType.getStructureType(),
                fileType.getFileType(),
                null,
                String.valueOf(yunheziParameters.getDatasourceId()),
                lastModified,
                taskExecutionContext.getUserId(),
                yunheziFileInfoDto.getFileSize());
//        log.error("【FILE_INFO】fileInfoDto: {}", JSONUtils.toJsonString(fileInfoDto));
        return fileInfoDto;
    }

    /**
     * 设置插入参数
     * "  INSERT INTO metadata_file (id,parent_id,ref_fld_id,ref_parent_id,file_name, file_path, file_id_path, fld_type,structure_type, file_type, charset, source_id, " +
     * " last_update_time, create_by, create_time, update_by, update_time, md5_hash) " +
     * "  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";
     */
    private void setFileInsertParameters(PreparedStatement stmt, FileInfoDto fileInfo) throws SQLException {
        stmt.setString(1, String.valueOf(fileInfo.getId()));
        stmt.setString(2, String.valueOf(fileInfo.getParentId()));
        stmt.setString(3, String.valueOf(fileInfo.getRefFldId()));
        stmt.setString(4, String.valueOf(fileInfo.getRefParentId()));
        stmt.setString(5, fileInfo.getFileName());
        stmt.setString(6, fileInfo.getFilePath());
        stmt.setString(7, fileInfo.getFileIdPath());
        stmt.setInt(8, fileInfo.getFldType());
        stmt.setString(9, fileInfo.getStructureType());
        stmt.setString(10, fileInfo.getFileType());
        stmt.setString(11, fileInfo.getCharset());
        stmt.setString(12, fileInfo.getSourceId());
        java.sql.Timestamp lastTimestamp = java.sql.Timestamp.valueOf(fileInfo.getLastUpdateTime());
        stmt.setTimestamp(13, lastTimestamp);
        stmt.setString(14, fileInfo.getCreateBy());
        java.sql.Timestamp createTime = java.sql.Timestamp.valueOf(fileInfo.getCreateTime());
        stmt.setTimestamp(15, createTime);
        stmt.setString(16, fileInfo.getUpdateBy());
        java.sql.Timestamp updateTime = java.sql.Timestamp.valueOf(fileInfo.getUpdateTime());
        stmt.setTimestamp(17, updateTime);
        Long fileSize = fileInfo.getFileSize() == null ? 0 : fileInfo.getFileSize();
        stmt.setLong(18, fileSize);
    }

    /**
     * 设置更新参数
     */
    private void setFileUpdateParameters(PreparedStatement stmt, FileInfoDto fileInfo) throws SQLException {
        stmt.setString(1, String.valueOf(fileInfo.getParentId()));
        stmt.setString(2, String.valueOf(fileInfo.getRefFldId()));
        stmt.setString(3, String.valueOf(fileInfo.getRefParentId()));
        stmt.setString(4, fileInfo.getFileName());
        stmt.setString(5, fileInfo.getFilePath());
        stmt.setString(6, fileInfo.getFileIdPath());
        stmt.setInt(7, fileInfo.getFldType());
        stmt.setString(8, fileInfo.getStructureType());
        stmt.setString(9, fileInfo.getFileType());
        stmt.setString(10, fileInfo.getCharset());
        stmt.setString(11, fileInfo.getSourceId());
        java.sql.Timestamp lastTimestamp = java.sql.Timestamp.valueOf(fileInfo.getLastUpdateTime());
        stmt.setTimestamp(12, lastTimestamp);
        stmt.setString(13, fileInfo.getUpdateBy());
        java.sql.Timestamp updateTime = java.sql.Timestamp.valueOf(fileInfo.getUpdateTime());
        stmt.setTimestamp(14, updateTime);
        Long fileSize = fileInfo.getFileSize() == null ? 0 : fileInfo.getFileSize();
        stmt.setLong(15, fileSize);
        stmt.setString(16, String.valueOf(fileInfo.getId()));
    }

    private Connection getConnection() throws ClassNotFoundException, UnsupportedEncodingException, SQLException {
        // 获取数据源配置
        DefaultDataSourceExecutionContext executionContext = taskExecutionContext.getDefaultDataSourceExecutionContext();
        DefaultConnectionParam connectionParams = JSONUtils.parseObject(executionContext.getConnectionParams(), DefaultConnectionParam.class);
        log.info("BaseConnectionParam: {}", connectionParams);
        String datasourceUrl = connectionParams.getJdbcUrl();
        String driverClassName = connectionParams.getDriverClassName();
        Map<String, String> other = connectionParams.getOther();
        // 加载数据库驱动
        Class.forName(driverClassName);
        Properties props = new Properties();
        props.setProperty("user", connectionParams.getUser());
        props.setProperty("password", connectionParams.getPassword());
        if (null != other.get("useSSL")) {
            props.setProperty("useSSL", other.get("useSSL")); // 禁用SSL
        }
        if (null != other.get("serverTimezone")) {
            String timezone = URLDecoder.decode(other.get("serverTimezone"), StandardCharsets.UTF_8.name());
            props.setProperty("serverTimezone", timezone); // 设置时区
        }
        if (null != other.get("useUnicode")) {
            props.setProperty("useUnicode", other.get("useUnicode")); // 使用Unicode
        }

        if (null != other.get("characterEncoding")) {
            props.setProperty("characterEncoding", other.get("characterEncoding")); // 字符编码
        }

        if (null != other.get("zeroDateTimeBehavior")) {
            props.setProperty("zeroDateTimeBehavior", other.get("zeroDateTimeBehavior"));
        }
        return DriverManager.getConnection(datasourceUrl, props);
    }

    private void cleanup() {
        if (executorService != null) {
            try {
                executorService.shutdown();
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                executorService.shutdownNow();
            }
        }
    }


    @Override
    public void cancel() throws TaskException {
        cleanup();
        setExitStatusCode(EXIT_CODE_FAILURE);
        throw new TaskException("MinIO task was cancelled");
    }

    @Override
    public AbstractParameters getParameters() {
        return yunheziParameters;
    }
}