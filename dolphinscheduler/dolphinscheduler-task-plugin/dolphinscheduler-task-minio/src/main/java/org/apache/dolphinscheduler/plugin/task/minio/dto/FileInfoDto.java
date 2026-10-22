package org.apache.dolphinscheduler.plugin.task.minio.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.apache.dolphinscheduler.plugin.task.minio.dto.FieldInfoDto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 文件类元数据
 * </p>
 *
 * @author cui_com
 * @since 2022-03-04
 */
@Data
public class FileInfoDto {

    private Long id;
    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件夹路径
     */
    private String filePath;

    /**
     * 结构化类型(结构化,半结构化,非结构化)
     */
    private String structureType;

    /**
     * 文件类型(结构化类型xlsx,txt,orc,xls,csv;半结构化:log;非结构化:word,pdf,jpg)
     */
    private String fileType;

    /**
     * 字符集(ISO-8859-1,UTF-16,UTF-8,GBK,GBK2312,UTF-16LE)
     */
    private String charset;

    /**
     * 文件来源(服务器id)
     */
    private String sourceId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastUpdateTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    private String updateBy;

    /**
     * 文件字段信息
     */
    private List<FieldInfoDto> columns;

    private Integer level;

    private Long parentId;
    private Long refFldId;
    private Long refParentId;
    /**
     * 文件字段类型(文件字段,文件夹字段)
     */
    private Integer fldType;
    private String fileIdPath;
    @JsonProperty("fileSize")
    private Long fileSize;

    @JsonProperty("dir_name")
    private String dirName;

    public FileInfoDto(){

    }

    public FileInfoDto(Long id, Long parentId,Long refFldId,Long  refParentId, String fileName, String filePath,
                       String fileIdPath, Integer fldType, String structureType, String fileType, String charset,
                       String sourceId,  LocalDateTime lastUpdateTime, String createBy, Long fileSize, String dirName) {
        this.id = id;
        this.parentId = parentId;
        this.refFldId = refFldId;
        this.refParentId = refParentId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileIdPath = fileIdPath;
        this.fldType = fldType;
        this.structureType = structureType;
        this.fileType = fileType;
        this.charset = charset;
        this.sourceId = sourceId;
        this.lastUpdateTime = lastUpdateTime;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        this.createBy = createBy;
        this.updateBy = createBy;
        this.fileSize = fileSize;
        this.dirName = dirName;
    }
}
