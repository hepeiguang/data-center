package org.apache.dolphinscheduler.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Data
@TableName("t_ds_resources")
public class Resources {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String alias;
    private String fileName;
    private String description;
    private Integer userId;
    private Integer type; //0 file 1 udf
    private Integer size;
    private Date createTime;
    private Date updateTime;
    private Integer pid;
    private String fullName;
    private Integer isDirectory;
    private Integer customType; // 0 hive 1 seatunnel 2 目录
    private Integer level;
    private String pfullName;
    private List<Resources> children;
}
