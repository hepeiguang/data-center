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

import org.apache.dolphinscheduler.api.constants.ApiFuncIdentificationConstant;
import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.service.UdfFuncService;
import org.apache.dolphinscheduler.api.utils.PageInfo;
import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.common.enums.AuthorizationType;
import org.apache.dolphinscheduler.common.enums.UdfType;
import org.apache.dolphinscheduler.dao.entity.FunctionInfoCfg;
import org.apache.dolphinscheduler.dao.entity.UdfFunc;
import org.apache.dolphinscheduler.dao.entity.User;
import org.apache.dolphinscheduler.dao.mapper.FunctionInfoCfgMapper;
import org.apache.dolphinscheduler.dao.mapper.UDFUserMapper;
import org.apache.dolphinscheduler.dao.mapper.UdfFuncMapper;
import org.apache.dolphinscheduler.plugin.storage.api.StorageOperate;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * udf func service impl
 */
@Service
@Slf4j
public class UdfFuncServiceImpl extends BaseServiceImpl implements UdfFuncService {

    public static final String HIVE_UDF = "HIVE";
    public static final String SEATUNNEL_UDF = "SEATUNNEL";
    public static final String FILE_RESOURCE = "FILE";
    public static final String UDF_RESOURCE = "UDF";

    @Value("${seatunnel.plugin:}")
    private String seatunnelPluginPath;

    /** UDP JS / 偏移表脚本热插拔目录，默认 $SEATUNNEL_HOME/udf/udp-codec */
    @Value("${seatunnel.udp-codec:}")
    private String seatunnelUdpCodecPath;

    @Value("${seatunnel.home:}")
    private String seatunnelHome;

    @Autowired
    private UdfFuncMapper udfFuncMapper;

    @Autowired
    private UDFUserMapper udfUserMapper;

    @Autowired(required = false)
    private StorageOperate storageOperate;

    @Autowired
    private FunctionInfoCfgMapper functionInfoCfgMapper;

    /**
     * create udf function
     *
     * @param loginUser login user
     * @param type udf type
     * @param funcName function name
     * @param argTypes argument types
     * @param database database
     * @param desc description
     * @param className class name
     * @return create result code
     */
    @Override
    @Transactional
    public Result<Object> createUdfFunction(User loginUser,
                                            String funcName,
                                            String className,
                                            String fullName,
                                            String argTypes,
                                            String database,
                                            String desc,
                                            UdfType type,
                                            //增加 参数字段
                                            String list) {
        Result<Object> result = new Result<>();

        //检查用户是否有权限
        boolean canOperatorPermissions = canOperatorPermissions(loginUser, null, AuthorizationType.UDF,
                ApiFuncIdentificationConstant.UDF_FUNCTION_CREATE);
        if (!canOperatorPermissions) {
            putMsg(result, Status.NO_CURRENT_OPERATING_PERMISSION);
            return result;
        }
        //描述过长
        if (checkDescriptionLength(desc)) {
            log.warn("Parameter description is too long.");
            putMsg(result, Status.DESCRIPTION_TOO_LONG_ERROR);
            return result;
        }

        // verify udf func name exist
        // 检查udf函数名称是否存在
        if (checkUdfFuncNameExists(funcName)) {
            log.warn("Udf function with the same name already exists.");
            putMsg(result, Status.UDF_FUNCTION_EXISTS);
            return result;
        }

        Boolean existResource = false;
        try {
            existResource = storageOperate.exists(fullName);
        } catch (IOException e) {
            log.error("Check resource error: {}", fullName, e);
        }

        if (!existResource) {
            log.error("resource full name {} is not exist", fullName);
            putMsg(result, Status.RESOURCE_NOT_EXIST);
            return result;
        }

        // save data
        UdfFunc udf = new UdfFunc();
        Date now = new Date();
        udf.setUserId(loginUser.getId());
        udf.setFuncName(funcName);
        udf.setClassName(className);
        if (!StringUtils.isEmpty(argTypes)) {
            udf.setArgTypes(argTypes);
        }
        if (!StringUtils.isEmpty(database)) {
            udf.setDatabase(database);
        }
        udf.setDescription(desc);
        // set resourceId to -1 because we do not store resource to db anymore, instead we use fullName
        udf.setResourceId(-1);
        udf.setResourceName(fullName);
        //增加自定义类型
        udf.setType(type);
        udf.setCreateTime(now);
        udf.setUpdateTime(now);
        udf.setList(list);
        udfFuncMapper.insert(udf);
        if (SEATUNNEL_UDF.equalsIgnoreCase(type.name())) {
            //加载当前jar包到home/lib   plugin.property文件夹
            transferFileToSeatunnelDir(fullName, seatunnelPluginPath);
            //todo  追加
            //在这里尝试上传到func_info_cfg 表
            FunctionInfoCfg functionInfoCfg = new FunctionInfoCfg();
            functionInfoCfg.setId(null);
            functionInfoCfg.setFunctionType("4");
            functionInfoCfg.setFunctionName(funcName);
            functionInfoCfg.setFunctionCode(className);
            //设置排序
            Integer length = functionInfoCfgMapper.queryFunctionInfoCfgNumByTypeName("自定义函数");
            functionInfoCfg.setSort(length+1);
            functionInfoCfg.setFunctionRemark(desc);
            functionInfoCfg.setFunctionAttribute(list);
            functionInfoCfg.setStatus(1);
            functionInfoCfg.setCreateTime(now);
            functionInfoCfg.setTypeName("自定义函数");
            functionInfoCfg.setUdfsId(udf.getId());
            functionInfoCfgMapper.insert(functionInfoCfg);
        }
        log.info("UDF function create complete, udfFuncName:{}.", udf.getFuncName());
        putMsg(result, Status.SUCCESS);
        return result;
    }
    /**
     * 将文件从源路径传输到 seatunnelPath 指定的目录
     *
     * @param fullName 源文件路径
     * @param seatunnelLibPath 目标目录路径
     */
    private void transferFileToSeatunnelDir(String fullName, String seatunnelLibPath) {
        if (seatunnelLibPath == null || seatunnelLibPath.isEmpty()) {
            log.error("Seatunnel path is empty.");
            return;
        }
        try {
            Path targetDirPath = Paths.get(seatunnelLibPath);
            if (!Files.exists(targetDirPath)) {
                Files.createDirectories(targetDirPath);
            }
            Path sourceFilePath = Paths.get(URI.create(fullName));
            Path targetFilePath = targetDirPath.resolve(sourceFilePath.getFileName());
            Files.copy(sourceFilePath, targetFilePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            log.info("File transferred successfully from {} to {}", fullName, targetFilePath);

            // UDP 解析脚本 / 偏移表：同步到 udf/udp-codec，供 parseMode=js|offset_table 热加载
            String fileName = sourceFilePath.getFileName().toString().toLowerCase();
            if (fileName.endsWith(".js")
                    || fileName.endsWith(".json")
                    || fileName.endsWith(".py")) {
                Path codecDir = resolveUdpCodecDir();
                if (codecDir != null) {
                    if (!Files.exists(codecDir)) {
                        Files.createDirectories(codecDir);
                    }
                    Path codecTarget = codecDir.resolve(sourceFilePath.getFileName());
                    Files.copy(sourceFilePath, codecTarget, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    log.info("UDP codec file synced to {}", codecTarget);
                }
            }
        } catch (IOException e) {
            log.error("Failed to transfer file from {} to seatunnel directory", fullName, e);
        }
    }

    private Path resolveUdpCodecDir() {
        if (seatunnelUdpCodecPath != null && !seatunnelUdpCodecPath.trim().isEmpty()) {
            return Paths.get(seatunnelUdpCodecPath.trim());
        }
        if (seatunnelHome != null && !seatunnelHome.trim().isEmpty()) {
            return Paths.get(seatunnelHome.trim(), "udf", "udp-codec");
        }
        String envHome = System.getenv("SEATUNNEL_HOME");
        if (envHome != null && !envHome.trim().isEmpty()) {
            return Paths.get(envHome.trim(), "udf", "udp-codec");
        }
        log.warn("seatunnel.udp-codec / seatunnel.home / SEATUNNEL_HOME not configured, skip UDP codec sync");
        return null;
    }

    /**
     *
     * @param name name
     * @return check result code
     */
    private boolean checkUdfFuncNameExists(String name) {
        List<UdfFunc> resource = udfFuncMapper.queryUdfByIdStr(null, name);
        return resource != null && !resource.isEmpty();
    }

    /**
     * query udf function
     *
     * @param id  udf function id
     * @return udf function detail
     */
    @Override
    public Result<Object> queryUdfFuncDetail(User loginUser, int id) {
        Result<Object> result = new Result<>();
        boolean canOperatorPermissions = canOperatorPermissions(loginUser, new Object[]{id}, AuthorizationType.UDF,
                ApiFuncIdentificationConstant.UDF_FUNCTION_VIEW);
        if (!canOperatorPermissions) {
            putMsg(result, Status.NO_CURRENT_OPERATING_PERMISSION);
            return result;
        }
        UdfFunc udfFunc = udfFuncMapper.selectById(id);
        if (udfFunc == null) {
            log.error("Resource does not exist, udf func id:{}.", id);
            putMsg(result, Status.RESOURCE_NOT_EXIST);
            return result;
        }
        result.setData(udfFunc);
        putMsg(result, Status.SUCCESS);
        return result;
    }

    /**
     * updateProcessInstance udf function
     *
     * @param udfFuncId udf function id
     * @param type  resource type
     * @param funcName function name
     * @param argTypes argument types
     * @param database data base
     * @param desc description
     * @param fullName resource full name
     * @param className class name
     * @return update result code
     */
    @Override
    public Result<Object> updateUdfFunc(User loginUser,
                                        int udfFuncId,
                                        String funcName,
                                        String className,
                                        String argTypes,
                                        String database,
                                        String desc,
                                        UdfType type,
                                        String fullName,
                                        String list) {
        Result<Object> result = new Result<>();

        boolean canOperatorPermissions = canOperatorPermissions(loginUser, new Object[]{udfFuncId},
                AuthorizationType.UDF, ApiFuncIdentificationConstant.UDF_FUNCTION_UPDATE);
        if (!canOperatorPermissions) {
            putMsg(result, Status.NO_CURRENT_OPERATING_PERMISSION);
            return result;
        }
        if (checkDescriptionLength(desc)) {
            log.warn("Parameter description is too long.");
            putMsg(result, Status.DESCRIPTION_TOO_LONG_ERROR);
            return result;
        }
        // verify udfFunc is exist
        UdfFunc udf = udfFuncMapper.selectUdfById(udfFuncId);
        String functionName = udf.getFuncName();

        if (udf == null) {
            log.error("UDF function does not exist, udfFuncId:{}.", udfFuncId);
            result.setCode(Status.UDF_FUNCTION_NOT_EXIST.getCode());
            result.setMsg(Status.UDF_FUNCTION_NOT_EXIST.getMsg());
            return result;
        }

        // verify udfFuncName is exist
        if (!funcName.equals(udf.getFuncName())) {
            if (checkUdfFuncNameExists(funcName)) {
                log.warn("Udf function exists, can not create again, udfFuncName:{}.", funcName);
                result.setCode(Status.UDF_FUNCTION_EXISTS.getCode());
                result.setMsg(Status.UDF_FUNCTION_EXISTS.getMsg());
                return result;
            }
        }

        Boolean doesResExist = false;
        try {
            doesResExist = storageOperate.exists(fullName);
        } catch (Exception e) {
            log.error("udf resource :{} checking error", fullName, e);
            result.setCode(Status.RESOURCE_NOT_EXIST.getCode());
            result.setMsg(Status.RESOURCE_NOT_EXIST.getMsg());
            return result;
        }

        if (!doesResExist) {
            log.error("resource full name {} is not exist", fullName);
            result.setCode(Status.RESOURCE_NOT_EXIST.getCode());
            result.setMsg(Status.RESOURCE_NOT_EXIST.getMsg());
            return result;
        }

        Date now = new Date();
        udf.setFuncName(funcName);
        udf.setClassName(className);
        udf.setArgTypes(argTypes);
        if (!StringUtils.isEmpty(database)) {
            udf.setDatabase(database);
        }
        udf.setDescription(desc);
        // set resourceId to -1 because we do not store resource to db anymore, instead we use fullName
        udf.setResourceId(-1);
        udf.setResourceName(fullName);
        udf.setType(type);

        udf.setUpdateTime(now);

        //传入新的list
        udf.setList(list);
        //todo 校验list

        udfFuncMapper.updateById(udf);
        log.info("UDF function update complete, udfFuncId:{}, udfFuncName:{}.", udfFuncId, funcName);
        putMsg(result, Status.SUCCESS);

        if (SEATUNNEL_UDF.equalsIgnoreCase(type.name())) {
            //todo 修改当前jar包到home/lib   plugin.property文件夹
            transferFileToSeatunnelDir(fullName, seatunnelPluginPath);
            //todo 在这里尝试修改到func_info_cfg 表
//            UdfFunc udfFunc = udfFuncMapper.selectUdfById(udfFuncId);
            FunctionInfoCfg functionInfoCfg = functionInfoCfgMapper.queryByUdfsId(udfFuncId);
            functionInfoCfg.setFunctionName(funcName);
            functionInfoCfg.setFunctionCode(className);
            functionInfoCfg.setFunctionRemark(desc);
            functionInfoCfg.setFunctionAttribute(list);
            functionInfoCfgMapper.fixFunctionInfoCfg(functionInfoCfg);
        }
        return result;
    }

    /**
     * query udf function list paging
     *
     * @param loginUser login user
     * @param pageNo page number
     * @param pageSize page size
     * @param searchVal search value
     * @return udf function list page
     */
    @Override

    //todo mapper里增加了返回结果待测试
    public Result<Object> queryUdfFuncListPaging(User loginUser, String searchVal, Integer pageNo, Integer pageSize) {
        Result<Object> result = new Result();
        boolean canOperatorPermissions = canOperatorPermissions(loginUser, null, AuthorizationType.UDF,
                ApiFuncIdentificationConstant.UDF_FUNCTION_VIEW);
        if (!canOperatorPermissions) {
            putMsg(result, Status.NO_CURRENT_OPERATING_PERMISSION);
            return result;
        }
        PageInfo<UdfFunc> pageInfo = new PageInfo<>(pageNo, pageSize);
        IPage<UdfFunc> udfFuncList = getUdfFuncsPage(loginUser, searchVal, pageSize, pageNo);
        pageInfo.setTotal((int) udfFuncList.getTotal());
        pageInfo.setTotalList(udfFuncList.getRecords());
        result.setData(pageInfo);
        putMsg(result, Status.SUCCESS);
        return result;
    }

    /**
     * get udf functions
     *
     * @param loginUser login user
     * @param searchVal search value
     * @param pageSize page size
     * @param pageNo page number
     * @return udf function list page
     */
    private IPage<UdfFunc> getUdfFuncsPage(User loginUser, String searchVal, Integer pageSize, int pageNo) {
        Set<Integer> udfFuncIds = resourcePermissionCheckService.userOwnedResourceIdsAcquisition(AuthorizationType.UDF,
                loginUser.getId(), log);
        Page<UdfFunc> page = new Page<>(pageNo, pageSize);
        if (udfFuncIds.isEmpty()) {
            return page;
        }
        return udfFuncMapper.queryUdfFuncPaging(page, new ArrayList<>(udfFuncIds), searchVal);
    }

    /**
     * query udf list
     *
     * @param loginUser login user
     * @param type  udf type
     * @return udf func list
     */
    @Override
    public Result<Object> queryUdfFuncList(User loginUser, Integer type) {
        Result<Object> result = new Result<>();

        boolean canOperatorPermissions = canOperatorPermissions(loginUser, null, AuthorizationType.UDF,
                ApiFuncIdentificationConstant.UDF_FUNCTION_VIEW);
        if (!canOperatorPermissions) {
            putMsg(result, Status.NO_CURRENT_OPERATING_PERMISSION);
            return result;
        }
        Set<Integer> udfFuncIds = resourcePermissionCheckService.userOwnedResourceIdsAcquisition(AuthorizationType.UDF,
                loginUser.getId(), log);
        if (udfFuncIds.isEmpty()) {
            result.setData(Collections.emptyList());
            putMsg(result, Status.SUCCESS);
            return result;
        }
        List<UdfFunc> udfFuncList = udfFuncMapper.getUdfFuncByType(new ArrayList<>(udfFuncIds), type);

        result.setData(udfFuncList);
        putMsg(result, Status.SUCCESS);
        return result;
    }

    /**
     * delete udf function
     *
     * @param id udf function id
     * @return delete result code
     */
    @Override
    @Transactional
    public Result<Object> delete(User loginUser, int id) {
        Result<Object> result = new Result<>();

        boolean canOperatorPermissions = canOperatorPermissions(loginUser, new Object[]{id}, AuthorizationType.UDF,
                ApiFuncIdentificationConstant.UDF_FUNCTION_DELETE);
        if (!canOperatorPermissions) {
            putMsg(result, Status.NO_CURRENT_OPERATING_PERMISSION);
            return result;
        }
        try{
            UdfFunc udfFunc = udfFuncMapper.selectUdfById(id);
            if (udfFunc.getType().equals (UdfType.valueOf( "SEATUNNEL"))) {
                //todo 删除当前jar包到home/lib   plugin.property文件夹

                //todo 在这里尝试删除到func_info_cfg 表
                functionInfoCfgMapper.deleteFunctionInfoCfgByUdfsId(id);
            }
        }catch(Exception exception){
            //todo
            exception.printStackTrace();
            log.error("删除udf失败");
        }
        udfFuncMapper.deleteById(id);
        udfUserMapper.deleteByUdfFuncId(id);
        log.info("UDF function delete complete, udfFuncId:{}.", id);

        putMsg(result, Status.SUCCESS);
        return result;
    }

    /**
     * verify udf function by name
     *
     * @param name name
     * @return true if the name can user, otherwise return false
     */
    @Override
    public Result<Object> verifyUdfFuncByName(User loginUser, String name) {
        Result<Object> result = new Result<>();
        boolean canOperatorPermissions = canOperatorPermissions(loginUser, null, AuthorizationType.UDF,
                ApiFuncIdentificationConstant.UDF_FUNCTION_VIEW);
        if (!canOperatorPermissions) {
            putMsg(result, Status.NO_CURRENT_OPERATING_PERMISSION);
            return result;
        }

        if (checkUdfFuncNameExists(name)) {
            log.warn("Udf function with the same already exists.");
            putMsg(result, Status.UDF_FUNCTION_EXISTS);
        } else {
            putMsg(result, Status.SUCCESS);
        }
        return result;
    }
}
