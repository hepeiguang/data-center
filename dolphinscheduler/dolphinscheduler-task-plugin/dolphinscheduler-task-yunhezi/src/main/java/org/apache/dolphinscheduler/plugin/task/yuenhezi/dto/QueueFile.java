package org.apache.dolphinscheduler.plugin.task.yuenhezi.dto;

import lombok.Data;

@Data
public class QueueFile {
    /**
     * 云盒子的文件id
     */
    private Long refFileId;
    /**
     * metadata_file对应的id
     */
    private Long id;
    /**
     * 文件路径
     */
    private String filePath;
    /**
     * 组装成dam的id
     */
    private String fileIdPath;
}
