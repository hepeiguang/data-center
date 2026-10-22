package org.apache.dolphinscheduler.plugin.task.minio.dto;

import lombok.Data;

@Data
public class FileInfoExistence {
    private Long id;
    private Integer fldType;
    private String filePath;
}
