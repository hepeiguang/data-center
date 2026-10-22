package org.apache.seatunnel.transform.sql.zeta.functions.udf;

import com.google.auto.service.AutoService;
import org.apache.seatunnel.api.table.type.BasicType;
import org.apache.seatunnel.api.table.type.SeaTunnelDataType;
import org.apache.seatunnel.common.utils.JsonUtils;
import org.apache.seatunnel.transform.sql.zeta.ZetaUDF;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@AutoService(ZetaUDF.class)
public class Base64ToJsonFunction implements ZetaUDF {

    @Override
    public String functionName() {
        return "BASE64_TO_JSON";
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
                    "参数不足，需要3个参数：binaryStr(二进制字符串), delimiter(分隔符), fieldNames(字段名列表), charset(Base64字符串编码)。当前参数个数：" + args.size()
            );
        }
        try {
            String binaryStr = (String) args.get(0);
            String delimiter = (String) args.get(1);
            String fieldNames = (String) args.get(2);
            Charset charset;
            if (args.size() > 2) {
                charset = Charset.forName((String) args.get(3));
            } else {
                charset = StandardCharsets.UTF_8;
            }
            // 1. 检查是否是合法 JSON（如果是，直接返回）
            if (JsonUtils.isJson(binaryStr)) {
                return binaryStr;
            }

            // 2. 按分隔符拆分二进制字符串（假设二进制字符串是16进制格式，如 "414243" -> "ABC"）
            String decodedStr = base64ToStr(binaryStr, charset);
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
        } catch (UnsupportedCharsetException e) {
            throw new RuntimeException("charset parameter exception: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Binary to JSON failed: " + e.getMessage(), e);
        }
    }

    // 16 进制字符串 → 普通字符串
    public static String base64ToStr(String base64Str, Charset charset) {
        // 1. 解码 Base64 字符串 → 字节数组
        byte[] decodedBytes = Base64.getDecoder().decode(base64Str);

        // 2. 字节数组 → 字符串（UTF-8 编码）
        return new String(decodedBytes, charset);
    }
}
