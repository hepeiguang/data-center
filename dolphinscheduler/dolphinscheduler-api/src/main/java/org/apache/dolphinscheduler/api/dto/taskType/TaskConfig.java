package org.apache.dolphinscheduler.api.dto.taskType;

import java.util.List;

public class TaskConfig {
    private String taskType;
    private String taskName;
    private String jobMode;
    private List<TaskConfig> children;

    private boolean isCollection;

    // Getters and Setters
    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getJobMode() {
        return jobMode;
    }

    public void setJobMode(String jobMode) {
        this.jobMode = jobMode;
    }

    public List<TaskConfig> getChildren() {
        return children;
    }

    public void setChildren(List<TaskConfig> children) {
        this.children = children;
    }

    public boolean isCollection() {
        return isCollection;
    }

    public void setCollection(boolean collection) {
        isCollection = collection;
    }

    @Override
    public String toString() {
        return "TaskConfig{" +
                "taskType='" + taskType + '\'' +
                ", taskName='" + taskName + '\'' +
                ", jobMode='" + jobMode + '\'' +
                ", children=" + children +
                '}';
    }
}
