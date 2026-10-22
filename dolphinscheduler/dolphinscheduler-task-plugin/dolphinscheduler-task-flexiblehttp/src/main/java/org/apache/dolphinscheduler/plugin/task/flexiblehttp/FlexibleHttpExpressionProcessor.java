// HttpExpressionProcessor.java
package org.apache.dolphinscheduler.plugin.task.flexiblehttp;

import com.fasterxml.jackson.databind.node.ObjectNode;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.model.Property;
import org.apache.dolphinscheduler.plugin.task.api.utils.ParameterUtils;
import org.apache.dolphinscheduler.plugin.task.http.HttpParameters;
import org.apache.dolphinscheduler.plugin.task.http.HttpParametersType;
import org.apache.dolphinscheduler.plugin.task.http.HttpProperty;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Slf4j
public class FlexibleHttpExpressionProcessor {
    
    private final FlexibleHttpExpressionConfig config;
    private final Map<String, Property> contextParams;
    
    public FlexibleHttpExpressionProcessor(FlexibleHttpExpressionConfig config, Map<String, Property> contextParams) {
        this.config = config;
        this.contextParams = contextParams;
    }
    
    public void process(HttpParameters parameters) {
        if (config == null || !config.hasExpressionConfig()) {
            return;
        }
        
        // 创建表达式上下文（包含所有系统参数）
        Map<String, Object> expressionContext = createExpressionContext();
        
        // 处理body表达式
        if (StringUtils.isNotEmpty(config.getBodyExpression())) {
            String dynamicBody = evaluateExpression(config.getBodyExpression(), expressionContext);
            parameters.setHttpBody(dynamicBody);
        }
        
        // 处理headers表达式
        if (StringUtils.isNotEmpty(config.getHeadersExpression())) {
            Map<String, String> headers = evaluateMapExpression(config.getHeadersExpression(), expressionContext);
            updateHttpHeaders(parameters, headers);
        }
        
        // 处理params表达式
        if (StringUtils.isNotEmpty(config.getParamsExpression())) {
            Map<String, String> params = evaluateMapExpression(config.getParamsExpression(), expressionContext);
            updateHttpParams(parameters, params);
        }

        // 处理path表达式
        if (StringUtils.isNotEmpty(config.getPathExpression())) {
            Map<String, String> params = evaluateMapExpression(config.getPathExpression(), expressionContext);
            updateUrl(parameters, params);
        }
    }




    private Map<String, Object> createExpressionContext() {
        Map<String, Object> context = new HashMap<>();
        
        // 添加系统参数
        for (Map.Entry<String, Property> entry : contextParams.entrySet()) {
            context.put(entry.getKey(), entry.getValue().getValue());
        }
        
        // 添加特殊变量（如localParams等）
//        context.put("localParams", MapUtils.convertToMap(contextParams));
        
        return context;
    }


    private Map<String, String> evaluateMapExpression(String expression, Map<String, Object> context) {
        String evaluated = evaluateExpression(expression, context);
        try {
            return JSONUtils.parseObject(evaluated, Map.class);
        }catch (Exception e){
            log.error("Failed to parse map expression: {}", expression, e);
            return Collections.EMPTY_MAP;
        }
    }
    
    private String evaluateExpression(String expression, Map<String, Object> context) {
        // 使用DolphinScheduler现有的参数解析器
        context.put("json", new JsonUtils());
        context.put("log", new GroovyLogger());
        return parseNestedExpressions(expression, context);
    }

    private String parseNestedExpressions(String expression, Map<String, Object> context) {
        StringBuilder result = new StringBuilder();
        int currentIndex = 0;
        int braceLevel = 0;
        int exprStart = -1;
        char[] chars = expression.toCharArray();

        while (currentIndex < chars.length) {
            if(currentIndex < chars.length -1 &&
               chars[currentIndex] == '#' &&
               chars[currentIndex + 1] == '{') {
                if(braceLevel == 0) {
                    result.append(expression.substring(Math.max(exprStart + 1, 0), currentIndex));
                    exprStart = currentIndex;
                    braceLevel =1 ;
                    currentIndex += 2;
                    continue;
                }
            }
            if(braceLevel > 0) {
                if(chars[currentIndex] == '{'){
                    braceLevel++;
                }else if(chars[currentIndex] == '}'){
                    braceLevel--;

                    if(braceLevel==0){
                        int contentStart = exprStart +2;
                        String innerExpr = expression.substring(contentStart, currentIndex);
                        Object value = evaluateGroovyExpression(innerExpr, context);
                        result.append(value != null ? value.toString() : "");
                        //重置
                        exprStart = -1;
                    }
                }
            }

//            if(braceLevel == 0 && exprStart == -1){
//                result.append(chars[currentIndex]);
//            }
            currentIndex++;
        }
        return result.toString();
    }

    private Object evaluateGroovyExpression(String expression, Map<String, Object> context) {
        try {
            Binding binding = new Binding(context);
            GroovyShell shell = createSecureShell(binding);
            return shell.evaluate(expression);
        }catch (Exception e) {
            log.error("Evaluate expression error: {}", expression, e);
            return null;
        }

    }

    private static GroovyShell createSecureShell(Binding binding) {
        SecureASTCustomizer secure = new SecureASTCustomizer();
        secure.setClosuresAllowed(true);
        secure.setMethodDefinitionAllowed(false);
        secure.setImportsWhitelist(Arrays.asList(
                "java.util.Date",
                "java.util.List",
                "java.util.Map"
        ));
        secure.setStaticImportsWhitelist(Collections.emptyList());
        try {
            secure.setReceiversClassesBlackList(Arrays.asList(
                    Class.forName("java.lang.System"),
                    Class.forName("java.lang.Runtime")
            ));
        } catch (Exception e) {
            log.error("set ReceiversClassesBlackList error: ", e);
        }
        CompilerConfiguration config = new CompilerConfiguration();
        config.addCompilationCustomizers(secure);
        return new GroovyShell(binding, config);
    }


    private void updateHttpHeaders(HttpParameters parameters, Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) return;
        
        List<HttpProperty> existing = parameters.getHttpParams();
        if (existing == null) {
            existing = new ArrayList<>();
            parameters.setHttpParams(existing);
        }
        
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            existing.add(new HttpProperty(
                entry.getKey(), 
                HttpParametersType.HEADERS,
                entry.getValue()
            ));
        }
    }
    
    private void updateHttpParams(HttpParameters parameters, Map<String, String> params) {
        if (params == null || params.isEmpty()) return;
        
        List<HttpProperty> existing = parameters.getHttpParams();
        if (existing == null) {
            existing = new ArrayList<>();
            parameters.setHttpParams(existing);
        }
        
        for (Map.Entry<String, String> entry : params.entrySet()) {
            existing.add(new HttpProperty(
                entry.getKey(), 
                HttpParametersType.PARAMETER, 
                entry.getValue()
            ));
        }
    }


    /**
     *
     * @param parameters
     * @param params
     */
    private void updateUrl(HttpParameters parameters, Map<String, String> params) {
        String url = parameters.getUrl();
        //通过convertParameterPlaceholders 将${key} 替换
        ParameterUtils.convertParameterPlaceholders(url, params);
    }

    public static class JsonUtils {
        public Object parse(String json) {
            return JSONUtils.parseObject(json, Object.class);
        }

        public String encode(Object obj) {
            return JSONUtils.toJsonString(obj);
        }
    }

    public static class GroovyLogger {

        private static final Logger log = LoggerFactory.getLogger(GroovyLogger.class);

        public void info(String message) {
            log.info("[Groovy] {}", message);
        }

        public void warn(String message) {
            log.warn("[Groovy] {}", message);
        }
    }

//    public static void main(String[] args) {
//        String expression = "    def jsonResponse = this.binding.getVariable('sjdd.response')\n" +
//                "    def data = json.parse(jsonResponse)\n" +
//                "    def newList = []\n" +
//                "    data.each {\n" +
//                "       outerObj -> outerObj.each { key, innerArray ->\n" +
//                "          if(innerArray instanceof List) {\n" +
//                "            newList.add(innerArray[0])\n" +
//                "         }\n" +
//                "       }\n" +
//                "    }\n" +
//                "    json.encode(newList)";
//
//        Pattern pattern = Pattern.compile("#\\{((?:[^{}]|\\{[^{}]*\\})*)\\}", Pattern.DOTALL);
//        Matcher matcher = pattern.matcher(expression);
//        Map<String, Object> context = new HashMap<>();
//        List<String> stringList = new ArrayList<>();
//        stringList.add("");
//        String s = "";
//        context.put("sjdd.response", s);
//        context.put("json", new JsonUtils());
//        context.put("log", new GroovyLogger());
//        Binding binding = new Binding(context);
//        GroovyShell shell = createSecureShell(binding);
//        Object evaluate = shell.evaluate(expression);
////        ObjectNode jsonNodes = JSONUtils.parseObject(JSONUtils.toJsonString(evaluate));
//        System.out.println(evaluate);
//    }


}