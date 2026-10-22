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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.common.constants.Constants;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.dao.entity.User;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.SeatunnelPluginManager;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.OutputFieldDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * access token controller
 */
@RestController
@RequestMapping("/seatunnelTask")
@Slf4j
public class SeatunnelController extends BaseController {

    /**
     * getSeatunnel Task Output
     *
     * @param vo seatunnelTaskNodeVO
     * @return create result state code
     */
    @Operation(summary = "getSeatunnelTaskOutput", description = "getSeatunnelTaskOutput")
    @PostMapping(value="/getSeatunnelTaskOutput")
    @ResponseStatus(HttpStatus.OK)
    public Result<List<OutputFieldDTO>> getSeatunnelTaskOutput(@Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                                               @RequestBody SeatunnelTaskNodeVO vo) {
        log.info("getSeatunnelTaskOutput in：{}", JSONUtils.toJsonString(vo));
        TaskTypeFactory factory = SeatunnelPluginManager.getTaskTypeFactory(vo.getType(), vo.getPluginName());
        log.info("getSeatunnelTaskOutput out：{}", JSONUtils.toJsonString(factory.getOutputFields(vo)));
        return success(factory.getOutputFields(vo));
    }


}
