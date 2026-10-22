package org.apache.dolphinscheduler.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * (DictInfoCfg)实体类
 *
 * @author makejava
 * @since 2025-04-27 18:57:32
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("dict_info_cfg")
public class DictInfoCfg {

    private String id;
/**
     * 字典类型1:命名标准2：主题标准3：数据项标准4:维度标准
     */
    private String dictType;

    /**
     * 函数大类1:字符串处理2：数学公式3：日期函数
     */
    private String dictName;

/**
     * 函数标识
     */
    private String dictCode;
    /**

     * 函数使用说明及示例
     */
    private String dictRemark;

/**
     * 状态
     */
    private Integer status;
/**
     * 创建时间
     */
    private Date createTime;
    private String label;

    private String  value;
    private String  apiUrl;



    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDictType() {
        return dictType;
    }

    public void setDictType(String dictType) {
        this.dictType = dictType;
    }

    public String getDictName() {
        return dictName;
    }

    public void setDictName(String dictName) {
        this.dictName = dictName;
    }

    public String getDictCode() {
        return dictCode;
    }

    public void setDictCode(String dictCode) {
        this.dictCode = dictCode;
    }

    public String getDictRemark() {
        return dictRemark;
    }

    public void setDictRemark(String dictRemark) {
        this.dictRemark = dictRemark;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }
}

