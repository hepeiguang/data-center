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

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.api.dto.taskType.TaskConfig;
import org.apache.dolphinscheduler.api.service.FavTaskService;
import org.apache.dolphinscheduler.api.utils.AdvancedTaskYamlParser;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.dao.entity.FavTask;
import org.apache.dolphinscheduler.dao.entity.User;
import org.apache.dolphinscheduler.dao.mapper.FavTaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class FavTaskServiceImpl extends BaseServiceImpl implements FavTaskService {

    //    @Resource
//    private TaskTypeConfiguration taskTypeConfiguration;
    @Resource
    private FavTaskMapper favMapper;

    private AdvancedTaskYamlParser advancedTaskYamlParser = new AdvancedTaskYamlParser();

    private List<TaskConfig> taskConfigs = new ArrayList<>();

    @Autowired
    private ResourceLoader resourceLoader;

    @Override
    public List<TaskConfig> getFavTaskList(User loginUser) {
        if (!CollectionUtils.isEmpty(taskConfigs)) {
            return taskConfigs;
        }
        Set<String> userFavTaskTypes = favMapper.getUserFavTaskTypes(loginUser.getId());
        org.springframework.core.io.Resource resource = resourceLoader.getResource("classpath:task-type-config.yaml");
        try (InputStream inputStream = resource.getInputStream()) {
            List<TaskConfig> defaultTaskTypes = advancedTaskYamlParser.parseYaml(inputStream);
            // clone default list and modify fav task type flag
            log.info("defaultTaskTypes: {}", JSONUtils.toJsonString(defaultTaskTypes));
            defaultTaskTypes.forEach(e -> {
                e.getChildren().forEach(child -> {
                    if (userFavTaskTypes.contains(child.getTaskType())) {
                        child.setCollection(true);
                    }
                });
                taskConfigs.add(e);
            });
        } catch (IOException ex) {
            log.error("Failed to parse to file", ex);
        }
        return taskConfigs;
    }

    @Override
    public boolean deleteFavTask(User loginUser, String taskType) {
        return favMapper.deleteUserFavTask(loginUser.getId(), taskType);
    }

    @Override
    public int addFavTask(User loginUser, String taskType) {
        favMapper.deleteUserFavTask(loginUser.getId(), taskType);
        return favMapper.insert(new FavTask(null, taskType, loginUser.getId()));
    }
}
