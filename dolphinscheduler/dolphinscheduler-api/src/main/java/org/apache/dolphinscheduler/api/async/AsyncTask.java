package org.apache.dolphinscheduler.api.async;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.dolphinscheduler.api.dto.TjiLogDto;
import org.apache.dolphinscheduler.api.security.impl.htzh.HtzhAuthConfig;
import org.apache.dolphinscheduler.api.service.AuditService;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.dao.entity.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 异步处理 分布式获取请求头有问题
 */
@Slf4j
@Component
public class AsyncTask {

    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private AuditService logService;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private HtzhAuthConfig htzhAuthConfig;

    /**
     * 每分钟的0秒执行一次
     */
    @Scheduled(cron = "0 * * * * ?")
    public void pushLogToOtherServer() {
        log.warn("Start to push log.");

        try {
            LocalDateTime now = LocalDateTime.now();

            // 当前分钟的0秒（开始时间）
            LocalDateTime startOfMinute = now.withSecond(0);

            // 下一分钟的0秒（结束时间）
            LocalDateTime endOfMinute = startOfMinute.minusMinutes(1);

            // log查询
            List<AuditLog> logs = logService.queryLogList(startOfMinute, endOfMinute);
            if (CollectionUtils.isEmpty(logs)) {
                return;
            }
            List<TjiLogDto> tjiLogVos = convertToTjiLogVos(logs);
            String url = htzhAuthConfig.getLoggerUrl();
            log.warn("push log to {}, startTime:{}, endTime:{}", url,startOfMinute,endOfMinute);
            log.warn("push log to body{}", JSONUtils.toJsonString(tjiLogVos));
            // 调用添加服务
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, tjiLogVos, String.class);
            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                log.error("push log failed, detail: {}", JSONUtils.toJsonString(responseEntity));
            }
            log.info("push log to res{}", JSONUtils.toJsonString(responseEntity));
        } catch (Exception e) {
            log.error("push log failed.", e);
        }
    }

    /**
     * 新增方法：批量转换
     * 如果其他地方需要批量转换，可以添加这个方法
     */
    private List<TjiLogDto> convertToTjiLogVos(List<AuditLog> logDtos) {
        return logDtos.stream()
                .map(this::convertToTjiLogVo)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 新增方法：将LogDto转换为TjiLogVo
     * 如果其他地方需要用到转换，可以添加这个方法
     */
    private TjiLogDto convertToTjiLogVo(AuditLog logDto) {
        TjiLogDto tjiLogVo = new TjiLogDto();
        Date date = logDto.getCreateTime();
        // 转换为字符串
        String dateString = formatter.format(date);
        tjiLogVo.setExecutionTime(dateString);
        tjiLogVo.setExecutor(logDto.getUserName());
        tjiLogVo.setLogType("0");
        tjiLogVo.setMoudleType(logDto.getModelType());
//        tjiLogVo.setOpeContent(logDto.);
        tjiLogVo.setOpeName(logDto.getDescription());
//        tjiLogVo.setReqUrl(logDto.getRequestUri());
        tjiLogVo.setRequestType(logDto.getHttpMethod());
//        tjiLogVo.setResultCode(logDto.getExCode());
        tjiLogVo.setResultMsg(logDto.getDetail());
//        tjiLogVo.setResultStatus(logDto.getResultStatus());
        return tjiLogVo;
    }
}