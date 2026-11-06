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

package org.apache.dolphinscheduler.api.controller;

import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.exceptions.ApiException;
import org.apache.dolphinscheduler.api.service.SeaTunnelConnectorService;
import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.common.constants.Constants;
import org.apache.dolphinscheduler.dao.entity.User;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * SeaTunnel Connector-V2 catalog for 数据计算 visual orchestration.
 */
@Tag(name = "SEATUNNEL_CONNECTOR_TAG")
@RestController
@RequestMapping("/seatunnel")
public class SeaTunnelConnectorController extends BaseController {

    @Resource
    private SeaTunnelConnectorService seaTunnelConnectorService;

    @Operation(summary = "listConnectors", description = "List SeaTunnel V2 source/transform/sink connectors")
    @GetMapping(value = "/connectors")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(Status.LIST_TASK_TYPE_ERROR)
    public Result listConnectors(
                                 @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser) {
        Map<String, Object> catalog = seaTunnelConnectorService.getConnectorCatalog();
        return success(Status.SUCCESS.getMsg(), catalog);
    }

    @Operation(summary = "listUdpCodecs", description = "List UDP UDF codecs under SEATUNNEL_HOME/udf/udp-codec")
    @GetMapping(value = "/udp-codecs")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(Status.LIST_TASK_TYPE_ERROR)
    public Result listUdpCodecs(
                                @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser) {
        List<Map<String, Object>> list = seaTunnelConnectorService.listUdpCodecs();
        return success(Status.SUCCESS.getMsg(), list);
    }

    @Operation(summary = "uploadUdpCodec", description = "Upload JS / offset-table JSON UDF for UDP Source")
    @PostMapping(value = "/udp-codecs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiException(Status.REQUEST_PARAMS_NOT_VALID_ERROR)
    public Result uploadUdpCodec(
                                 @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                 @RequestParam("file") MultipartFile file,
                                 @RequestParam(value = "name", required = false) String name) {
        Map<String, Object> saved = seaTunnelConnectorService.uploadUdpCodec(file, name);
        return success(Status.SUCCESS.getMsg(), saved);
    }
}
