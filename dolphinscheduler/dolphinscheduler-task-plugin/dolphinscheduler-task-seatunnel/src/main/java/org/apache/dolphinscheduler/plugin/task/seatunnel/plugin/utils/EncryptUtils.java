package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.common.utils.HttpUtils;

@Slf4j
public class EncryptUtils {

    private static final String encryptHost = System.getenv("DAM_BASE_URL");

    private static final String encryptKey = "/api/system/inner/data/security/secretkey/data?operationType=%s&algorithms=%s";

    private static final String decryptData = "/api/system/inner/data/security/single/table/store/encrypt/data?datasourceId=%s&tableName=%s";

//    static {
//        Properties prop = new Properties();
//        String propFileName = "conf/application.yaml";
//        // 使用 try-with-resources 自动关闭流
//        try (InputStream inputStream = EncryptUtils.class.getClassLoader().getResourceAsStream(propFileName)) {
//            if (inputStream == null) {
//                throw new RuntimeException("找不到配置文件: " + propFileName);
//            }
//            prop.load(inputStream);
//
//            encryptHost = prop.getProperty("dam.base-url");
//            encryptKey = prop.getProperty("dam.urls.encrypt-key");
//            decryptData = prop.getProperty("dam.urls.decrypt-data");
//            if (encryptHost == null || encryptHost.isEmpty()) {
//                log.info("=========================encrypt.host 配置未找到或为空");
//            } else {
//                log.info("=========================encrypt.host 配置初始化成功: {}", encryptHost);
//            }
//        } catch (Exception e) {
//            throw new RuntimeException("加载配置文件失败: " + propFileName, e);
//        }
//    }

    /**
     * 获取密钥
     *
     * @param operationType 操作类型    encrypt decrypt
     * @param algorithms    算法列表 List<String>
     * @return
     */
    public static String getSecretKey(String operationType, String algorithms) {
        String url = encryptHost + String.format(encryptKey, operationType, algorithms);
        return HttpUtils.get(url);
    }

    /**
     * 查询sink表需要加密和脱敏的字段
     *
     * @param sinkDatasourceId 目标数据源id
     * @param sinkTableName    目标数据源表名
     * @return
     */
    public static String getEncryptFieldBySinkTable(int sinkDatasourceId, String sinkTableName) {
        // 查询sink表需要加密和脱敏的字段
        String encryptUrl = encryptHost + String.format(decryptData, sinkDatasourceId, sinkTableName);
        return HttpUtils.get(encryptUrl);
    }


    public static void main(String[] args) {
        System.out.println(EncryptUtils.getEncryptFieldBySinkTable(1, "test"));
    }
}
