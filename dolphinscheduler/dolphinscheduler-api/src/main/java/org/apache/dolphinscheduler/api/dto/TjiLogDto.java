package org.apache.dolphinscheduler.api.dto;

import lombok.Data;

@Data
public class TjiLogDto implements java.io.Serializable{
    private static final long serialVersionUID = 1L;
    /**
     * 执行时间
     */
    private String executionTime;
    /**
     * 执行人
     */
    private String executor;
    /**
     * 日志类型(“0”系统日志)
     */
    private String logType;
    /**
     * 模块类型
     */
    private String moudleType;
    /**
     * 请求参数
     */
    private String opeContent;
    /**
     * 操作名称
     */
    private String opeName;
    /**
     * 请求地址
     */
    private String reqUrl;
    /**
     * 请求方式
     */
    private String requestType;
    /**
     * 结果码
     */
    private String resultCode;
    /**
     * 结果信息
     */
    private String resultMsg;
    /**
     * 返回结果状态
     */
    private String resultStatus;
}
