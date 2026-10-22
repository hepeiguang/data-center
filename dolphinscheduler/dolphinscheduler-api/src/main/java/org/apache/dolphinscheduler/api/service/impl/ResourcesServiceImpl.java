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

import com.google.common.io.Files;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.api.dto.resources.DeleteDataTransferResponse;
import org.apache.dolphinscheduler.api.dto.resources.ResourceComponent;
import org.apache.dolphinscheduler.api.dto.resources.filter.ResourceFilter;
import org.apache.dolphinscheduler.api.dto.resources.visitor.ResourceTreeVisitor;
import org.apache.dolphinscheduler.api.dto.resources.visitor.Visitor;
import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.exceptions.ServiceException;
import org.apache.dolphinscheduler.api.metrics.ApiServerMetrics;
import org.apache.dolphinscheduler.api.service.ResourcesService;
import org.apache.dolphinscheduler.api.utils.PageInfo;
import org.apache.dolphinscheduler.api.utils.RegexUtils;
import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.api.vo.ResourcesVo;
import org.apache.dolphinscheduler.common.constants.Constants;
import org.apache.dolphinscheduler.common.enums.ProgramType;
import org.apache.dolphinscheduler.common.enums.ResUploadType;
import org.apache.dolphinscheduler.common.utils.FileUtils;
import org.apache.dolphinscheduler.common.utils.PropertyUtils;
import org.apache.dolphinscheduler.dao.entity.Resources;
import org.apache.dolphinscheduler.dao.entity.Tenant;
import org.apache.dolphinscheduler.dao.entity.UdfFunc;
import org.apache.dolphinscheduler.dao.entity.User;
import org.apache.dolphinscheduler.dao.mapper.ResourcesMapper;
import org.apache.dolphinscheduler.dao.mapper.TenantMapper;
import org.apache.dolphinscheduler.dao.mapper.UdfFuncMapper;
import org.apache.dolphinscheduler.dao.mapper.UserMapper;
import org.apache.dolphinscheduler.plugin.storage.api.StorageEntity;
import org.apache.dolphinscheduler.plugin.storage.api.StorageOperate;
import org.apache.dolphinscheduler.spi.enums.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.dolphinscheduler.common.constants.Constants.*;

@Service
@Slf4j
public class ResourcesServiceImpl extends BaseServiceImpl implements ResourcesService {

    @Autowired
    private UdfFuncMapper udfFunctionMapper;

    @Autowired
    private TenantMapper tenantMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired(required = false)
    private StorageOperate storageOperate;

    @Autowired
    private ResourcesMapper resourcesMapper;

    public static final String HIVE_UDF = "HIVE";
    public static final String SEATUNNEL_UDF = "SEATUNNEL";
    public static final String FILE_RESOURCE = "FILE";
    public static final String UDF_RESOURCE = "UDF";

    /**
     * create directory
     *
     * @param loginUser  login user
     * @param name       alias
     * @param type       type
     * @param pid        parent id
     * @param currentDir current directory
     * @return create directory result
     */
    @Override
    //@Transactional
    @Transactional
    //资源管理下新增目录
    public Result<Object> createDirectory(User loginUser, String name,
                                          ResourceType type, int pid, String currentDir) {
        Result<Object> result = new Result<>();

        //文件命名校验 判断是否带“/” 以及是否相对路径
        if (FileUtils.directoryTraversal(name)) {
            log.warn("Parameter name is invalid, name:{}.", RegexUtils.escapeNRT(name));
            putMsg(result, Status.VERIFY_PARAMETER_NAME_FAILED);
            return result;
        }

        //获取用户信息
        User user = userMapper.selectById(loginUser.getId());
        if (user == null) {
            log.error("user {} not exists", loginUser.getId());
            putMsg(result, Status.USER_NOT_EXIST, loginUser.getId());
            return result;
        }

        log.info("当前用户：{}", user.toString());
        //租户相关设置
        String tenantCode = getTenantCode(user);
        //判断 currentDir 是否为空，如果为空，则设置为用户的根目录
        checkFullName(tenantCode, currentDir);
        //拼接路径
        //file:/dolphinscheduler/default/udfs/
        String userResRootPath = ResourceType.UDF.equals(type) ? storageOperate.getUdfDir(tenantCode)
                : storageOperate.getResDir(tenantCode);
        String fullName = !currentDir.contains(userResRootPath) ? userResRootPath + name : currentDir + name;

        //先判断文件系统里资源是否存在
        try {
            if (checkResourceExists(fullName)) {
                log.error("resource directory {} has exist, can't recreate", fullName);
                putMsg(result, Status.RESOURCE_EXIST);
                return result;
            }
        } catch (Exception e) {
            log.warn("Resource exists, can not create again, fullName:{}.", fullName, e);
            throw new ServiceException("resource already exists, can't recreate");
        }
        // create directory in storage
        createDirectory(loginUser, fullName, type, result);

        //如果文件校验没有问题，就创建目录并存储到数据库中
        //因为fullName是数据库的唯一索引字段 所以改数据库表只存储udf不存储file
        if (UDF_RESOURCE.equalsIgnoreCase(type.name())) {
            Resources resourceByFullName = resourcesMapper.getResourceByFullName(fullName);
            //判断数据库中是否存在该文件夹
            if (resourceByFullName != null) {
                log.error("resource directory {} has exist, can't recreate", fullName);
                putMsg(result, Status.RESOURCE_EXIST);
                return result;
            }
            try {
                //存储数据到数据库
                Resources resources = new Resources();
                resources.setId(null);
                resources.setAlias(name + FOLDER_SEPARATOR);
                resources.setFileName(name + FOLDER_SEPARATOR);
                resources.setUserId(Integer.valueOf(loginUser.getId()));
                resources.setType(type.getCode());
                resources.setFullName(fullName + FOLDER_SEPARATOR);
                resources.setPid(pid);
                resources.setIsDirectory(1); // 表示这是一个目录
                resources.setCreateTime(new Date());
                resources.setUpdateTime(new Date());
                resources.setSize(0); // 目录大小默认为 0
                if (UDF_RESOURCE.equalsIgnoreCase(type.getDescp())) {
                    resources.setCustomType(2); //文件类型
                } else if (FILE_RESOURCE.equalsIgnoreCase(type.getDescp())) {
                    resources.setCustomType(3); //文件类型
                } else {
                    log.error("resource {} has incorrect type, can't recreate", fullName);
                    putMsg(result, Status.valueOf("错误的资源类型！"));
                    return result;
                }
                //判断层级
                String s = resources.getFullName().replaceFirst(userResRootPath, "");
                String[] split = s.split(FOLDER_SEPARATOR);
                resources.setLevel(split.length - 1);
                resources.setPfullName(fullName.substring(0, fullName.lastIndexOf(FOLDER_SEPARATOR)));
                //保存到数据库
                resourcesMapper.saveResource(resources);
            } catch (Exception exception) {
                // 如果创建目录失败，删除已创建的目录
                FileUtils.deleteFile(fullName + FOLDER_SEPARATOR);
                log.error("can not create directory, fullName:{}", fullName, exception);
                throw new ServiceException("resource already exists, can't recreate");
            }
        }
        putMsg(result, Status.SUCCESS);
        return result;
    }

    /**
     * create resource
     *
     * @param loginUser  login user
     * @param name       alias
     * @param type       type
     * @param file       file
     * @param currentDir current directory
     * @return create result code
     */
    @Override
    @Transactional
    public Result<Object> uploadResource(User loginUser, String name, ResourceType type, MultipartFile file,
                                         String currentDir, String customType) {
        Result<Object> result = new Result<>();

        // 判断用户是否存在
        User user = userMapper.selectById(loginUser.getId());
        if (user == null) {
            log.error("user {} not exists", loginUser.getId());
            putMsg(result, Status.USER_NOT_EXIST, loginUser.getId());
            return result;
        }

        //租户相关设置
        String tenantCode = getTenantCode(user);
        checkFullName(tenantCode, currentDir);

        //文件命名校验
        result = verifyFile(name, type, file);
        if (!result.getCode().equals(Status.SUCCESS.getCode())) {
            return result;
        }

        //增加对customType的校验
        if (!HIVE_UDF.equals(customType)
                && !SEATUNNEL_UDF.equalsIgnoreCase(customType)
                && !FILE_RESOURCE.equalsIgnoreCase(customType)
                && !UDF_RESOURCE.equalsIgnoreCase(customType)
        ) {
            log.error("resource {} not HIVE or SEATUNNEL", customType);
            putMsg(result, Status.valueOf("错误的资源类型！"));
            return result;
        }

        // check resource name exists
        String userResRootPath = ResourceType.UDF.equals(type)
                //存储到用户的udf目录下
                ? storageOperate.getUdfDir(tenantCode)
                //存储到用户的资源目录下
                : storageOperate.getResDir(tenantCode);
        String currDirNFileName = !currentDir.contains(userResRootPath)
                //拼接路径
                ? userResRootPath + name :
                //拼接路径
                currentDir + name;

        try {
            if (checkResourceExists(currDirNFileName)) {
                log.error("resource {} has exist, can't recreate", RegexUtils.escapeNRT(name));
                putMsg(result, Status.RESOURCE_EXIST);
                return result;
            }
        } catch (Exception e) {
            throw new ServiceException("resource already exists, can't recreate");
        }

        //文件名长度校验
        if (currDirNFileName.length() > Constants.RESOURCE_FULL_NAME_MAX_LENGTH) {
            log.error(
                    "Resource file's name is longer than max full name length, fullName:{}, "
                            + "fullNameSize:{}, maxFullNameSize:{}",
                    RegexUtils.escapeNRT(name), currDirNFileName.length(), Constants.RESOURCE_FULL_NAME_MAX_LENGTH);
            putMsg(result, Status.RESOURCE_FULL_NAME_TOO_LONG_ERROR);
            return result;
        }

        // fail upload
        if (!upload(loginUser, currDirNFileName, file, type, customType)) {
            log.error("upload resource: {} file: {} failed.", RegexUtils.escapeNRT(name),
                    RegexUtils.escapeNRT(file.getOriginalFilename()));
            putMsg(result, Status.STORE_OPERATE_CREATE_ERROR);
            throw new ServiceException(
                    String.format("upload resource: %s file: %s failed.", name, file.getOriginalFilename()));
        } else
            ApiServerMetrics.recordApiResourceUploadSize(file.getSize());
        log.info("Upload resource file complete, resourceName:{}, fileName:{}.", RegexUtils.escapeNRT(name),
                RegexUtils.escapeNRT(file.getOriginalFilename()));

        log.info("测试：{}", UDF_RESOURCE.equals(type.name()));
        if (UDF_RESOURCE.equals(type.name())) {
            //增加对数据库的判断 如果数据库中存在该文件名
            Resources resourceByFullName = resourcesMapper.getResourceByFullName(currDirNFileName);
            if (resourceByFullName != null && resourceByFullName.getIsDirectory() == 0) {
                log.error("resource {} has exist, can't recreate", RegexUtils.escapeNRT(name));
                putMsg(result, Status.RESOURCE_EXIST);
                return result;
            }
            try {
                //存到mysql数据库作为数据记录
                Resources resources = new Resources();
                resources.setId(null);
                resources.setAlias(name); // 别名可以是上传时的名字
                resources.setFileName(file.getOriginalFilename()); // 原始文件名
                resources.setDescription(null); // 如果有 description 参数传入
                resources.setUserId(Integer.valueOf(loginUser.getId())); // 当前登录用户ID
                resources.setType(type.getCode()); // 类型：0-file，1-udf 2 file
                resources.setSize((int) file.getSize()); // 文件大小（单位：字节）
                resources.setCreateTime(new Date()); // 创建时间
                resources.setUpdateTime(new Date()); // 更新时间
                resources.setPid(0); // 父节点ID，默认为根目录或根据业务逻辑设置
                resources.setFullName(currDirNFileName); // 完整路径
                resources.setIsDirectory(0); // 默认不是目录
                Integer customTypeCode = -1;
                if (HIVE_UDF.equals(customType)) {
                    customTypeCode = 0;
                } else if (SEATUNNEL_UDF.equals(customType)) {
                    customTypeCode = 1;
                } else if (FILE_RESOURCE.equals(customType)) {
                    customTypeCode = 2;
                } else {
                    log.error("resource {} not HIVE or SEATUNNEL", file.getOriginalFilename());
                    throw new ServiceException(
                            String.format("resource: %s file: %s has incorrect customType.", name, file.getOriginalFilename()));
                }
                resources.setCustomType(customTypeCode); // 自定义类型："HIVE" 0或 "SEATUNNEL" 1 或 2 文件 ""
                //文件层级
                String s = resources.getFullName().replaceFirst(userResRootPath, "") + FOLDER_SEPARATOR;
                String[] split = s.split(FOLDER_SEPARATOR);
                resources.setLevel(split.length - 1);
                resources.setPfullName(currDirNFileName.substring(0, currDirNFileName.lastIndexOf(FOLDER_SEPARATOR)));
                resourcesMapper.saveResource(resources);
            } catch (Exception e) {
                log.error("can not create directory, fullName:{}", currDirNFileName, e);
                FileUtils.deleteFile(currDirNFileName);
            }
        }
        putMsg(result, Status.SUCCESS);
        return result;
    }

    /**
     * check resource is exists
     *
     * @param fullName fullName
     * @return true if resource exists
     */
    //增加判断 当资源在存储中都存在时，返回true
    private boolean checkResourceExists(String fullName) {
        Boolean existResource = false;
        try {
            existResource = storageOperate.exists(fullName);
        } catch (IOException e) {
            log.error("error occurred when checking resource: " + fullName, e);
        }
        return Boolean.TRUE.equals(existResource);
    }

    /**
     * update resource
     *
     * @param loginUser        login user
     * @param resourceFullName resource full name
     * @param resTenantCode    tenantCode in the request field "resTenantCode" for tenant code owning the resource,
     *                         can be different from the login user in the case of logging in as admin users.
     * @param name             name
     * @param type             resource type
     * @param file             resource file
     * @return update result code
     */
    //更新资源
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Object> updateResource(User loginUser, String resourceFullName, String resTenantCode, String name,
                                         ResourceType type, MultipartFile file) {

        Result<Object> result = new Result<>();

        // 判断用户是否存在
        User user = userMapper.selectById(loginUser.getId());
        if (user == null) {
            log.error("user {} not exists", loginUser.getId());
            putMsg(result, Status.USER_NOT_EXIST, loginUser.getId());
            return result;
        }

        //租户相关设置
        String tenantCode = getTenantCode(user);
        checkFullName(tenantCode, resourceFullName);
        result = verifyFile(name, type, file);
        if (!result.getCode().equals(Status.SUCCESS.getCode())) {
            return result;
        }

        //权限校验
        if (!isUserTenantValid(isAdmin(loginUser), tenantCode, resTenantCode)) {
            log.error("current user does not have permission");
            putMsg(result, Status.NO_CURRENT_OPERATING_PERMISSION);
            return result;
        }

        // 默认路径
        // 对于 HDFS 存储，默认路径是用户的根目录；对于其他存储类型，默认路径是租户的根目录。
        String defaultPath = storageOperate.getDir(type, tenantCode);

        // 获取资源的状态信息
        StorageEntity resource;
        try {
            resource = storageOperate.getFileStatus(resourceFullName, defaultPath, resTenantCode, type);
        } catch (Exception e) {
            log.error("Get file status fail, resource path: {}", resourceFullName, e);
            putMsg(result, Status.RESOURCE_NOT_EXIST);
            throw new ServiceException((String.format("Get file status fail, resource path: %s", resourceFullName)));
        }

        // 如果资源是目录，且存储类型是 S3，且目录名称与更新后的名称不同，返回错误信息。
        // 这是因为 S3 存储不支持重命名目录。
        // TODO: deal with OSS
        if (resource.isDirectory() && storageOperate.returnStorageType().equals(ResUploadType.S3)
                && !resource.getFileName().equals(name)) {
            log.warn("Directory in S3 storage can not be renamed.");
            putMsg(result, Status.S3_CANNOT_RENAME);
            return result;
        }

        //全路径
        String originFullName = resource.getFullName();
        //别名
        String originResourceName = resource.getAlias();

        //fullName
        originFullName = originFullName.endsWith(FOLDER_SEPARATOR) ? StringUtils.chop(originFullName) : originFullName;
        //别名
        name = name.endsWith(FOLDER_SEPARATOR) ? StringUtils.chop(name) : name;
        // updated fullName
        // 拼接新的路径
        // 1. 如果资源是目录，那么新的路径就是原来的路径加上新的名称。
        // 2. 如果资源不是目录，那么新的路径就是原来的路径去掉原来的名称，加上新的名称。
        // 3. 如果新的名称和原来的名称相同，那么新的路径就是原来的路径。
        String fullName = String.format(FORMAT_SS, originFullName.substring(0, originFullName.lastIndexOf(FOLDER_SEPARATOR) + 1), name);

        // 如果新的路径和原来的路径相同，那么就不需要更新资源。
        if (!originResourceName.equals(name)) {
            try {
                if (checkResourceExists(fullName)) {
                    log.error("resource {} already exists, can't recreate", fullName);
                    putMsg(result, Status.RESOURCE_EXIST);
                    return result;
                }
            } catch (Exception e) {
                throw new ServiceException(String.format("error occurs while querying resource: %s", fullName));
            }
        }


        result = verifyFile(name, type, file);
        if (!result.getCode().equals(Status.SUCCESS.getCode())) {
            return result;
        }

        Date now = new Date();

        //设置资源的别名、文件名、全路径和更新时间。
        resource.setAlias(name);
        resource.setFileName(name);
        resource.setFullName(fullName);
        resource.setUpdateTime(now);
        if (file != null) {
            resource.setSize(file.getSize());
        }

        // if name unchanged, return directly without moving on HDFS
        //名字没改变+然后没上传文件
        if (originResourceName.equals(name) && file == null) {
            return result;
        }

        if (file != null) {
            // fail upload
            if (!upload(loginUser, fullName, file, type, "")) {
                log.error("Storage operation error, resourceName:{}, originFileName:{}.", name,
                        RegexUtils.escapeNRT(file.getOriginalFilename()));
                putMsg(result, Status.HDFS_OPERATION_ERROR);
                throw new ServiceException(
                        String.format("upload resource: %s file: %s failed.", name, file.getOriginalFilename()));
            }
            if (!fullName.equals(originFullName)) {
                try {
                    storageOperate.delete(originFullName, false);
                } catch (IOException e) {
                    log.error("Resource delete error, resourceFullName:{}.", originFullName, e);
                    throw new ServiceException(String.format("delete resource: %s failed.", originFullName));
                }
            }
            ApiServerMetrics.recordApiResourceUploadSize(file.getSize());
        } else {
            // get the path of dest file in hdfs
            //文件覆盖
            String destHdfsFileName = fullName;
            try {
                log.info("start  copy {} -> {}", originFullName, destHdfsFileName);
                storageOperate.copy(originFullName, destHdfsFileName, true, true);
                putMsg(result, Status.SUCCESS);
            } catch (Exception e) {
                log.error(MessageFormat.format(" copy {0} -> {1} fail", originFullName, destHdfsFileName), e);
                putMsg(result, Status.HDFS_COPY_FAIL);
                throw new ServiceException(
                        MessageFormat.format(Status.HDFS_COPY_FAIL.getMsg(), originFullName, destHdfsFileName));
            }
        }


        if (UDF_RESOURCE.equalsIgnoreCase(type.name())) {
            //从数据库获取
            Resources resourceByFullName = resourcesMapper.getResourceByFullName(resourceFullName);
            //在数据库中修改该资源
            if (resourceFullName.endsWith(FOLDER_SEPARATOR)) {
                String FolderPrefix = StringUtils.chop(resourceFullName);
                String UpdateFolderPrefix = StringUtils.chop(fullName);
                UpdateFolderPrefix = UpdateFolderPrefix.substring(0, UpdateFolderPrefix.lastIndexOf(FOLDER_SEPARATOR) + 1) + name;
                //如果是目录 查找数据库中的子文件夹和文件并修改
                List<Resources> resourcesList = resourcesMapper.getResourceListLikePartName(resourceFullName);
                fullName = fullName.endsWith(FOLDER_SEPARATOR) ? fullName : fullName + FOLDER_SEPARATOR;
                name = name.endsWith(FOLDER_SEPARATOR) ? name : name + FOLDER_SEPARATOR;
                if (resourcesList.isEmpty()) {
                    //表示当前目录无子目录，子文件
                    resourceByFullName.setFullName(fullName);
                    resourceByFullName.setFileName(name);
                    resourceByFullName.setAlias(name);
                    resourceByFullName.setUpdateTime(now);
                    resourcesMapper.fixResourceByFullName(fullName, name, now, originFullName + FOLDER_SEPARATOR);
                } else {
                    //如果目录不为空
                    for (Resources item : resourcesList) {
                        //查找一致的目录前缀
                        String fullName1 = item.getFullName();
                        fullName1 = fullName1.replaceFirst(FolderPrefix, UpdateFolderPrefix);
                        //修改数据库中的目录
                        resourcesMapper.fixResourceByFullName(fullName1, item.getFileName(), now, item.getFullName());
                    }
                    resourceByFullName.setFullName(fullName);
                    resourceByFullName.setFileName(name);
                    resourceByFullName.setAlias(name);
                    resourceByFullName.setUpdateTime(now);
                    resourcesMapper.fixResourceByFullName(fullName, name, now, originFullName + FOLDER_SEPARATOR);
                }
            } else {
                //如果是文件 直接修改
                resourceByFullName.setFullName(fullName);
                resourceByFullName.setFileName(name);
                resourceByFullName.setAlias(name);
                resourceByFullName.setUpdateTime(now);
                if (file != null) {
                    resource.setSize(file.getSize());
                }
                //传递新对象 和 旧的名称
                if (resourcesMapper.fixResourceByFullName(fullName, name, now, originFullName) == 0) {
                    log.error(" update database failed");
                    putMsg(result, Status.HDFS_COPY_FAIL);
                    return result;
                }
            }
        }
        putMsg(result, Status.SUCCESS);
        return result;
    }


    private Result<Object> verifyFile(String name, ResourceType type, MultipartFile file) {
        Result<Object> result = new Result<>();
        putMsg(result, Status.SUCCESS);

        if (FileUtils.directoryTraversal(name)) {
            log.warn("Parameter file alias name verify failed, fileAliasName:{}.", RegexUtils.escapeNRT(name));
            putMsg(result, Status.VERIFY_PARAMETER_NAME_FAILED);
            return result;
        }

        if (file != null && FileUtils.directoryTraversal(Objects.requireNonNull(file.getOriginalFilename()))) {
            log.warn("File original name verify failed, fileOriginalName:{}.",
                    RegexUtils.escapeNRT(file.getOriginalFilename()));
            putMsg(result, Status.VERIFY_PARAMETER_NAME_FAILED);
            return result;
        }

        if (file != null) {
            // file is empty
            if (file.isEmpty()) {
                log.warn("Parameter file is empty, fileOriginalName:{}.",
                        RegexUtils.escapeNRT(file.getOriginalFilename()));
                putMsg(result, Status.RESOURCE_FILE_IS_EMPTY);
                return result;
            }

            // file suffix
            String fileSuffix = Files.getFileExtension(file.getOriginalFilename());
            String nameSuffix = Files.getFileExtension(name);

            // determine file suffix
            if (!fileSuffix.equalsIgnoreCase(nameSuffix)) {
                // rename file suffix and original suffix must be consistent
                log.warn("Rename file suffix and original suffix must be consistent, fileOriginalName:{}.",
                        RegexUtils.escapeNRT(file.getOriginalFilename()));
                putMsg(result, Status.RESOURCE_SUFFIX_FORBID_CHANGE);
                return result;
            }

            // If resource type is UDF, only jar packages are allowed to be uploaded, and the suffix must be .jar
            if (Constants.UDF.equals(type.name()) && !JAR.equalsIgnoreCase(fileSuffix)) {
                log.warn(Status.UDF_RESOURCE_SUFFIX_NOT_JAR.getMsg());
                putMsg(result, Status.UDF_RESOURCE_SUFFIX_NOT_JAR);
                return result;
            }
            if (file.getSize() > Constants.MAX_FILE_SIZE) {
                log.warn(
                        "Resource file size is larger than max file size, fileOriginalName:{}, fileSize:{}, maxFileSize:{}.",
                        RegexUtils.escapeNRT(file.getOriginalFilename()), file.getSize(), Constants.MAX_FILE_SIZE);
                putMsg(result, Status.RESOURCE_SIZE_EXCEED_LIMIT);
                return result;
            }
        }
        return result;
    }


    /**
     * query resources list paging
     *
     * @param loginUser     login user
     * @param fullName      resource full name
     * @param resTenantCode tenantCode in the request field "resTenantCode" for tenant code owning the resource,
     *                      can be different from the login user in the case of logging in as admin users.
     * @param type          resource type
     * @param searchVal     search value
     * @param pageNo        page number
     * @param pageSize      page size
     * @return resource list page
     */
    //todo
    //todo
    //todo
    @Override
    public Result<PageInfo<Object>> queryResourceListPaging(User loginUser, String fullName,
                                                            String resTenantCode, ResourceType type,
                                                            String searchVal, Integer pageNo,
                                                            Integer pageSize, String customType) {
        Result<PageInfo<Object>> result = new Result<>();
        //校验用户是否存在
        User user = userMapper.selectById(loginUser.getId());
        if (user == null) {
            log.error("user {} not exists", loginUser.getId());
            putMsg(result, Status.USER_NOT_EXIST, loginUser.getId());
            return result;
        }

        String tenantCode = getTenantCode(user);
        checkFullName(tenantCode, fullName);

        if (!isUserTenantValid(isAdmin(loginUser), tenantCode, resTenantCode)) {
            log.error("current user does not have permission");
            putMsg(result, Status.NO_CURRENT_OPERATING_PERMISSION);
            return result;
        }

        String userResRootPath = ResourceType.UDF.equals(type)
                //存储到用户的udf目录下
                ? storageOperate.getUdfDir(tenantCode)
                //存储到用户的资源目录下
                : storageOperate.getResDir(tenantCode);


        //如果是查询udf目录下的资源
        if (UDF_RESOURCE.equalsIgnoreCase(type.name())) {
            PageInfo<Object> pageInfo = new PageInfo<>(pageNo, pageSize);

            //查询数据
            String trimmedSearchVal = searchVal != null ? searchVal.trim() : "";
            log.info("trimmedSearchVal :{}", trimmedSearchVal);

            String currDirNFileName = !fullName.contains(userResRootPath) ? userResRootPath : fullName;
            //file:/dolphinscheduler/default/udfs

            List<Resources> list = null;
            //如果现在查询的是根目录,只查第一层
            //file:/dolphinscheduler/default/udfs/   =>   file:/dolphinscheduler/default/udfs/...和file:/dolphinscheduler/default/udfs/.../
            if (fullName.equals("")) {
                list = resourcesMapper.getResourceListPaging(trimmedSearchVal, null, type, customType, 0);
            } else {
                //如果现在查的是文件夹
                //文件层级
                String s = fullName.replaceFirst(userResRootPath, "");
                s = s.endsWith("/") ? StringUtils.chop(s) : s;
                String[] split = s.split(FOLDER_SEPARATOR);
                if (fullName.endsWith("/")) {
                    list = resourcesMapper.getResourceListPaging(trimmedSearchVal, fullName, type, customType, split.length);
                }
            }

            List<ResourcesVo> resourcesVoList = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                ResourcesVo resourcesVo = new ResourcesVo();
                resourcesVo.setId(list.get(i).getId());
                resourcesVo.setFullName(list.get(i).getFullName());
                resourcesVo.setFileName(list.get(i).getFileName());
                resourcesVo.setAlias(list.get(i).getAlias());
                //处理pfullName
                if (list.get(i).getCustomType() != 2) {
                    String substring = list.get(i).getFullName().substring(0, list.get(i).getFullName().lastIndexOf(FOLDER_SEPARATOR) + 1);
                    resourcesVo.setPfullName(substring);
                } else {
                    String substring = list.get(i).getFullName().substring(0, list.get(i).getFullName().lastIndexOf(FOLDER_SEPARATOR));
                    String substring1 = substring.substring(0, substring.lastIndexOf(FOLDER_SEPARATOR) + 1);
                    resourcesVo.setPfullName(substring1);
                }
                resourcesVo.setUserId(list.get(i).getUserId());
                resourcesVo.setUserName("default");
                resourcesVo.setType("UDF");
                resourcesVo.setSize(list.get(i).getSize());
                resourcesVo.setCreateTime(list.get(i).getCreateTime());
                resourcesVo.setUpdateTime(list.get(i).getUpdateTime());
                if (list.get(i).getCustomType() == 2) {
                    resourcesVo.setDirectory(true);
                    resourcesVo.setCustomType("");
                } else {
                    resourcesVo.setDirectory(false);
                    resourcesVo.setCustomType(list.get(i).getCustomType() == 0 ? "HIVE" : "SEATUNNEL");
                }
                resourcesVoList.add(resourcesVo);
            }

            pageInfo.setTotal(list.size());
            //pageInfo.setTotalList(resourcesVoList);
            pageInfo.setTotalList(new ArrayList<>(resourcesVoList));
            result.setData(pageInfo);
            putMsg(result, Status.SUCCESS);
            return result;
        } else {
            //查询资源 res
            PageInfo<Object> pageInfo = new PageInfo<>(pageNo, pageSize);
            if (storageOperate == null) {
                log.warn("The resource storage is not opened.");
                return Result.success(pageInfo);
            }

            List<StorageEntity> resourcesList;
            try {
                resourcesList = queryStorageEntityList(loginUser, fullName, type, tenantCode, false);
            } catch (ServiceException e) {
                putMsg(result, Status.RESOURCE_NOT_EXIST);
                return result;
            }

            // remove leading and trailing spaces in searchVal
            String trimmedSearchVal = searchVal != null ? searchVal.trim() : "";
            // filter based on trimmed searchVal
            List<StorageEntity> filteredResourceList = resourcesList.stream()
                    .filter(x -> x.getFileName().contains(trimmedSearchVal)).collect(Collectors.toList());
            // inefficient pagination
            List<StorageEntity> slicedResourcesList = filteredResourceList.stream().skip((long) (pageNo - 1) * pageSize)
                    .limit(pageSize).collect(Collectors.toList());

            pageInfo.setTotal(filteredResourceList.size());
            pageInfo.setTotalList(new ArrayList<>(slicedResourcesList));
            result.setData(pageInfo);
            putMsg(result, Status.SUCCESS);
            return result;
        }
    }


//    @Override
//    public Result<PageInfo<StorageEntity>> queryResourceListPaging(User loginUser, String fullName,
//                                                                   String resTenantCode, ResourceType type,
//                                                                   String searchVal, Integer pageNo,
//                                                                   Integer pageSize, String customType) {
//        Result<PageInfo<StorageEntity>> result = new Result<>();
//        PageInfo<StorageEntity> pageInfo = new PageInfo<>(pageNo, pageSize);
//        if (storageOperate == null) {
//            log.warn("The resource storage is not opened.");
//            return Result.success(pageInfo);
//        }
//
//        User user = userMapper.selectById(loginUser.getId());
//        if (user == null) {
//            log.error("user {} not exists", loginUser.getId());
//            putMsg(result, Status.USER_NOT_EXIST, loginUser.getId());
//            return result;
//        }
//
//        String tenantCode = getTenantCode(user);
//        checkFullName(tenantCode, fullName);
//
//        if (!isUserTenantValid(isAdmin(loginUser), tenantCode, resTenantCode)) {
//            log.error("current user does not have permission");
//            putMsg(result, Status.NO_CURRENT_OPERATING_PERMISSION);
//            return result;
//        }
//
//        List<StorageEntity> resourcesList;
//        try {
//            resourcesList = queryStorageEntityList(loginUser, fullName, type, tenantCode, false);
//        } catch (ServiceException e) {
//            putMsg(result, Status.RESOURCE_NOT_EXIST);
//            return result;
//        }
//
//        // remove leading and trailing spaces in searchVal
//        String trimmedSearchVal = searchVal != null ? searchVal.trim() : "";
//        // filter based on trimmed searchVal
//        List<StorageEntity> filteredResourceList = resourcesList.stream()
//                .filter(x -> x.getFileName().contains(trimmedSearchVal)).collect(Collectors.toList());
//        // inefficient pagination
//        List<StorageEntity> slicedResourcesList = filteredResourceList.stream().skip((long) (pageNo - 1) * pageSize)
//                .limit(pageSize).collect(Collectors.toList());
//
//        pageInfo.setTotal(filteredResourceList.size());
//        pageInfo.setTotalList(slicedResourcesList);
//        result.setData(pageInfo);
//        putMsg(result, Status.SUCCESS);
//        return result;
//    }


    private List<StorageEntity> queryStorageEntityList(User loginUser, String fullName, ResourceType type,
                                                       String tenantCode, boolean recursive) {
        String defaultPath = "";
        List<StorageEntity> resourcesList = new ArrayList<>();
        String resourceStorageType =
                PropertyUtils.getString(Constants.RESOURCE_STORAGE_TYPE, ResUploadType.LOCAL.name());
        if (isAdmin(loginUser) && StringUtils.isBlank(fullName)) {
            // list all tenants' resources to admin users in the root directory
            List<User> userList = userMapper.selectList(null);
            Set<String> visitedTenantEntityCode = new HashSet<>();
            for (User userEntity : userList) {
                String tenantEntityCode = getTenantCode(userEntity);
                if (!visitedTenantEntityCode.contains(tenantEntityCode)) {
                    defaultPath = storageOperate.getResDir(tenantEntityCode);
                    if (type.equals(ResourceType.UDF)) {
                        defaultPath = storageOperate.getUdfDir(tenantEntityCode);
                    }
                    try {
                        resourcesList.addAll(recursive
                                ? storageOperate.listFilesStatusRecursively(defaultPath, defaultPath, tenantEntityCode,
                                type)
                                : storageOperate.listFilesStatus(defaultPath, defaultPath, tenantEntityCode, type));

                        visitedTenantEntityCode.add(tenantEntityCode);
                    } catch (Exception e) {
                        log.error(e.getMessage() + " Resource path: {}", defaultPath, e);
                        throw new ServiceException(
                                String.format(e.getMessage() + " make sure resource path: %s exists in %s", defaultPath,
                                        resourceStorageType));
                    }
                }
            }
        } else {
            defaultPath = storageOperate.getResDir(tenantCode);
            if (type.equals(ResourceType.UDF)) {
                defaultPath = storageOperate.getUdfDir(tenantCode);
            }

            try {
                if (StringUtils.isBlank(fullName)) {
                    fullName = defaultPath;
                }
                resourcesList =
                        recursive ? storageOperate.listFilesStatusRecursively(fullName, defaultPath, tenantCode, type)
                                : storageOperate.listFilesStatus(fullName, defaultPath, tenantCode, type);
            } catch (Exception e) {
                log.error(e.getMessage() + " Resource path: {}", fullName, e);
                throw new ServiceException(String.format(e.getMessage() + " make sure resource path: %s exists in %s",
                        defaultPath, resourceStorageType));
            }
        }

        return resourcesList;
    }

    /**
     * create directory
     * xxx The steps to verify resources are cumbersome and can be optimized
     *
     * @param loginUser login user
     * @param fullName  full name
     * @param type      resource type
     * @param result    Result
     */
    private void createDirectory(User loginUser, String fullName, ResourceType type, Result<Object> result) {
        String tenantCode = tenantMapper.queryById(loginUser.getTenantId()).getTenantCode();
        // String directoryName = storageOperate.getFileName(type, tenantCode, fullName);
        String resourceRootPath = storageOperate.getDir(type, tenantCode);
        try {
            if (!storageOperate.exists(resourceRootPath)) {
                storageOperate.createTenantDirIfNotExists(tenantCode);
            }

            if (!storageOperate.mkdir(tenantCode, fullName)) {
                throw new ServiceException(String.format("Create resource directory: %s failed.", fullName));
            }
            putMsg(result, Status.SUCCESS);
        } catch (Exception e) {
            throw new ServiceException(String.format("create resource directory: %s failed.", fullName));
        }
    }

    /**
     * upload file to hdfs
     *
     * @param loginUser login user
     * @param fullName  full name
     * @param file      file
     * @param type      resource type
     * @return upload success return true, otherwise false
     */
    private boolean upload(User loginUser, String fullName, MultipartFile file, ResourceType type, String customType) {
        // save to local
        String fileSuffix = Files.getFileExtension(file.getOriginalFilename());
        String nameSuffix = Files.getFileExtension(fullName);

        // determine file suffix
        if (!fileSuffix.equalsIgnoreCase(nameSuffix)) {
            return false;
        }
        // query tenant
        String tenantCode = getTenantCode(loginUser);
        // random file name
        String localFilename = FileUtils.getUploadFilename(tenantCode, UUID.randomUUID().toString());

        // save file to hdfs, and delete original file
        String resourcePath = storageOperate.getDir(type, tenantCode);
        try {
            // if tenant dir not exists
            if (!storageOperate.exists(resourcePath)) {
                storageOperate.createTenantDirIfNotExists(tenantCode);
            }
//            //给文件增加执行权限
//            try{
//                ProcessBuilder processBuilder = new ProcessBuilder("chmod", "+x", localFilename);
//                processBuilder.inheritIO();
//                try {
//                    Process process = processBuilder.start();
//                    int exitCode = process.waitFor();
//                    if (exitCode != 0) {
//                        throw new IOException("Failed to make the file executable");
//                    }
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt(); // 恢复中断状态
//                    throw new IOException("Interrupted while waiting for chmod command", e);
//                }
//            }catch (Exception e){
//                log.error(e.getMessage(), e);
//            }
            org.apache.dolphinscheduler.api.utils.FileUtils.copyInputStreamToFile(file, localFilename);
            storageOperate.upload(tenantCode, localFilename, fullName, true, true);
            FileUtils.deleteFile(localFilename);
        } catch (Exception e) {
            FileUtils.deleteFile(localFilename);
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }


    /**
     * query resource list
     *
     * @param loginUser login user
     * @param type      resource type
     * @param fullName  resource full name
     * @return resource list
     */
    @Override
    public Map<String, Object> queryResourceList(User loginUser, ResourceType type, String fullName) {
        Map<String, Object> result = new HashMap<>();
        if (storageOperate == null) {
            result.put(Constants.DATA_LIST, Collections.emptyList());
            result.put(Constants.STATUS, Status.SUCCESS);
            return result;
        }

        User user = userMapper.selectById(loginUser.getId());
        if (user == null) {
            log.error("user {} not exists", loginUser.getId());
            putMsg(result, Status.USER_NOT_EXIST, loginUser.getId());
            return null;
        }

        String tenantCode = getTenantCode(user);
        checkFullName(tenantCode, fullName);

        String baseDir = storageOperate.getDir(type, tenantCode);

        List<StorageEntity> resourcesList = new ArrayList<>();
        if (StringUtils.isBlank(fullName)) {
            if (isAdmin(loginUser)) {
                List<User> userList = userMapper.selectList(null);
                Set<String> visitedTenantEntityCode = new HashSet<>();
                for (User userEntity : userList) {
                    String tenantEntityCode = getTenantCode(userEntity);
                    if (!visitedTenantEntityCode.contains(tenantEntityCode)) {
                        baseDir = storageOperate.getDir(type, tenantEntityCode);
                        resourcesList.addAll(storageOperate.listFilesStatusRecursively(baseDir, baseDir,
                                tenantEntityCode, type));
                        visitedTenantEntityCode.add(tenantEntityCode);
                    }
                }
            } else {
                resourcesList = storageOperate.listFilesStatusRecursively(baseDir, baseDir, tenantCode, type);
            }
        } else {
            resourcesList = storageOperate.listFilesStatusRecursively(fullName, baseDir, tenantCode, type);
        }

        Visitor resourceTreeVisitor = new ResourceTreeVisitor(resourcesList);
        List<ResourceComponent> resourceList =resourceTreeVisitor.visit(baseDir).getChildren();
        for (ResourceComponent component : resourceList) {
            setCustomType(component);
        }
        result.put(Constants.DATA_LIST, resourceList);
        putMsg(result, Status.SUCCESS);

        return result;
    }

    public void setCustomType(ResourceComponent component) {
        Resources resourceByFullName = resourcesMapper.getResourceByFullName(component.getFullName());
        // 递归处理子节点
        if (resourceByFullName != null) {
            component.setCustomType(resourceByFullName.getCustomType());
            for (ResourceComponent child : component.getChildren()) {
                setCustomType(child);
            }
        }
    }


    /**
     * query resource list by program type
     *
     * @param loginUser login user
     * @param type      resource type
     * @return resource list
     */
    @Override
    public Result<Object> queryResourceByProgramType(User loginUser, ResourceType type, ProgramType programType) {
        Result<Object> result = new Result<>();

        User user = userMapper.selectById(loginUser.getId());
        if (user == null) {
            log.error("user {} not exists", loginUser.getId());
            putMsg(result, Status.USER_NOT_EXIST, loginUser.getId());
            return result;
        }

        Tenant tenant = tenantMapper.queryById(user.getTenantId());
        if (tenant == null) {
            log.error("tenant not exists");
            putMsg(result, Status.CURRENT_LOGIN_USER_TENANT_NOT_EXIST);
            return result;
        }

        String tenantCode = tenant.getTenantCode();

        List<StorageEntity> allResourceList = queryStorageEntityList(loginUser, "", type, tenantCode, true);

        String suffix = ".jar";
        if (programType != null) {
            switch (programType) {
                case JAVA:
                case SCALA:
                    break;
                case PYTHON:
                    suffix = ".py";
                    break;
                default:
            }
        }
        List<StorageEntity> resources = new ResourceFilter(suffix, new ArrayList<>(allResourceList)).filter();
        Visitor visitor = new ResourceTreeVisitor(resources);
        result.setData(visitor.visit("").getChildren());
        putMsg(result, Status.SUCCESS);
        return result;
    }

    /**
     * delete resource
     *
     * @param loginUser     login user
     * @param fullName      resource full name
     * @param resTenantCode tenantCode in the request field "resTenantCode" for tenant code owning the resource,
     *                      can be different from the login user in the case of logging in as admin users.
     * @return delete result code
     * @throws IOException exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Object> delete(User loginUser, String fullName, String resTenantCode) throws IOException {
        Result<Object> result = new Result<>();
        User user = userMapper.selectById(loginUser.getId());
        if (user == null) {
            log.error("user {} not exists", loginUser.getId());
            putMsg(result, Status.USER_NOT_EXIST, loginUser.getId());
            return result;
        }

        String tenantCode = getTenantCode(user);
        checkFullName(tenantCode, fullName);

        if (!isUserTenantValid(isAdmin(loginUser), tenantCode, resTenantCode)) {
            log.error("current user does not have permission");
            putMsg(result, Status.NO_CURRENT_OPERATING_PERMISSION);
            return result;
        }

        String baseDir = storageOperate.getResDir(tenantCode);
        StorageEntity resource = null;
        try {
            resource = storageOperate.getFileStatus(fullName, baseDir, resTenantCode, null);
        } catch (FileNotFoundException e) {
            log.warn("File not found, file path: {}", fullName);
        } catch (Exception e) {
            log.error(e.getMessage() + " Resource path: {}", fullName, e);
            putMsg(result, Status.RESOURCE_NOT_EXIST);
            throw new ServiceException(String.format(e.getMessage() + " Resource path: %s", fullName));
        }
//
        if (resource == null) {
            List<Resources> allResourceList = resourcesMapper.getAllResourceList(fullName);
            if (CollectionUtils.isNotEmpty(allResourceList)) {
                allResourceList.forEach(item -> {
                    resourcesMapper.delResourceByFullName(item.getFullName());
                });
            }
            log.error("Resource does not exist, resource full name:{}.", fullName);
            putMsg(result, Status.SUCCESS);
            return result;
        }

        // recursively delete a folder
        List<String> allChildren =
                storageOperate.listFilesStatusRecursively(fullName, baseDir, resTenantCode, resource.getType())
                        .stream().map(storageEntity -> storageEntity.getFullName()).collect(Collectors.toList());

        String[] allChildrenFullNameArray = allChildren.stream().toArray(String[]::new);

        // if resource type is UDF,need check whether it is bound by UDF function
        if (resource.getType() == (ResourceType.UDF)) {
            List<UdfFunc> udfFuncs = udfFunctionMapper.listUdfByResourceFullName(allChildrenFullNameArray);
            if (CollectionUtils.isNotEmpty(udfFuncs)) {
                log.warn("Resource can not be deleted because it is bound by UDF functions, udfFuncIds:{}", udfFuncs);
                putMsg(result, Status.UDF_RESOURCE_IS_BOUND, udfFuncs.get(0).getFuncName());
                return result;
            }
        }

        // delete file on hdfs,S3
        storageOperate.delete(fullName, allChildren, true);

        Resources resourceByFullName = resourcesMapper.getResourceByFullName(fullName);
        if (resourceByFullName.getType() == 1){
            resourcesMapper.delResourceByFullName(fullName);
            allChildren.forEach(child -> resourcesMapper.delResourceByFullName(child));
        }
        putMsg(result, Status.SUCCESS);

        return result;
    }

    /**
     * verify resource by name and type
     *
     * @param loginUser login user
     * @param fullName  resource full name
     * @param type      resource type
     * @return true if the resource name not exists, otherwise return false
     */
    @Override
    public Result<Object> verifyResourceName(String fullName, ResourceType type, User loginUser) {
        Result<Object> result = new Result<>();
        putMsg(result, Status.SUCCESS);
        if (checkResourceExists(fullName)) {
            log.error("Resource with same name exists so can not create again, resourceType:{}, resourceName:{}.", type,
                    RegexUtils.escapeNRT(fullName));
            putMsg(result, Status.RESOURCE_EXIST);
        }

        return result;
    }

    /**
     * verify resource by full name or pid and type
     *
     * @param fileName      resource file name
     * @param type          resource type
     * @param resTenantCode tenantCode in the request field "resTenantCode" for tenant code owning the resource,
     *                      can be different from the login user in the case of logging in as admin users.
     * @return true if the resource full name or pid not exists, otherwise return false
     */
    @Override
    public Result<Object> queryResourceByFileName(User loginUser, String fileName, ResourceType type,
                                                  String resTenantCode) {
        Result<Object> result = new Result<>();
        if (StringUtils.isBlank(fileName)) {
            putMsg(result, Status.REQUEST_PARAMS_NOT_VALID_ERROR);
            return result;
        }

        User user = userMapper.selectById(loginUser.getId());
        if (user == null) {
            log.error("user {} not exists", loginUser.getId());
            putMsg(result, Status.USER_NOT_EXIST, loginUser.getId());
            return result;
        }

        String tenantCode = getTenantCode(user);

        if (!isUserTenantValid(isAdmin(loginUser), tenantCode, resTenantCode)) {
            log.error("current user does not have permission");
            putMsg(result, Status.NO_CURRENT_OPERATING_PERMISSION);
            return result;
        }

        String defaultPath = storageOperate.getDir(type, resTenantCode);
        StorageEntity file;
        try {
            file = storageOperate.getFileStatus(defaultPath + fileName, defaultPath, resTenantCode, type);
        } catch (Exception e) {
            log.error(e.getMessage() + " Resource path: {}", defaultPath + fileName, e);
            putMsg(result, Status.RESOURCE_NOT_EXIST);
            return result;
        }

        putMsg(result, Status.SUCCESS);
        result.setData(file);
        return result;
    }

    /**
     * view resource file online
     *
     * @param fullName      resource fullName
     * @param resTenantCode owner's tenant code of the resource
     * @param skipLineNum   skip line number
     * @param limit         limit
     * @return resource content
     */
    @Override
    public Result<Object> readResource(User loginUser, String fullName, String resTenantCode, int skipLineNum,
                                       int limit) {
        Result<Object> result = new Result<>();

        User user = userMapper.selectById(loginUser.getId());
        if (user == null) {
            log.error("user {} not exists", loginUser.getId());
            putMsg(result, Status.USER_NOT_EXIST, loginUser.getId());
            return result;
        }

        String tenantCode = getTenantCode(user);
        checkFullName(tenantCode, fullName);

        if (!isUserTenantValid(isAdmin(loginUser), tenantCode, resTenantCode)) {
            log.error("current user does not have permission");
            putMsg(result, Status.NO_CURRENT_OPERATING_PERMISSION);
            return result;
        }

        // check preview or not by file suffix
        String nameSuffix = Files.getFileExtension(fullName);
        String resourceViewSuffixes = FileUtils.getResourceViewSuffixes();
        if (StringUtils.isNotEmpty(resourceViewSuffixes)) {
            List<String> strList = Arrays.asList(resourceViewSuffixes.split(","));
            if (!strList.contains(nameSuffix)) {
                log.error("Resource suffix does not support view,resourceFullName:{}, suffix:{}.", fullName,
                        nameSuffix);
                putMsg(result, Status.RESOURCE_SUFFIX_NOT_SUPPORT_VIEW);
                return result;
            }
        }

        List<String> content;
        try {
            if (storageOperate.exists(fullName)) {
                content = storageOperate.vimFile(tenantCode, fullName, skipLineNum, limit);
                long size = content.stream().mapToLong(String::length).sum();
                ApiServerMetrics.recordApiResourceDownloadSize(size);
            } else {
                log.error("read file {} not exist in storage", fullName);
                putMsg(result, Status.RESOURCE_FILE_NOT_EXIST, fullName);
                return result;
            }

        } catch (Exception e) {
            log.error("Resource {} read failed", fullName, e);
            putMsg(result, Status.HDFS_OPERATION_ERROR);
            return result;
        }

        putMsg(result, Status.SUCCESS);
        Map<String, Object> map = new HashMap<>();
        map.put(ALIAS, fullName);
        map.put(CONTENT, String.join("\n", content));
        result.setData(map);

        return result;
    }

    /**
     * create resource file online
     *
     * @param loginUser  login user
     * @param type       resource type
     * @param fileName   file name
     * @param fileSuffix file suffix
     * @param content    content
     * @param currentDir current directory
     * @return create result code
     */
    @Override
    @Transactional
    public Result<Object> createResourceFile(User loginUser, ResourceType type, String fileName, String fileSuffix,
                                             String content, String currentDir) {
        Result<Object> result = new Result<>();

        User user = userMapper.selectById(loginUser.getId());
        if (user == null) {
            log.error("user {} not exists", loginUser.getId());
            putMsg(result, Status.USER_NOT_EXIST, loginUser.getId());
            return result;
        }

        String tenantCode = getTenantCode(user);
        checkFullName(tenantCode, currentDir);

        if (FileUtils.directoryTraversal(fileName)) {
            log.warn("File name verify failed, fileName:{}.", RegexUtils.escapeNRT(fileName));
            putMsg(result, Status.VERIFY_PARAMETER_NAME_FAILED);
            return result;
        }

        // check file suffix
        String nameSuffix = fileSuffix.trim();
        String resourceViewSuffixes = FileUtils.getResourceViewSuffixes();
        if (StringUtils.isNotEmpty(resourceViewSuffixes)) {
            List<String> strList = Arrays.asList(resourceViewSuffixes.split(","));
            if (!strList.contains(nameSuffix)) {
                log.warn("Resource suffix does not support view, suffix:{}.", nameSuffix);
                putMsg(result, Status.RESOURCE_SUFFIX_NOT_SUPPORT_VIEW);
                return result;
            }
        }

        String name = fileName.trim() + "." + nameSuffix;

        String userResRootPath = storageOperate.getResDir(tenantCode);
        String fullName = currentDir.contains(userResRootPath) ? currentDir + name : userResRootPath + name;

        result = verifyResourceName(fullName, type, loginUser);
        if (!result.getCode().equals(Status.SUCCESS.getCode())) {
            return result;
        }

        result = uploadContentToStorage(fullName, tenantCode, content);
        if (!result.getCode().equals(Status.SUCCESS.getCode())) {
            throw new ServiceException(result.getMsg());
        }
        return result;
    }

    @Override
    @Transactional
    public StorageEntity createOrUpdateResource(String userName, String filepath,
                                                String resourceContent) throws Exception {
        User user = userMapper.queryByUserNameAccurately(userName);
        int suffixLabelIndex = filepath.indexOf(PERIOD);
        if (suffixLabelIndex == -1) {
            throw new IllegalArgumentException(String
                    .format("Not allow create or update resources without extension name, filepath: %s", filepath));
        }

        String defaultPath = storageOperate.getResDir(user.getTenantCode());
        String fullName = defaultPath + filepath;

        Result<Object> result = uploadContentToStorage(fullName, user.getTenantCode(), resourceContent);
        if (result.getCode() != Status.SUCCESS.getCode()) {
            throw new ServiceException(result.getMsg());
        }
        return storageOperate.getFileStatus(fullName, defaultPath, user.getTenantCode(), ResourceType.FILE);
    }

    /**
     * updateProcessInstance resource
     *
     * @param fullName      resource full name
     * @param resTenantCode tenantCode in the request field "resTenantCode" for tenant code owning the resource,
     *                      can be different from the login user in the case of logging in as admin users.
     * @param content       content
     * @return update result cod
     */
    @Override
    @Transactional
    public Result<Object> updateResourceContent(User loginUser, String fullName, String resTenantCode, String content) {
        Result<Object> result = new Result<>();
        User user = userMapper.selectById(loginUser.getId());
        if (user == null) {
            log.error("user {} not exists", loginUser.getId());
            putMsg(result, Status.USER_NOT_EXIST, loginUser.getId());
            return result;
        }

        String tenantCode = getTenantCode(user);
        checkFullName(tenantCode, fullName);

        if (!isUserTenantValid(isAdmin(loginUser), tenantCode, resTenantCode)) {
            log.error("current user does not have permission");
            putMsg(result, Status.NO_CURRENT_OPERATING_PERMISSION);
            return result;
        }

        StorageEntity resource;
        try {
            resource = storageOperate.getFileStatus(fullName, "", resTenantCode, ResourceType.FILE);
        } catch (Exception e) {
            log.error("error occurred when fetching resource information ,  resource full name {}", fullName);
            putMsg(result, Status.RESOURCE_NOT_EXIST);
            return result;
        }

        if (resource == null) {
            log.error("Resource does not exist, resource full name:{}.", fullName);
            putMsg(result, Status.RESOURCE_NOT_EXIST);
            return result;
        }

        // check can edit by file suffix
        String nameSuffix = Files.getFileExtension(resource.getAlias());
        String resourceViewSuffixes = FileUtils.getResourceViewSuffixes();
        if (StringUtils.isNotEmpty(resourceViewSuffixes)) {
            List<String> strList = Arrays.asList(resourceViewSuffixes.split(","));
            if (!strList.contains(nameSuffix)) {
                log.warn("Resource suffix does not support view, resource full name:{}, suffix:{}.", fullName,
                        nameSuffix);
                putMsg(result, Status.RESOURCE_SUFFIX_NOT_SUPPORT_VIEW);
                return result;
            }
        }

        result = uploadContentToStorage(resource.getFullName(), resTenantCode, content);

        if (!result.getCode().equals(Status.SUCCESS.getCode())) {
            throw new ServiceException(result.getMsg());
        } else
            log.info("Update resource content complete, resource full name:{}.", fullName);
        return result;
    }

    /**
     * @param fullName   resource full name
     * @param tenantCode tenant code
     * @param content    content
     * @return result
     */
    private Result<Object> uploadContentToStorage(String fullName, String tenantCode, String content) {
        Result<Object> result = new Result<>();
        String localFilename = "";
        try {
            localFilename = FileUtils.getUploadFilename(tenantCode, UUID.randomUUID().toString());

            if (!FileUtils.writeContent2File(content, localFilename)) {
                // write file fail
                log.error("Write file error, fileName:{}, content:{}.", localFilename, RegexUtils.escapeNRT(content));
                putMsg(result, Status.RESOURCE_NOT_EXIST);
                return result;
            }

            // get resource file path
            String resourcePath = storageOperate.getResDir(tenantCode);
            log.info("resource  path is {}, resource dir is {}", fullName, resourcePath);

            if (!storageOperate.exists(resourcePath)) {
                // create if tenant dir not exists
                storageOperate.createTenantDirIfNotExists(tenantCode);
                log.info("Create tenant dir because path {} does not exist, tenantCode:{}.", resourcePath, tenantCode);
            }
            if (storageOperate.exists(fullName)) {
                storageOperate.delete(fullName, false);
            }

            storageOperate.upload(tenantCode, localFilename, fullName, true, true);
        } catch (Exception e) {
            log.error("Upload content to storage error, tenantCode:{}, destFileName:{}.", tenantCode, localFilename, e);
            result.setCode(Status.HDFS_OPERATION_ERROR.getCode());
            result.setMsg(String.format("copy %s to hdfs %s fail", localFilename, fullName));
            return result;
        } finally {
            FileUtils.deleteFile(localFilename);
        }
        log.info("Upload content to storage complete, tenantCode:{}, destFileName:{}.", tenantCode, localFilename);
        putMsg(result, Status.SUCCESS);
        return result;
    }

    /**
     * download file
     *
     * @return resource content
     */
    @Override
    public Resource downloadResource(User loginUser, String fullName) {
        if (fullName.endsWith("/")) {
            log.error("resource id {} is directory,can't download it", fullName);
            throw new ServiceException("can't download directory");
        }

        String userId = loginUser.getId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.error("User does not exits, userId:{}.", userId);
            throw new ServiceException(String.format("Resource owner id %d does not exist", userId));
        }

        String tenantCode = getTenantCode(user);
        checkFullName(tenantCode, fullName);

        String[] aliasArr = fullName.split("/");
        String alias = aliasArr[aliasArr.length - 1];
        String localFileName = FileUtils.getDownloadFilename(alias);
        log.info("Resource path is {}, download local filename is {}", alias, localFileName);

        try {
            storageOperate.download(fullName, localFileName, true);
            ApiServerMetrics.recordApiResourceDownloadSize(java.nio.file.Files.size(Paths.get(localFileName)));
            return org.apache.dolphinscheduler.api.utils.FileUtils.file2Resource(localFileName);
        } catch (IOException e) {
            log.error("Download resource error, the path is {}, and local filename is {}, the error message is {}",
                    fullName, localFileName, e.getMessage());
            throw new ServiceException("Download the resource file failed ,it may be related to your storage");
        }
    }

    @Override
    public StorageEntity queryFileStatus(String userName, String fileName) throws Exception {
        // TODO: It is used in PythonGateway, should be revised
        User user = userMapper.queryByUserNameAccurately(userName);

        String defaultPath = storageOperate.getResDir(user.getTenantCode());
        return storageOperate.getFileStatus(defaultPath + fileName, defaultPath, user.getTenantCode(),
                ResourceType.FILE);
    }

    @Override
    public DeleteDataTransferResponse deleteDataTransferData(User loginUser, Integer days) {
        DeleteDataTransferResponse result = new DeleteDataTransferResponse();

        User user = userMapper.selectById(loginUser.getId());
        if (user == null) {
            log.error("user {} not exists", loginUser.getId());
            putMsg(result, Status.USER_NOT_EXIST, loginUser.getId());
            return result;
        }

        String tenantCode = getTenantCode(user);

        String baseFolder = storageOperate.getResourceFullName(tenantCode, "DATA_TRANSFER");

        LocalDateTime now = LocalDateTime.now();
        now = now.minus(days, ChronoUnit.DAYS);
        String deleteDate = now.toLocalDate().toString().replace("-", "");
        List<StorageEntity> storageEntities;
        try {
            storageEntities = new ArrayList<>(
                    storageOperate.listFilesStatus(baseFolder, baseFolder, tenantCode, ResourceType.FILE));
        } catch (Exception e) {
            log.error("delete data transfer data error", e);
            putMsg(result, Status.DELETE_RESOURCE_ERROR);
            return result;
        }

        List<String> successList = new ArrayList<>();
        List<String> failList = new ArrayList<>();

        for (StorageEntity storageEntity : storageEntities) {
            File path = new File(storageEntity.getFullName());
            String date = path.getName();
            if (date.compareTo(deleteDate) <= 0) {
                try {
                    storageOperate.delete(storageEntity.getFullName(), true);
                    successList.add(storageEntity.getFullName());
                } catch (Exception ex) {
                    log.error("delete data transfer data {} error, please delete it manually", date, ex);
                    failList.add(storageEntity.getFullName());
                }
            }
        }

        result.setSuccessList(successList);
        result.setFailedList(failList);
        putMsg(result, Status.SUCCESS);
        return result;
    }

    /**
     * get resource base dir
     *
     * @param loginUser login user
     * @param type      resource type
     * @return
     */
    @Override
    public Result<Object> queryResourceBaseDir(User loginUser, ResourceType type) {
        Result<Object> result = new Result<>();
        if (storageOperate == null) {
            putMsg(result, Status.SUCCESS);
            result.setData(EMPTY_STRING);
            return result;
        }
        User user = userMapper.selectById(loginUser.getId());
        if (user == null) {
            log.error("user {} not exists", loginUser.getId());
            putMsg(result, Status.USER_NOT_EXIST, loginUser.getId());
            return result;
        }

        String tenantCode = getTenantCode(user);

        String baseDir = isAdmin(loginUser) ? storageOperate.getDir(ResourceType.ALL, tenantCode)
                : storageOperate.getDir(type, tenantCode);

        putMsg(result, Status.SUCCESS);
        result.setData(baseDir);

        return result;
    }

    /**
     * check permission by comparing login user's tenantCode with tenantCode in the request
     *
     * @param isAdmin        is the login user admin
     * @param userTenantCode loginUser's tenantCode
     * @param resTenantCode  tenantCode in the request field "resTenantCode" for tenant code owning the resource,
     *                       can be different from the login user in the case of logging in as admin users.
     * @return isValid
     */
    private boolean isUserTenantValid(boolean isAdmin, String userTenantCode,
                                      String resTenantCode) throws ServiceException {
        if (isAdmin) {
            return true;
        }
        if (StringUtils.isEmpty(resTenantCode)) {
            // TODO: resource tenant code will be empty when query resources list, need to be optimized
            return true;
        }
        return resTenantCode.equals(userTenantCode);
    }

    private String getTenantCode(User user) {
        Tenant tenant = tenantMapper.queryById(user.getTenantId());
        if (tenant == null) {
            throw new ServiceException(Status.CURRENT_LOGIN_USER_TENANT_NOT_EXIST);
        }
        return tenant.getTenantCode();
    }

    private void checkFullName(String userTenantCode, String fullName) {
        if (StringUtils.isEmpty(fullName)) {
            return;
        }
        if (FOLDER_SEPARATOR.equalsIgnoreCase(fullName)) {
            return;
        }
        // Avoid returning to the parent directory
        if (fullName.contains("../")) {
            throw new ServiceException(Status.ILLEGAL_RESOURCE_PATH, fullName);
        }
        String baseDir = storageOperate.getDir(ResourceType.ALL, userTenantCode);
        if (!StringUtils.startsWith(fullName, baseDir)) {
            throw new ServiceException(Status.ILLEGAL_RESOURCE_PATH, fullName);
        }
    }
}
