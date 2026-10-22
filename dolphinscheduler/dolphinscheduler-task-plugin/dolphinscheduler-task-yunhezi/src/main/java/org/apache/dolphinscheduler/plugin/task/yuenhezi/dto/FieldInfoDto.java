package org.apache.dolphinscheduler.plugin.task.yuenhezi.dto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FieldInfoDto {
    private String id;
    /**
     * 关联文件类元数据表
     */
    private String refFileId;

    /**
     * 字段名称
     */
    private String columnName;

    /**
     * 字段类型
     */
    private String columnType;

    private String sortIndex;

    /**
     * 由文件路径，文件名称，桶名称，字段名称组成的唯一标识符，同步数据时使用
     */
    private String md5Hash;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    /**
     * 更新人
     */
    private String updateBy;

    public FieldInfoDto(String fieldName, String columnType, int sortIndex,  String md5Hash) {
        this.columnName = fieldName;
        this.columnType = columnType;
        this.sortIndex = String.valueOf(sortIndex);
        this.md5Hash = md5Hash;
    }
}
