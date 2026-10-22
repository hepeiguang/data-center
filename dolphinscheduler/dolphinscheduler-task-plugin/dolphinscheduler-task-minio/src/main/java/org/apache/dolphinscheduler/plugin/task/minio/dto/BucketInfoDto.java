package org.apache.dolphinscheduler.plugin.task.minio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class BucketInfoDto {
    private String name;
    private String usage;
    private long objects;
    private String access;
    private LocalDate creationDate;
}
