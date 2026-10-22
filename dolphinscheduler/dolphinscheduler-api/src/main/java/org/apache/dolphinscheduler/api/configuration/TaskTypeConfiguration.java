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

package org.apache.dolphinscheduler.api.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.dolphinscheduler.api.dto.FavTaskDto;
import org.apache.dolphinscheduler.common.config.YamlPropertySourceFactory;
import org.apache.dolphinscheduler.common.constants.Constants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@EnableConfigurationProperties
@PropertySource(value = {"classpath:task-type-config.yaml"}, factory = YamlPropertySourceFactory.class)
@ConfigurationProperties(prefix = "task")
@Getter
@Setter
@Slf4j
public class TaskTypeConfiguration {

    private List<TaskItem> universal;
    private List<TaskItem> cloud;
    private List<TaskItem> logic;
    private List<TaskItem> dataIntegration;
    private List<TaskItem> dataQuality;
    private List<TaskItem> other;
    private List<TaskItem> machineLearning;
    private List<TaskItem> seatunnel;
    private List<SourceItem> source;
    private List<TaskItem> sink;
    private List<TaskItem> transform;
    private List<TaskItem> metadata;

    private static final List<FavTaskDto> defaultTaskTypes = new ArrayList<>();

    public List<FavTaskDto> getDefaultTaskTypes() {
        if (CollectionUtils.isNotEmpty(defaultTaskTypes)) {
            return defaultTaskTypes;
        }
        printDefaultTypes();
        universal.forEach(task -> defaultTaskTypes.add(new FavTaskDto(task.getTaskType(), false, Constants.TYPE_UNIVERSAL, null)));
        cloud.forEach(task -> defaultTaskTypes.add(new FavTaskDto(task.getTaskType(), false, Constants.TYPE_CLOUD, null)));
        logic.forEach(task -> defaultTaskTypes.add(new FavTaskDto(task.getTaskType(), false, Constants.TYPE_LOGIC, null)));
        dataIntegration
                .forEach(task -> defaultTaskTypes.add(new FavTaskDto(task.getTaskType(), false, Constants.TYPE_DATA_INTEGRATION, null)));
        dataQuality.forEach(task -> defaultTaskTypes.add(new FavTaskDto(task.getTaskType(), false, Constants.TYPE_DATA_QUALITY, null)));
        machineLearning
                .forEach(task -> defaultTaskTypes.add(new FavTaskDto(task.getTaskType(), false, Constants.TYPE_MACHINE_LEARNING, null)));
        other.forEach(task -> defaultTaskTypes.add(new FavTaskDto(task.getTaskType(), false, Constants.TYPE_OTHER, null)));
        seatunnel.forEach(task -> defaultTaskTypes.add(new FavTaskDto(task.getTaskType(), false, Constants.TYPE_SEATUNNEL, null)));
        source.forEach(task -> defaultTaskTypes.add(new FavTaskDto(task.getTaskType(), false, Constants.TYPE_SOURCE, task.getJobMode())));
        sink.forEach(task -> defaultTaskTypes.add(new FavTaskDto(task.getTaskType(), false, Constants.TYPE_SINK, null)));
        transform.forEach(task -> defaultTaskTypes.add(new FavTaskDto(task.getTaskType(), false, Constants.TYPE_TRANSFORM, null)));
        metadata.forEach(task -> defaultTaskTypes.add(new FavTaskDto(task.getTaskType(), false, Constants.TYPE_METADATA, null)));
        return defaultTaskTypes;
    }

    public void printDefaultTypes() {
        log.info("support default universal task types: {}", universal);
        log.info("support default cloud task types: {}", cloud);
        log.info("support default logic task types: {}", logic);
        log.info("support default dataIntegration task types: {}", dataIntegration);
        log.info("support default dataQuality task types: {}", dataQuality);
        log.info("support default machineLearning task types: {}", machineLearning);
        log.info("support default seatunnel task types: {}", seatunnel);
        log.info("support default sink task types: {}", sink);
        log.info("support default transform task types: {}", transform);
        log.info("support default source task types: {}", source);
        log.info("support default metadata task types: {}", metadata);
    }

    // 通用任务项
    public static class TaskItem {
        private String taskType;

        public String getTaskType() {
            return taskType;
        }

        public void setTaskType(String taskType) {
            this.taskType = taskType;
        }
    }

    // 特殊任务项（如 Source）
    public static class SourceItem extends TaskItem {
        private String jobMode;

        public String getJobMode() {
            return jobMode;
        }

        public void setJobMode(String jobMode) {
            this.jobMode = jobMode;
        }
    }
}
