package org.apache.dolphinscheduler.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/**
 * (FunctionInfoCfg)实体类
 *
 * @author makejava
 * @since 2025-04-27 18:57:32
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("function_info_cfg")
public class FunctionInfoCfg {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
/**
     * 函数大类 1:字符串处理
    * 2：数学公式
 * 3：日期函数
 * * 4:自定义函数
     */
    private String functionType;

    /**
     * 函数大类1:字符串处理2：数学公式3：日期函数
     */
    private String typeName;
/**
     * 函数名称
     */
    private String functionName;
/**
     * 函数标识
     */
    private String functionCode;
/**
     * 排序
     */
    private Integer sort;
/**
     * 函数使用说明及示例
     */
    private String functionRemark;
/**
     * 属性信息
     */
    private String functionAttribute;
/**
     * 状态
     */
    private Integer status;
/**
     * 创建时间
     */
    private Date createTime;

    private Integer udfsId;

    private String label;

    private String  value;

    @TableField(exist = false)
    private boolean  hasParam;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFunctionType() {
        return functionType;
    }

    public void setFunctionType(String functionType) {
        this.functionType = functionType;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getFunctionCode() {
        return functionCode;
    }

    public void setFunctionCode(String functionCode) {
        this.functionCode = functionCode;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public String getFunctionRemark() {
        return functionRemark;
    }

    public void setFunctionRemark(String functionRemark) {
        this.functionRemark = functionRemark;
    }

    public String getFunctionAttribute() {
        return functionAttribute;
    }

    public void setFunctionAttribute(String functionAttribute) {
        this.functionAttribute = functionAttribute;
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

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getUdfsId() {
        return udfsId;
    }
    public void setUdfsId(Integer udfsId) {
        this.udfsId = udfsId;
    }


}

