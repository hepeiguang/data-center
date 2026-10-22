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

package org.apache.dolphinscheduler.plugin.task.minio;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.databind.JsonNode;
import io.minio.GetBucketPolicyArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.*;
import org.apache.dolphinscheduler.plugin.task.api.enums.ResourceType;
import org.apache.dolphinscheduler.plugin.task.api.parameters.AbstractParameters;
import org.apache.dolphinscheduler.plugin.task.api.parameters.resource.DataSourceParameters;
import org.apache.dolphinscheduler.plugin.task.minio.dto.*;
import org.apache.dolphinscheduler.spi.datasource.DefaultConnectionParam;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.EXIT_CODE_FAILURE;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.EXIT_CODE_SUCCESS;

@Slf4j
public class MinioTask extends AbstractTask {
    // 默认批处理大小
    private static final int DEFAULT_BATCH_SIZE = 500;
    // 默认并行度
    private static final int DEFAULT_PARALLELISM = 4;
    // 引入雪花id
    private Snowflake snowflake = IdUtil.getSnowflake(1, 1);

    private MinioParameters minioParameters;

    /**
     * taskExecutionContext
     */
    protected TaskExecutionContext taskExecutionContext;
    private MinioClient minioClient;
    // 默认并行度
    private ExecutorService executorService;

    private Connection conn;

    // bucket 信息
    private static final String updateSql = "UPDATE metadata_file SET file_name  = ?, file_path = ?, file_regex = ?, " +
            "structure_type = ?, file_type = ? , last_update_time = ?, "+
            "fld_type = ?, file_size = ?  WHERE source_id = ? and file_path = ? ";

    // 文件列 信息
    private static final String COLUMN_INSERT_SQL =
            "INSERT INTO metadata_file_column (ref_file_id, column_name, column_type, sort_index, " +
                    "create_by, create_time, update_by, update_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ";
    private static final String COLUMN_DELETE_SQL = "DELETE FROM metadata_file_column WHERE ref_file_id = ? ";


    // 文件 信息
    private static final String FILE_UPDATE_SQL =
            "  UPDATE metadata_file SET parent_id =?,  ref_fld_id  = ?, ref_parent_id =?, file_name = ?, file_path = ?, " +
                    "file_id_path =? ,fld_type =? ,structure_type = ?, file_type = ?, " +
                    "             charset = ?, source_id = ?, last_update_time = ?, " +
                    "             update_by = ?, update_time = ?, file_size = ? ,dir_name = ?  WHERE id = ?";
    private static final String FILE_INSERT_SQL =
            "  INSERT INTO metadata_file (id,parent_id,ref_fld_id,ref_parent_id,file_name, file_path, file_id_path, fld_type,structure_type, file_type, charset, source_id, " +
                    " last_update_time, create_by, create_time, update_by, update_time, file_size, dir_name) " +
                    "  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";
    private static final String FILE_DELETE_SQL = "DELETE FROM metadata_file WHERE source_id = ? AND file_path = ? ";

    private static final String FILE_DELETE_SUBFILE_SQL = "DELETE FROM metadata_file WHERE source_id = ? AND file_path LIKE CONCAT(?,'%') ";


    public MinioTask(TaskExecutionContext taskExecutionContext) {
        super(taskExecutionContext);
        this.taskExecutionContext = taskExecutionContext;
    }

    @Override
    public void init() {
        minioParameters = JSONUtils.parseObject(taskExecutionContext.getTaskParams(), MinioParameters.class);
        log.info("Initializing Minio task with params: {}, lastestStartTime :{} , userId :{} ", JSONUtils.toPrettyJsonString(minioParameters),
                taskExecutionContext.getLatestHistoryStartTime(), taskExecutionContext.getUserId());
        if (minioParameters == null || !minioParameters.checkParameters()) {
            throw new TaskException("Minio task params is not valid");
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
            setExitStatusCode(EXIT_CODE_SUCCESS);
            // Initialize MinIO client
            initializeMinioClient();
            initializeDatabaseConnection();
            try {
                // 开启事务
                conn.setAutoCommit(false);
                // 处理桶信息并实时入库
                processBucketsAndSaveToDatabase();
                // 提交事务
                conn.commit();
                log.info("The MinIO synchronization task has been completed, and the data has been real-time stored in the database.");
            } catch (Exception e) {
                // 回滚事务
//                if (conn != null) {
//                    conn.rollback();
//                }
                throw new TaskException("Database operation failed", e);
            }
        } catch (Exception e) {
            log.error("MinIO task execution failed", e);
            setExitStatusCode(EXIT_CODE_FAILURE);
            throw new TaskException("Execute MinIO task failed", e);
        } finally {
            cleanup();
        }
    }

    private void initializeDatabaseConnection() throws SQLException, ClassNotFoundException, UnsupportedEncodingException {
        conn = getConnection();
        log.info("MinIO connection initialization succeeded");
    }

    private void processBucketsAndSaveToDatabase() throws Exception {
        // 1. 获取MinIO所有桶
        List<Bucket> buckets = minioClient.listBuckets();
        log.info("{} buckets to process have been found", buckets.size());

        if (buckets.isEmpty()) return;

        // 2. 初始化桶根目录DTOs
        List<FileInfoDto> rootDirectories = new ArrayList<>();
        for (Bucket bucket : buckets) {
            String bucketName = bucket.name();
            FileInfoDto dto = initRootDirectory(bucketName);
            rootDirectories.add(dto);
        }

        // 3. 桶新增/更新/删除
        Map<String, FileInfoExistence> dbBucketsMap = checkExistenceFileBatch(conn, rootDirectories, 0L, "/");
        List<FileInfoDto> addRoots = new ArrayList<>();
        List<FileInfoDto> updateRoots = new ArrayList<>();
        Set<String> minioBucketPaths = new HashSet<>();
        for (FileInfoDto dto : rootDirectories) {
            minioBucketPaths.add(dto.getFilePath());
            FileInfoExistence exist = dbBucketsMap.get(dto.getFilePath());
            if (exist != null) {
                dto.setId(exist.getId());
                updateRoots.add(dto);
            } else {
                addRoots.add(dto);
            }
        }

        List<String> deleteBuckets = new ArrayList<>();
        for (Map.Entry<String, FileInfoExistence> entry : dbBucketsMap.entrySet()) {
            if (!minioBucketPaths.contains(entry.getKey())) {
                deleteBuckets.add(entry.getKey());
            }
        }

        if (!addRoots.isEmpty()) saveAddRealTime(addRoots);
        if (!updateRoots.isEmpty()) saveUpdateRealTime(updateRoots);
        if (!deleteBuckets.isEmpty()) saveDeleteRealTime(Collections.emptyList(), deleteBuckets);
        // 4. 原有递归逻辑处理多层目录和文件（这会包含删除逻辑）
        for (FileInfoDto root : rootDirectories) {
            processSingleBucketRealTime(root);
        }
    }


    private FileInfoDto initRootDirectory(String bucketName) throws SQLException {
        String filePath = "/" + bucketName + "/";
        String sql = "SELECT id, parent_id FROM metadata_file WHERE source_id = ? AND file_path = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, String.valueOf(minioParameters.getSourceId()));
            stmt.setString(2, filePath);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // 使用数据库中已存在的完整信息
                    long existId = rs.getLong("id");
                    long existIdParentId = rs.getLong("parent_id");

                    return new FileInfoDto(existId, existIdParentId, existId, existIdParentId,
                            bucketName, filePath, "/", 2, "", "", "",
                            minioParameters.getSourceId().toString(), LocalDateTime.now(),
                            String.valueOf(taskExecutionContext.getUserId()), 0L, ""
                    );
                }
            }
        }
        // 只有真正的新桶才生成新ID
        long id = snowflake.nextId();
        return new FileInfoDto(id, 0L, id, 0L, bucketName, filePath, "/", 2, "", "", "",
                minioParameters.getSourceId().toString(), LocalDateTime.now(),
                String.valueOf(taskExecutionContext.getUserId()), 0L, ""
        );
    }


    private void processSingleBucketRealTime(FileInfoDto fileInfoDto) {
        try {
            log.info("start to process bucket: {}", fileInfoDto.getFileName());
            // 先检查桶根目录下是否有数据
            String bucketName = fileInfoDto.getFileName();
            Iterable<Result<Item>> rootResults = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix("")
                            .recursive(false)
                            .delimiter("/")
                            .build()
            );
//            log.debug("Folder hierarchy for {}: ID={}, ParentID={}, RefFldID={}, RefParentID={}",
//                    fileInfoDto.getFileName(), fileInfoDto.getId(), fileInfoDto.getParentId(),
//                    fileInfoDto.getRefFldId(), fileInfoDto.getRefParentId());

            boolean hasDataInRoot = false;
            for (Result<Item> result : rootResults) {
                hasDataInRoot = true;
                break; // 只要有一个数据就跳出
            }

            // 如果桶根目录没有数据，但数据库中有记录，则删除
            // 如果桶根目录没有数据，但数据库中有记录，则删除所有“子项”，不删根目录本身
            if (!hasDataInRoot) {
                Map<String, FileInfoExistence> exitFiles = getExistencesByDirPath(conn, fileInfoDto.getFilePath());
                if (!exitFiles.isEmpty()) {
//                    log.warn("桶 {} 根目录在MinIO中无数据，但数据库存在残留数据，开始清理", bucketName);
                    List<String> files = new ArrayList<>();
                    List<String> folders = new ArrayList<>();
                    for (Map.Entry<String, FileInfoExistence> e : exitFiles.entrySet()) {
                        String path = e.getKey();
                        FileInfoExistence ex = e.getValue();
                        if (path.equals(fileInfoDto.getFilePath())) {
                            continue;
                        }
                        if (ex.getFldType() == 1) {
                            files.add(path);
                        } else {
                            folders.add(path);
                        }
                    }

                    saveDeleteRealTime(files, folders);
//                    log.info("桶 {} 的子目录和文件已全部清理完毕", bucketName);
                    return;
                }
            }


            // 计算桶使用情况并获取文件信息
            calculateBucketUsageRealTime(fileInfoDto);
            log.info("Bucket {} has been processed successfully", fileInfoDto.getFileName());
        } catch (Exception e) {
            log.error("Failed to process Bucket {}", fileInfoDto.getFileName(), e);
            throw new TaskException("Failed to process Bucket ", e);
        }
    }

    private Map<String, FileInfoExistence> getExistencesByDirPath(Connection conn, String currentDirPath) throws SQLException {
        String sql = "SELECT id, file_path, fld_type FROM metadata_file WHERE file_id_path = ? AND source_id = ?";
        Map<String, FileInfoExistence> result = new HashMap<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, currentDirPath);
            stmt.setString(2, String.valueOf(minioParameters.getSourceId()));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    FileInfoExistence fexist = new FileInfoExistence();
                    fexist.setId(rs.getLong("id"));
                    fexist.setFldType(rs.getInt("fld_type"));
                    String filePath = rs.getString("file_path");
                    fexist.setFilePath(filePath);
                    result.put(filePath, fexist);
                }
            }
        }
        return result;
    }


    private void calculateBucketUsageRealTime(FileInfoDto fileInfoDto) throws Exception {
        ProcessResult result = processDirectoryRecursive(fileInfoDto);
        log.info("Processing of Bucket {} completed: {} objects, total size {}",
                fileInfoDto.getFileName(), result.objectCount, formatSize(result.totalSize));
        BucketUsageDto usage = new BucketUsageDto(result.totalSize, result.objectCount);
        // 更新桶信息到数据库
        updateBucketInfoToDatabase(fileInfoDto.getFileName(), usage);
    }

    // 方案1 通过minio的自动分页获取数据
    private ProcessResult processDirectoryRecursive(FileInfoDto root) throws Exception {
        log.info("Processing directory: bucket={}", root.getFileName());
        String bucketName = root.getFileName();

        long totalSize = 0;
        long objectCount = 0;
        int itemsProcessed = 0;
        int batchSize = minioParameters.getBatchSize() != null ? minioParameters.getBatchSize() : DEFAULT_BATCH_SIZE;

        Queue<QueueFile> directoryQueue = new LinkedList<>();
        QueueFile queueStartFile = new QueueFile();
        queueStartFile.setDirId(root.getId());
        queueStartFile.setPrefix("");
        queueStartFile.setDirPath(root.getFilePath());
        queueStartFile.setDirName(root.getFileName());
        directoryQueue.offer(queueStartFile);

        List<FileInfoDto> fileInfos = new ArrayList<>();
        List<FileInfoDto> addFileInfos = new ArrayList<>();
        List<FileInfoDto> updateFileInfos = new ArrayList<>();

        while (!directoryQueue.isEmpty()) {

            QueueFile poll = directoryQueue.poll();
            String prefix = poll.getPrefix();
            Long currentDirId = poll.getDirId();
            String currentDirPath = poll.getDirPath();
            String currentDirName = poll.getDirName();

            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .recursive(false)
                            .delimiter("/")
                            .maxKeys(batchSize)
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                itemsProcessed++;

                if (item.isDir()) {
                    FileInfoDto folderInfoDto = createFolderInfoDto(
                            item, bucketName, currentDirId, currentDirPath, currentDirName);
                    fileInfos.add(folderInfoDto);

                    QueueFile subFolder = new QueueFile();
                    subFolder.setDirId(folderInfoDto.getId());
                    subFolder.setPrefix(item.objectName());
                    subFolder.setDirPath(folderInfoDto.getFilePath());
                    subFolder.setDirName(folderInfoDto.getFileName());
                    directoryQueue.offer(subFolder);

                } else {
                    FileInfoDto dto = createFileInfoDto(
                            item, bucketName, currentDirId, currentDirPath, currentDirName);

                    if (dto != null) {
                        fileInfos.add(dto);
                        totalSize += item.size();
                        objectCount++;
                    }
                }
            }

            Map<String, FileInfoExistence> exitFiles =
                    checkExistenceFileBatch(conn, fileInfos, currentDirId, currentDirPath);

            for (FileInfoDto entity : fileInfos) {
                FileInfoExistence existence = exitFiles.get(entity.getFilePath());
                if (existence != null) {
                    entity.setId(existence.getId());
                    updateFileInfos.add(entity);
                } else {
                    addFileInfos.add(entity);
                }
            }

            if (!addFileInfos.isEmpty()) {
                saveAddRealTime(addFileInfos);
                addFileInfos.clear();
            }

            if (!updateFileInfos.isEmpty()) {
                saveUpdateRealTime(updateFileInfos);
                updateFileInfos.clear();
            }

            Set<String> currentMinioPaths = fileInfos.stream()
                    .map(FileInfoDto::getFilePath)
                    .collect(Collectors.toSet());

            List<String> deleteBatchPaths = exitFiles.keySet().stream()
                    .filter(dbPath -> !currentMinioPaths.contains(dbPath))
                    .collect(Collectors.toList());

            if (!deleteBatchPaths.isEmpty()) {
                List<String> files = new ArrayList<>();
                List<String> folders = new ArrayList<>();
                for (String p : deleteBatchPaths) {
                    FileInfoExistence ex = exitFiles.get(p);
                    if (ex.getFldType() == 2) {
                        folders.add(p);
                    } else {
                        files.add(p);
                    }
                }
                saveDeleteRealTime(files, folders);
            }
            fileInfos.clear();
        }
        return new ProcessResult(totalSize, objectCount, itemsProcessed);
    }


    private void saveAddRealTime(List<FileInfoDto> addFileInfoDtos) throws SQLException {
        if (CollectionUtils.isEmpty(addFileInfoDtos)) return;
        int batchSize = minioParameters.getBatchSize() != null ? minioParameters.getBatchSize() : 500;
        List<FieldInfoDto> fieldsToInsert = new ArrayList<>();
        for (FileInfoDto entity : addFileInfoDtos) {
            if (entity.getFldType() == 1 && entity.getId() != null && CollectionUtils.isNotEmpty(entity.getColumns())) {
                for (FieldInfoDto column : entity.getColumns()) {
                    column.setRefFileId(String.valueOf(entity.getId()));
                    fieldsToInsert.add(column);
                }
            }
        }
        // 分批插入文件
        for (int i = 0; i < addFileInfoDtos.size(); i += batchSize) {
            List<FileInfoDto> subList = addFileInfoDtos.subList(i, Math.min(i + batchSize, addFileInfoDtos.size()));
            try (PreparedStatement fileInsertStmt = conn.prepareStatement(FILE_INSERT_SQL)) {
                for (FileInfoDto entity : subList) {
                    setFileInsertParameters(fileInsertStmt, entity);
                    fileInsertStmt.addBatch();
                }
                int[] results = fileInsertStmt.executeBatch();
//                log.info("【ADD FILE BATCH】插入文件 {} 条", results.length);
            }
        }

        // 分批插入字段
        for (int i = 0; i < fieldsToInsert.size(); i += batchSize) {
            List<FieldInfoDto> subList = fieldsToInsert.subList(i, Math.min(i + batchSize, fieldsToInsert.size()));
            try (PreparedStatement fieldInsertStmt = conn.prepareStatement(COLUMN_INSERT_SQL)) {
                for (FieldInfoDto column : subList) {
                    setColumnInsertParameters(fieldInsertStmt, column);
                    fieldInsertStmt.addBatch();
                }
                int[] results = fieldInsertStmt.executeBatch();
//                log.info("【ADD FIELD BATCH】插入字段 {} 条", results.length);
            }
        }

        conn.commit();
//        log.info("【ADD SUCCESS】文件和字段插入完成");
    }

    private void saveUpdateRealTime(List<FileInfoDto> updateFileInfoDtos) throws SQLException {
        if (CollectionUtils.isEmpty(updateFileInfoDtos)) return;

        int batchSize = minioParameters.getBatchSize() != null ? minioParameters.getBatchSize() : 500;

        List<Long> fileIdsToProcess = new ArrayList<>();
        List<FieldInfoDto> fieldsToInsert = new ArrayList<>();

        for (FileInfoDto entity : updateFileInfoDtos) {
            if (entity.getFldType() == 1 && entity.getId() != null) {
                fileIdsToProcess.add(entity.getId());
                if (CollectionUtils.isNotEmpty(entity.getColumns())) {
                    for (FieldInfoDto column : entity.getColumns()) {
                        column.setRefFileId(String.valueOf(entity.getId()));
                        fieldsToInsert.add(column);
                    }
                }
            }
        }
        // --- 分批更新文件 ---
        for (int i = 0; i < updateFileInfoDtos.size(); i += batchSize) {
            List<FileInfoDto> subList = updateFileInfoDtos.subList(i, Math.min(i + batchSize, updateFileInfoDtos.size()));
            try (PreparedStatement fileUpdateStmt = conn.prepareStatement(FILE_UPDATE_SQL)) {
                for (FileInfoDto entity : subList) {
                    setFileUpdateParameters(fileUpdateStmt, entity);
                    fileUpdateStmt.addBatch();
                }
                int[] results = fileUpdateStmt.executeBatch();
//                log.info("【UPDATE FILE BATCH】更新文件 {} 条", results.length);
            }
        }
        // --- 分批删除字段 ---
        for (int i = 0; i < fileIdsToProcess.size(); i += batchSize) {
            List<Long> subList = fileIdsToProcess.subList(i, Math.min(i + batchSize, fileIdsToProcess.size()));
            try (PreparedStatement fieldDeleteStmt = conn.prepareStatement(COLUMN_DELETE_SQL)) {
                for (Long fileId : subList) {
                    fieldDeleteStmt.setLong(1, fileId);
                    fieldDeleteStmt.addBatch();
                }
                int[] results = fieldDeleteStmt.executeBatch();
//                log.info("【DELETE FIELD BATCH】删除字段 {} 条", Arrays.stream(results).sum());
            }
        }
        // --- 分批插入新字段 ---
        for (int i = 0; i < fieldsToInsert.size(); i += batchSize) {
            List<FieldInfoDto> subList = fieldsToInsert.subList(i, Math.min(i + batchSize, fieldsToInsert.size()));
            try (PreparedStatement fieldInsertStmt = conn.prepareStatement(COLUMN_INSERT_SQL)) {
                for (FieldInfoDto column : subList) {
                    setColumnInsertParameters(fieldInsertStmt, column);
                    fieldInsertStmt.addBatch();
                }
                int[] results = fieldInsertStmt.executeBatch();
//                log.info("【INSERT FIELD BATCH】插入字段 {} 条", results.length);
            }
        }
        conn.commit();
//        log.info("【UPDATE SUCCESS】文件和字段更新完成");
    }


    private void saveDeleteRealTime(List<String> deleteFilePaths, List<String> deleteFolderPaths) throws SQLException {
//        log.warn("【DELETE START】开始删除操作: deleteFilePaths={}, deleteFolderPaths={}", deleteFilePaths, deleteFolderPaths);
        List<Long> allFileIdsToDelete = getFileIdsByPathsAndFolders(deleteFilePaths, deleteFolderPaths);
//        log.warn("【DELETE STEP1】查询到需要删除 file_id 总数={}: {}", allFileIdsToDelete.size(), allFileIdsToDelete);
        int batchSize = minioParameters.getBatchSize() != null ? minioParameters.getBatchSize() : 500;

        // --- 分批处理字段删除 ---
        for (int i = 0; i < allFileIdsToDelete.size(); i += batchSize) {
            List<Long> subList = allFileIdsToDelete.subList(i, Math.min(i + batchSize, allFileIdsToDelete.size()));
            try (PreparedStatement fieldDeleteStmt = conn.prepareStatement(COLUMN_DELETE_SQL)) {
                for (Long fileId : subList) {
                    fieldDeleteStmt.setLong(1, fileId);
                    fieldDeleteStmt.addBatch();
                }
                int[] fieldResults = fieldDeleteStmt.executeBatch();
//                log.warn("【DELETE FIELD BATCH】执行字段删除 {} 条", Arrays.stream(fieldResults).sum());
            }
        }

        // --- 分批处理文件删除 ---
        for (int i = 0; i < deleteFilePaths.size(); i += batchSize) {
            List<String> subList = deleteFilePaths.subList(i, Math.min(i + batchSize, deleteFilePaths.size()));
            try (PreparedStatement fileDeleteStmt = conn.prepareStatement(FILE_DELETE_SQL)) {
                for (String path : subList) {
                    fileDeleteStmt.setString(1, String.valueOf(minioParameters.getSourceId()));
                    fileDeleteStmt.setString(2, path);
                    fileDeleteStmt.addBatch();
                }
                int[] fileResults = fileDeleteStmt.executeBatch();
//                log.warn("【DELETE FILE BATCH】执行文件删除 {} 条", fileResults.length);
            }
        }

        // --- 分批处理文件夹删除 ---
        for (int i = 0; i < deleteFolderPaths.size(); i += batchSize) {
            List<String> subList = deleteFolderPaths.subList(i, Math.min(i + batchSize, deleteFolderPaths.size()));
            try (PreparedStatement folderDeleteStmt = conn.prepareStatement(FILE_DELETE_SUBFILE_SQL)) {
                for (String path : subList) {
                    folderDeleteStmt.setString(1, String.valueOf(minioParameters.getSourceId()));
                    folderDeleteStmt.setString(2, path);
                    folderDeleteStmt.addBatch();
                }
                int[] folderResults = folderDeleteStmt.executeBatch();
//                log.warn("【DELETE FOLDER BATCH】执行文件夹删除 {} 条", folderResults.length);
            }
        }

        // --- 提交事务 ---
        conn.commit();
//        log.warn("【DELETE SUCCESS】事务提交成功");
    }

    private List<Long> getFileIdsByPathsAndFolders(List<String> deleteFilePaths, List<String> deleteFloderPaths) throws SQLException {
        List<Long> allFileIdsToDelete = new ArrayList<>();

        boolean hasFiles = CollectionUtils.isNotEmpty(deleteFilePaths);
        boolean hasFolders = CollectionUtils.isNotEmpty(deleteFloderPaths);

        if (!hasFiles && !hasFolders) {
//            log.warn("【GET FILE IDS】无删除目标");
            return allFileIdsToDelete;
        }

        StringBuilder sql = new StringBuilder("SELECT id FROM metadata_file WHERE source_id = ? AND (");
        List<String> parameters = new ArrayList<>();

        if (hasFiles) {
            String inClause = deleteFilePaths.stream().map(p -> "?").collect(Collectors.joining(", "));
            sql.append("file_path IN (").append(inClause).append(")");
            parameters.addAll(deleteFilePaths);
        }

        if (hasFolders) {
            if (hasFiles) sql.append(" OR ");
            String likeClause = deleteFloderPaths.stream()
                    .map(p -> "file_path LIKE ?")
                    .collect(Collectors.joining(" OR "));
            sql.append("(").append(likeClause).append(")");

            for (String p : deleteFloderPaths) {
                parameters.add(p + "%");
            }
        }

        sql.append(")");

//        log.warn("【GET FILE IDS】SQL = {}", sql);
//        log.warn("【GET FILE IDS】Params = source_id={}, other params={}",
//                minioParameters.getSourceId(), parameters);

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setString(1, String.valueOf(minioParameters.getSourceId()));
            int index = 2;
            for (String p : parameters) {
                stmt.setString(index++, p);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    allFileIdsToDelete.add(rs.getLong("id"));
                }
            }
        }

//        log.warn("【GET FILE IDS】匹配到 {} 个文件 id: {}", allFileIdsToDelete.size(), allFileIdsToDelete);
        return allFileIdsToDelete;
    }


    private Map<String,FileInfoExistence> checkExistenceFileBatch(Connection conn, List<FileInfoDto> fileRecords,
                                                                  Long currentFloderId, String currentDirPath) throws SQLException {
        String sql = "SELECT id, file_path, fld_type FROM metadata_file WHERE file_id_path = ? AND source_id = ?";
        if (fileRecords.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, FileInfoExistence> result = new HashMap<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            // 根据目录id进行判断
            stmt.setString(1, currentDirPath);
            stmt.setString(2, String.valueOf(minioParameters.getSourceId()));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    FileInfoExistence fexist = new FileInfoExistence();
                    fexist.setId(rs.getLong("id"));
                    fexist.setFldType(rs.getInt("fld_type"));
                    String filePath = rs.getString("file_path");
                    fexist.setFilePath(filePath);
                    // key 路径 value id和文件类型
                    result.put(filePath, fexist);
                }
            }
        }
//        log.info("从数据库获取的数据:{},", result.toString());
        return result;
    }


    private void updateBucketInfoToDatabase(String bucketName, BucketUsageDto usage) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setString(1, bucketName); // file_name
            stmt.setString(2, "/" + bucketName + "/" ); // file_path
            stmt.setString(3, ""); // file_regex
            stmt.setString(4, ""); // structure_type
            stmt.setString(5, ""); // file_type
            stmt.setTimestamp(6, new Timestamp(System.currentTimeMillis())); // last_update_time
            stmt.setInt(7, 2); // fld_type
            stmt.setLong(8, usage.getTotalSize()); // file_size
            stmt.setInt(9, minioParameters.getSourceId()); // source_id
            stmt.setString(10, "/" + bucketName + "/"); // file_path
            stmt.executeUpdate();
        }
    }

    private FileInfoDto createFileInfoDto(Item item, String bucketName, Long currentFolderId, String currentDirPath, String currentDirName) throws TimeoutException, ExecutionException, InterruptedException, SQLException {
        String extension = getFileExtension(item.objectName());
        if(StringUtils.isEmpty(extension)){
            log.error("Skipping file without extension: {}", item.objectName());
            return null;
        }

        LocalDateTime lastModified = item.lastModified() != null ?
                item.lastModified().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null;
        FileTypeEnum fileType = FileTypeEnum.getFileType(extension);
        String fileTypeStr = fileType==null ? "" : fileType.getFileType();

        // 时间过滤逻辑
        if (null != taskExecutionContext.getLatestHistoryStartTime()) {
            LocalDateTime historyTaskStartTime = taskExecutionContext.getLatestHistoryStartTime()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            if (lastModified.compareTo(historyTaskStartTime) < 0) {
                log.error("Skipping file modified before history start time: {}, lastModified={}",
                        item.objectName(), lastModified);
                return null;
            }
        }

        String fileName = getFileName(item.objectName());
        String filePath = "/" + bucketName + "/" + item.objectName();
        String sourceId = minioParameters.getSourceId().toString();

        // 首先检查文件是否已存在
        Long fileId = getExistingFileId(filePath);
        if (fileId != null) {
            // 使用已存在的文件ID
            return new FileInfoDto(
                    fileId,
                    currentFolderId,
                    fileId,
                    currentFolderId,
                    fileName,
                    filePath,
                    currentDirPath,
                    1,
                    "",
                    fileTypeStr,
                    "",
                    sourceId,
                    lastModified,
                    String.valueOf(taskExecutionContext.getUserId()),
                    item.size(),
                    currentDirName
            );
        } else {
            // 只有新文件才生成新ID
            Long id = snowflake.nextId();
            FileInfoDto fileInfoDto = new FileInfoDto(
                    id,
                    currentFolderId,
                    id,
                    currentFolderId,
                    fileName,
                    filePath,
                    currentDirPath,
                    1,
                    "",
                    fileTypeStr,
                    "",
                    sourceId,
                    lastModified,
                    String.valueOf(taskExecutionContext.getUserId()),
                    item.size(),
                    currentDirName
            );

            // 文件头解析逻辑
            List<FieldInfoDto> columns = null;
            int maxRetries = 3;

            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    columns = CompletableFuture.supplyAsync(() ->
                            FileHeaderParser.parseFileHeader(item.objectName(), fileName, bucketName, minioClient)
                    ).get(30, TimeUnit.SECONDS);
                    break;
                } catch (TimeoutException e) {
                    log.warn("第 {} 次解析文件 {} 表头超时", attempt, item.objectName());
                    if (attempt == maxRetries) {
                        throw e;
                    }
                } catch (Exception e) {
                    log.warn("第 {} 次解析文件 {} 表头失败", attempt, item.objectName(), e);
                    if (attempt == maxRetries) {
                        throw e;
                    }
                    Thread.sleep(1000);
                }
            }
            fileInfoDto.setColumns(columns);
            return fileInfoDto;
        }
    }

    private Long getExistingFileId(String filePath) throws SQLException {
        String sql = "SELECT id FROM metadata_file WHERE file_path = ? AND source_id = ? AND fld_type = 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, filePath);
            stmt.setString(2, String.valueOf(minioParameters.getSourceId()));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }
        return null;
    }

    private FileInfoDto createFolderInfoDto(Item item, String bucketName, Long currentFolderId, String currentDirPath, String currentDirName) throws SQLException {
        String filePath = "/" + bucketName + "/" + item.objectName();
        String fileName = getFileName(item.objectName());
        if (StringUtils.isBlank(fileName)) {
            fileName = extractDirName(item.objectName());
        }

        String sourceId = minioParameters.getSourceId().toString();

        // 首先检查文件夹是否已存在
        Long folderId = getExistingFolderId(filePath);
        if (folderId != null) {
            // 使用已存在的文件夹ID
            return new FileInfoDto(
                    folderId,
                    currentFolderId,  // 保持正确的父级关系
                    folderId,
                    currentFolderId,
                    fileName,
                    filePath,
                    currentDirPath,
                    2,
                    "",
                    "",
                    "",
                    sourceId,
                    LocalDateTime.now(),
                    String.valueOf(taskExecutionContext.getUserId()),
                    item.size(),
                    currentDirName
            );
        } else {
            // 只有新文件夹才生成新ID
            Long id = snowflake.nextId();
            return new FileInfoDto(
                    id,
                    currentFolderId,
                    id,
                    currentFolderId,
                    fileName,
                    filePath,
                    currentDirPath,
                    2,
                    "",
                    "",
                    "",
                    sourceId,
                    LocalDateTime.now(),
                    String.valueOf(taskExecutionContext.getUserId()),
                    item.size(),
                    currentDirName
            );
        }
    }

    private Long getExistingFolderId(String filePath) throws SQLException {
        String sql = "SELECT id FROM metadata_file WHERE file_path = ? AND source_id = ? AND fld_type = 2";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, filePath);
            stmt.setString(2, String.valueOf(minioParameters.getSourceId()));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }
        return null;
    }

    private String extractDirName(String path) {
        if (StringUtils.isBlank(path)) return "";
        String cleanPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int lastSlash = cleanPath.lastIndexOf('/');
        return lastSlash >= 0 ? cleanPath.substring(lastSlash + 1) : cleanPath;
    }

    private String generateMD5(String fileName, String filePath, String bucketName, String sourceId) {
        String combinedString = fileName + filePath + bucketName + sourceId;
        return DigestUtils.md5Hex(combinedString);
    }

    // 新增：获取目录路径
    private String getDirectoryPath(String objectName) {
        int lastSlash = objectName.lastIndexOf('/');
        return lastSlash > 0 ? objectName.substring(0, lastSlash) : "/";
    }

    // 新增：获取文件名
    private String getFileName(String objectName) {
        int lastSlash = objectName.lastIndexOf('/');
        return lastSlash >= 0 ? objectName.substring(lastSlash + 1) : objectName;
    }

    // 新增：获取文件扩展名
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
    }


    private void initializeMinioClient() {
        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                .getResourceParameters(ResourceType.DATASOURCE, minioParameters.getSourceId());

        log.debug("Creating MinIO client with datasource: {}", JSONUtils.toJsonString(dbSource));
        String connectionParams = dbSource.getConnectionParams();
        ConnectParamDTO connectParamDTO = JSONUtils.parseObject(connectionParams, ConnectParamDTO.class);

        this.minioClient = MinioClient.builder()
                .endpoint(connectParamDTO.getEndpoint(), connectParamDTO.getPort(), connectParamDTO.getSecure())
                .credentials(connectParamDTO.getAccessKey(), connectParamDTO.getSecretKey())
                .build();
    }

    /**
     * 设置插入参数
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
        stmt.setLong(18, fileInfo.getFileSize());
        stmt.setString(19, fileInfo.getDirName());
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
        stmt.setLong(15, fileInfo.getFileSize());
        stmt.setString(16, fileInfo.getDirName());
        stmt.setString(17, String.valueOf(fileInfo.getId()));
    }

    private static void setColumnUpdateParameters(PreparedStatement stmt, FieldInfoDto fieldInfo) throws SQLException {
        stmt.setString(1, fieldInfo.getRefFileId());
        stmt.setString(2, fieldInfo.getColumnName());
        stmt.setString(3, fieldInfo.getColumnType());
        stmt.setString(4, fieldInfo.getSortIndex());
        stmt.setString(5, fieldInfo.getUpdateBy());
        Timestamp updateTime = Timestamp.valueOf(fieldInfo.getUpdateTime());
        stmt.setTimestamp(6, updateTime);
        stmt.setLong(7, Long.parseLong(fieldInfo.getId()));
    }

    private static void setColumnInsertParameters(PreparedStatement stmt, FieldInfoDto fieldInfo) throws SQLException {
        stmt.setString(1, fieldInfo.getRefFileId());
        stmt.setString(2, fieldInfo.getColumnName());
        stmt.setString(3, fieldInfo.getColumnType());
        stmt.setString(4, fieldInfo.getSortIndex());
        stmt.setString(5, fieldInfo.getCreateBy());
        stmt.setObject(6, fieldInfo.getCreateTime());
        stmt.setString(7, fieldInfo.getUpdateBy());
        stmt.setObject(8, fieldInfo.getUpdateTime());
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

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String unit = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), unit);
    }

    private String getBucketAccess(MinioClient minioClient, String bucketName) {
        try {
            String policy = minioClient.getBucketPolicy(GetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .build());
            return parseAccessFromPolicy(policy);
        } catch (Exception e) {
            log.warn("Failed to get bucket policy for {}, assuming private", bucketName, e);
            return "PRIVATE";
        }
    }

    private String parseAccessFromPolicy(String policyJson) {
        if (policyJson == null || policyJson.isEmpty()) {
            return "PRIVATE";
        }

        try {
            JsonNode policy = JSONUtils.toJsonNode(policyJson);
            JsonNode statements = policy.path("Statement");

            for (JsonNode statement : statements) {
                if ("Allow".equals(statement.path("Effect").asText())) {
                    JsonNode principal = statement.path("Principal");
                    if (principal.has("*") || principal.path("AWS").asText("*").equals("*")) {
                        return "PUBLIC";
                    }
                }
            }
            return "CUSTOM";
        } catch (Exception e) {
            log.warn("Failed to parse bucket policy, assuming custom", e);
            return "CUSTOM";
        }
    }

    private void cleanup() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                executorService.shutdownNow();
            }
        }
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
    }


    @Override
    public void cancel() throws TaskException {
        cleanup();
        setExitStatusCode(EXIT_CODE_FAILURE);
        throw new TaskException("MinIO task was cancelled");
    }

    @Override
    public AbstractParameters getParameters() {
        return minioParameters;
    }

    // 定义返回结果的内部类
    private static class ProcessResult {
        long totalSize;
        long objectCount;
        int itemsProcessed;

        ProcessResult(long totalSize, long objectCount, int itemsProcessed) {
            this.totalSize = totalSize;
            this.objectCount = objectCount;
            this.itemsProcessed = itemsProcessed;
        }
    }

    private boolean isFile(Item item) {
        return !item.isDir() && !item.objectName().endsWith("/");
    }

}