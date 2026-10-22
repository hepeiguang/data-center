package org.apache.dolphinscheduler.plugin.task.minio.dto;

import lombok.Data;

@Data
public class QueueFile {
    private String prefix;
    private Long DirId;
    private String DirPath;
    private String DirName;
}
