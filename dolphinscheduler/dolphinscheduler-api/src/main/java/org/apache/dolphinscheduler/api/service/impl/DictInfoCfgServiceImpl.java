
package org.apache.dolphinscheduler.api.service.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.service.AccessTokenService;
import org.apache.dolphinscheduler.api.service.DictInfoCfgService;
import org.apache.dolphinscheduler.common.constants.Constants;
import org.apache.dolphinscheduler.dao.entity.*;
import org.apache.dolphinscheduler.dao.mapper.DictInfoCfgMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.Resource;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * (DictInfoCfgService)表服务实现类
 *
 * @author makejava
 * @since 2025-04-27 18:57:45
 */
@Service
@Slf4j
@Configuration
public class DictInfoCfgServiceImpl extends BaseServiceImpl implements DictInfoCfgService {
    @Resource
    private DictInfoCfgMapper dictInfoCfgMapper;

    @Value("${dict.topic-url:}")
    private String topicPath;
    @Value("${dict.data-url:}")
    private String dataItemPath;
    private static final ObjectMapper mapper = new ObjectMapper()
            // 忽略null值字段
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
            // 忽略空数组/集合
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY);

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public DictInfoCfg queryById(Integer id) {
        return this.dictInfoCfgMapper.selectById(id);
    }

    /**
     * @param dictInfoCfg
     * @param token
     * @return
     */
    @Override
    public Map<String, Object> queryByInfo(DictInfoCfg dictInfoCfg, String token) {
        Map<String, Object> result = new HashMap<>();
        dictInfoCfg.setStatus(1);
        List<DictInfoCfg> lts = this.dictInfoCfgMapper.queryAllByLimit(dictInfoCfg);
        if (lts.isEmpty()) {
            result.put(Constants.DATA_LIST, Collections.emptyList());
            putMsg(result, Status.SUCCESS);
            return result;
        }

//        List<DictInfoCfg> updatedList = lts.stream()
//                .map(item -> {
//
//                    DictInfoCfg newItem = new DictInfoCfg();
//                    newItem.setId(item.getId());
//                    newItem.setDictName(item.getDictName());
//                    newItem.setDictCode(item.getDictCode());
//                    newItem.setDictType(item.getDictType());
//                    // 添加新字段
//                    newItem.setLabel(item.getDictName());
//                    newItem.setValue(item.getDictType());
//                    newItem.setApiUrl(dataItemPath);
//                    return newItem;
//                })
//                .collect(Collectors.toList());
//        List<DictInfoCfg> updatedList = lts.stream()
//                .map(item -> {
//
//                    DictInfoCfg newItem = new DictInfoCfg();
//                    newItem.setId(item.getId());
//                    newItem.setDictName(item.getDictName());
//                    newItem.setDictCode(item.getDictCode());
//                    newItem.setDictType(item.getDictType());
//                    // 添加新字段
//                    newItem.setLabel(item.getDictName());
//                    newItem.setValue(item.getDictType());
//                    newItem.setApiUrl(dataItemPath);
//                    return newItem;
//                })
//                .collect(Collectors.toList());
//        List<DictGroup> results = new ArrayList<>();
//        for (DictInfoCfg item : updatedList) {
//            DictGroup newItem = new DictGroup();
//            newItem.setLabel(item.getLabel());
//            newItem.setValue(item.getValue());
//            newItem.setApiUrl(item.getApiUrl());
//            if (item.getDictType().equals("NAMESPACE")) {
//                newItem.setchildren(callApi(item, token));
//            }
//            results.add(newItem);
//        }
//        List<DictGroup> results = updatedList.stream()
//                .filter(item -> item.getDictType() .equals("NAMESPACE"))
//                .map(item -> {
//                    // 创建新对象或修改现有对象
//                    DictGroup newItem = new DictGroup();
//                    newItem.setLabel(item.getLabel());
//                    newItem.setValue(item.getValue());
//                    newItem.setApiUrl(item.getApiUrl());
//                    newItem.setchildren(callApi(item,token));
//                    return newItem;
//                })
//                .collect(Collectors.toList());
        List<DictInfoCfg> results = callApi(dictInfoCfg,token).stream()
                .map(item -> {
                    // 创建新对象或修改现有对象
                    DictInfoCfg newItem = new DictInfoCfg();
                    newItem.setId(item.getId());
                    newItem.setDictName(item.getDictName());
                    newItem.setDictCode(item.getDictCode());
                    newItem.setDictType(item.getDictType());
                    newItem.setLabel(item.getLabel());
                    newItem.setValue(item.getValue());
                    newItem.setApiUrl(dataItemPath);
                    return newItem;
                })
                .collect(Collectors.toList());
        if (results.isEmpty()) {
            result.put(Constants.DATA_LIST, Collections.emptyList());
            putMsg(result, Status.SUCCESS);
            return result;
        }
        result.put(Constants.DATA_LIST, results);
        putMsg(result, Status.SUCCESS);
        return result;
    }

    @Override
    public DictInfoCfg queryByCode(String code) {
        return this.dictInfoCfgMapper.queryByCode(code);
    }

    @Override
    public DictInfoCfg queryById(String id, String token) {
        DictInfoCfg dictInfoCfg = new DictInfoCfg();
        dictInfoCfg.setId(id);
        return callApi(dictInfoCfg, token).isEmpty() ? dictInfoCfg : callApi(dictInfoCfg, token).get(0);
    }

    @Override
    public List<DictInfoCfg> callApi(DictInfoCfg dictInfoCfg, String token) {
        String encryptObj = "";
        String url = "http://192.168.1.80:9528/api/data/standard/topic/tree/page?pageNum=1&pageSize=9999";
//        if(dictInfoCfg.getId()!=null){
//            url=url+"&id="+dictInfoCfg.getId();
//        }
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String tp = topicPath;
            HttpGet request = new HttpGet(topicPath);

//            request.setHeader("Authorization", "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsInRlbmFudElkIjoiLTEiLCJleHAiOjE3NTAyNDc3NDksImlhdCI6MTc1MDE2MTM0OX0.QMcCmwsyw09OEZHKf1fhrd36fnJx-2YehVMTObx-DL2CcxyK339M1X2amCMF_koFgdVtrsEwRAPPJ1I1mA7-Zw");
            request.setHeader("Accept", "application/json");
            HttpResponse response = client.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            System.out.println("Status Code:" + statusCode);
            encryptObj = EntityUtils.toString(response.getEntity());
            System.out.println("get dict responseBody :" + encryptObj);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        encryptObj="{\n" +
//                "    \"success\": true,\n" +
//                "    \"code\": 200,\n" +
//                "    \"msg\": \"操作成功\",\n" +
//                "    \"data\": {\n" +
//                "        \"records\": [\n" +
//                "            {\n" +
//                "                \"id\": \"1931973065272176641\",\n" +
//                "                \"status\": \"1\",\n" +
//                "                \"createTime\": \"2025-06-09 15:14:08\",\n" +
//                "                \"remark\": null,\n" +
//                "                \"topicNo\": \"001\",\n" +
//                "                \"topicName\": \"至於標準\",\n" +
//                "                \"parentId\": null,\n" +
//                "                \"createDept\": \"1197789917762031617\",\n" +
//                "                \"children\": []\n" +
//                "            }\n" +
//                "        ],\n" +
//                "        \"total\": \"1\",\n" +
//                "        \"size\": \"20\",\n" +
//                "        \"current\": \"1\",\n" +
//                "        \"orders\": [],\n" +
//                "        \"optimizeCountSql\": true,\n" +
//                "        \"searchCount\": true,\n" +
//                "        \"optimizeJoinOfCountSql\": true,\n" +
//                "        \"countId\": null,\n" +
//                "        \"maxLimit\": null,\n" +
//                "        \"pages\": \"1\"\n" +
//                "    },\n" +
//                "    \"timestamp\": \"1749534806960\"\n" +
//                "}";
        // 判断encryptObj是否为空 或者空字符串
        if (encryptObj.isEmpty()) {
            return Collections.emptyList();
        }

        ObjectNode rootNode = null;
        try {
            rootNode = (ObjectNode) mapper.readTree(encryptObj);
        } catch (Exception e) {
            log.error(" json config data error", e);
        }
        if (rootNode.get("data") == null) {
            return Collections.emptyList();
        }
        ArrayNode recordNode = (ArrayNode) rootNode.get("data").get("records");
        List<DictInfoCfg> updatedList = new ArrayList<>();
        recordNode.forEach(node -> {

            DictInfoCfg newItem = new DictInfoCfg();
            // 复制原有属性;
            newItem.setId(node.get("id").asText());
            newItem.setDictName(node.get("topicName").asText());
            newItem.setDictCode(node.get("topicNo").asText());
            // 添加新字段
            newItem.setLabel(node.get("topicName").asText());
            newItem.setValue(node.get("id").asText());
            if(("数值精度标准化").equals(node.get("topicName").asText())){
                newItem.setDictType("NUMACCURACY");
            } else if(("值域标准化").equals(node.get("topicName").asText())){
                newItem.setDictType("RANGE");
            } else if(("时间戳标准化").equals(node.get("topicName").asText())){
                newItem.setDictType("TIMESTAMP");
            }else{
                newItem.setDictType("NAMESPACE");
            }

            updatedList.add(newItem);

        });
        return updatedList;

    }


}
