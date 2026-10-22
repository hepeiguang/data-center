// HttpExpressionConfig.java
package org.apache.dolphinscheduler.plugin.task.flexiblehttp;

import org.apache.commons.lang3.StringUtils;

public class FlexibleHttpExpressionConfig {
    private String extractorType;
    private String extractExpression;
    private String bodyExpression;
    private String headersExpression;
    private String paramsExpression;
    private String pathExpression;

    // 检查是否有提取配置
    public boolean hasExtractorConfig() {
        return StringUtils.isNotEmpty(extractorType) && 
               StringUtils.isNotEmpty(extractExpression);
    }
    
    // 检查是否有表达式配置
    public boolean hasExpressionConfig() {
        return StringUtils.isNotEmpty(bodyExpression) ||
               StringUtils.isNotEmpty(headersExpression) ||
               StringUtils.isNotEmpty(paramsExpression) ||
               StringUtils.isNotEmpty(pathExpression);
    }
    
    // Getters and Setters
    public String getExtractorType() { return extractorType; }
    public void setExtractorType(String extractorType) { this.extractorType = extractorType; }
    public String getExtractExpression() { return extractExpression; }
    public void setExtractExpression(String extractExpression) { this.extractExpression = extractExpression; }
    public String getBodyExpression() { return bodyExpression; }
    public void setBodyExpression(String bodyExpression) { this.bodyExpression = bodyExpression; }
    public String getHeadersExpression() { return headersExpression; }
    public void setHeadersExpression(String headersExpression) { this.headersExpression = headersExpression; }
    public String getParamsExpression() { return paramsExpression; }
    public void setParamsExpression(String paramsExpression) { this.paramsExpression = paramsExpression; }
    public String getPathExpression() {
        return pathExpression;
    }
    public void setPathExpression(String pathExpression) {
        this.pathExpression = pathExpression;
    }
}