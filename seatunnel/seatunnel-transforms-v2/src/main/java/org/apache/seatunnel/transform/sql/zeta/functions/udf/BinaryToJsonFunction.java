package org.apache.seatunnel.transform.sql.zeta.functions.udf;

import com.google.auto.service.AutoService;
import org.apache.seatunnel.api.table.type.BasicType;
import org.apache.seatunnel.api.table.type.SeaTunnelDataType;
import org.apache.seatunnel.common.utils.JsonUtils;
import org.apache.seatunnel.transform.sql.zeta.ZetaUDF;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@AutoService(ZetaUDF.class)
public class BinaryToJsonFunction implements ZetaUDF {

    @Override
    public String functionName() {
        return "BINARY_TO_JSON";
    }

    @Override
    public SeaTunnelDataType<?> resultType(List<SeaTunnelDataType<?>> argsType) {
        return BasicType.STRING_TYPE;
    }

    @Override
    public Object evaluate(List<Object> args) {
        // 参数校验：必须至少有3个参数
        if (args.size() < 3) {
            throw new IllegalArgumentException(
                    "参数不足，需要3个参数：binaryStr(二进制字符串), delimiter(分隔符), fieldNames(字段名列表)。当前参数个数：" + args.size()
            );
        }
        try {
            String binaryStr = (String) args.get(0);
            String delimiter = (String) args.get(1);
            String fieldNames = (String) args.get(2);

            // 1. 检查是否是合法 JSON（如果是，直接返回）
            if (JsonUtils.isJson(binaryStr)) {
                return binaryStr;
            }

            // 2. 按分隔符拆分二进制字符串（假设二进制字符串是16进制格式，如 "414243" -> "ABC"）
            String decodedStr = binaryToStrWithBigInteger(binaryStr);
            if (JsonUtils.isJson(decodedStr)) {
                return decodedStr;
            }

            String[] parts = decodedStr.split(delimiter);
            String[] fields = fieldNames.split(",|，|;|；");
            fields = Arrays.stream(fields)
                    .map(s -> s.replaceAll("\\s", ""))
                    .toArray(String[]::new);

            // 3. 检查字段数量是否匹配
            List<String> extendList = null;
            if (parts.length > fields.length) {
                extendList = new ArrayList<>();
            }

            // 4. 封装成有序 JSON
            Map<String, Object> jsonMap = new LinkedHashMap<>();
            for (int i = 0; i < parts.length; i++) {
                if (extendList != null && i >= fields.length) {
                    extendList.add(parts[i].trim());
                } else {
                    jsonMap.put(fields[i], parts[i].trim());
                }
            }
            if (extendList != null) {
                jsonMap.put("@extend_list@", extendList);
            }
            return JsonUtils.toJsonString(jsonMap);
        } catch (Exception e) {
            throw new RuntimeException("Binary to JSON failed: " + e.getMessage(), e);
        }
    }

    // 16进制字符串 → 普通字符串（如 "414243" → "ABC"）
    public String binaryToStrWithBigInteger(String binaryStr) {
        String cleaned = binaryStr.replaceAll("\\s", "");
        if (!cleaned.matches("[01]+")) {
            throw new IllegalArgumentException("输入必须为二进制字符串（仅含 0/1）");
        }
        // 补零到 8 的倍数
        while (cleaned.length() % 8 != 0) {
            cleaned = "0" + cleaned;
        }
        byte[] bytes = new BigInteger(cleaned, 2).toByteArray();
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
