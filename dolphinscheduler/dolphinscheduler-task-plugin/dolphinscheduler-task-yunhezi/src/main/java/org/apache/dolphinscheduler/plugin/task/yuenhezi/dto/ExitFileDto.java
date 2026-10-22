package org.apache.dolphinscheduler.plugin.task.yuenhezi.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ExitFileDto implements Serializable {

    /**
     * 云盒子的文件id
     */
    private Long refFileId;

    private Long parentId;
    /**
     * metadata_file对应的id
     */
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastUpdateTime;
}
