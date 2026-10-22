package org.apache.dolphinscheduler.api.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
public class ResourcesVo {
    private Integer id;
    private String fullName;
    private String fileName;
    private String alias;
    private String pfullName;
    private Integer userId;
    private String userName;
    private String type; //0 file 1 udf
    private Integer size;
    private Date createTime;
    private Date updateTime;
    private Boolean directory;
    private String customType;
}
