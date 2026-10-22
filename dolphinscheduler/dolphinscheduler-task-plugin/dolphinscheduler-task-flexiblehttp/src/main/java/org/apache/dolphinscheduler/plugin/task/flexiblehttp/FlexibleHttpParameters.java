package org.apache.dolphinscheduler.plugin.task.flexiblehttp;



import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.plugin.task.api.model.Property;
import org.apache.dolphinscheduler.plugin.task.http.HttpParameters;

import java.util.List;

public class FlexibleHttpParameters extends HttpParameters {

    /**
     * task type
     */
    private FlexibleHttpTaskType flexibleHttpTaskType = FlexibleHttpTaskType.SIMPLE_REQUEST;

    /**
     * loop query condition
     */
    private String loopConditionField;   // 循环条件字段（JSON路径）
    private String loopConditionValue;   // 循环条件期望值

    /**
     * loop interval (ms) 循环间隔
     */
    private int loopInterval = 5000;

    /**
     * max loop times 循环次数
     */
    private int maxLoopNum = 3;

    /**
     * 输出参数
     */
    private List<FlexibleHttpOutputParameter> outputParameters;

    /**
     * 遍历的参数
     */
    private String iterateParam;

    /**
     * 遍历参数路径
     */
    private String iterateParamPath;

    /**
     * 别名
     */
    private String alias;

    /**
     * 其他参数 用来存放一些业务字段
     */
    private List<Property> otherParams;

    /**
     * 是否信任所有证书
     */
    private boolean trustAllCerts;

    /**
     * 指定SSL协议
     */
    private String sslProtocol;

    /**
     * 指定的加密套件
     */
    private String[] cipherSuites;

    /**
     * 表达式配置（可选扩展）
     */
    private FlexibleHttpExpressionConfig expressionConfig;

    /*********************************业管扩展********************************/

    /**
     * 是否记录日志用于业管记录接口调用日志
     *
     * 采用方式 采用可配置方式
     * 在logUrl中配置记录日志的url去请求 保存日志到所选目录
     */
    private boolean isLog;

    /**
     * 记录日志url
     */
    private String logUrl;

    public boolean getIsLog() {
        return isLog;
    }

    public void setLog(boolean isLog) {
        isLog = isLog;
    }

    public String getLogUrl() {
        return logUrl;
    }

    public void setLogUrl(String logUrl) {
        this.logUrl = logUrl;
    }

    public FlexibleHttpExpressionConfig getExpressionConfig() {
        return expressionConfig;
    }

    public void setExpressionConfig(FlexibleHttpExpressionConfig expressionConfig) {
        this.expressionConfig = expressionConfig;
    }

    public FlexibleHttpTaskType getFlexibleHttpTaskType() {
        return flexibleHttpTaskType;
    }

    public void setFlexibleHttpTaskType(FlexibleHttpTaskType flexibleHttpTaskType) {
        this.flexibleHttpTaskType = flexibleHttpTaskType;
    }

    public String getLoopConditionField() {
        return loopConditionField;
    }

    public void setLoopConditionField(String loopConditionField) {
        this.loopConditionField = loopConditionField;
    }

    public String getLoopConditionValue() {
        return loopConditionValue;
    }

    public void setLoopConditionValue(String loopConditionValue) {
        this.loopConditionValue = loopConditionValue;
    }

    public int getLoopInterval() {
        return loopInterval;
    }

    public void setLoopInterval(int loopInterval) {
        this.loopInterval = loopInterval;
    }


    public int getMaxLoopNum() {
        return maxLoopNum;
    }

    public void setMaxLoopNum(int maxLoopNum) {
        this.maxLoopNum = maxLoopNum;
    }

    public List<FlexibleHttpOutputParameter> getOutputParameters() {
        return outputParameters;
    }

    public void setOutputParameters(List<FlexibleHttpOutputParameter> outputParameters) {
        this.outputParameters = outputParameters;
    }

    public String getIterateParam() {
        return iterateParam;
    }

    public void setIterateParams(String iterateParams) {
        this.iterateParam = iterateParams;
    }

    public String getIterateParamPath() {
        return iterateParamPath;
    }

    public void setIterateParamPath(String iterateParamPath) {
        this.iterateParamPath = iterateParamPath;
    }

    @Override
    public boolean checkParameters() {
        boolean valid = super.checkParameters();
        if (flexibleHttpTaskType == FlexibleHttpTaskType.LOOP_REQUEST) {
            valid = valid && StringUtils.isNotEmpty(loopConditionField) && StringUtils.isNotEmpty(loopConditionValue);
        }
        if(flexibleHttpTaskType == FlexibleHttpTaskType.ITERATE_REQUEST) {
            valid = valid && StringUtils.isNotEmpty(iterateParam) && StringUtils.isNotEmpty(iterateParamPath);
        }
        return valid;
    }

    public void setIterateParam(String iterateParam) {
        this.iterateParam = iterateParam;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public List<Property> getOtherParams() {
        return otherParams;
    }

    public void setOtherParams(List<Property> otherParams) {
        this.otherParams = otherParams;
    }

    public boolean isTrustAllCerts() {
        return trustAllCerts;
    }

    public void setTrustAllCerts(boolean trustAllCerts) {
        this.trustAllCerts = trustAllCerts;
    }

    public String getSslProtocol() {
        return sslProtocol;
    }

    public void setSslProtocol(String sslProtocol) {
        this.sslProtocol = sslProtocol;
    }

    public String[] getCipherSuites() {
        return cipherSuites;
    }

    public void setCipherSuites(String[] cipherSuites) {
        this.cipherSuites = cipherSuites;
    }

    public boolean hasExpressionConfig() {
        return expressionConfig != null && expressionConfig.hasExpressionConfig();
    }
}