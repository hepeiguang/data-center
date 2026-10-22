package org.apache.dolphinscheduler.plugin.task.minio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class BucketUsageDto {
    private long totalSize;
    private long objectCount;

    public BucketUsageDto(long totalSize, long objectCount) {
        this.objectCount = objectCount;
        this.totalSize = totalSize;
    }
}
