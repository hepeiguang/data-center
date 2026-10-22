package org.apache.seatunnel.transform.sql.zeta.functions.udf;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class BinaryToJsonFunctionTest {
    @Test
    public void test1() {
        String binaryStr = BinaryToJsonFunctionTest.stringToBinary("{\"name\": \"wsl\"}");
        BinaryToJsonFunction function = new BinaryToJsonFunction();

        System.out.println(function.evaluate(Arrays.asList(binaryStr, ",", "id,name")));
    }

    @Test
    public void test2() {

        String binaryStr = BinaryToJsonFunctionTest.stringToBinary("123,wsl,19,ddd");
        BinaryToJsonFunction function = new BinaryToJsonFunction();

        System.out.println(function.evaluate(Arrays.asList(binaryStr, ",", "id,name")));
    }

    // 方法 2：适用于超长二进制字符串（自动补零）
    public static String binaryToStrWithBigInteger(String binaryStr) {
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

    public static String stringToBinary(String input) {
        StringBuilder binaryString = new StringBuilder();

        // 获取字符串的字节数组（UTF-8编码）
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);

        for (byte b : bytes) {
            // 将字节转换为8位二进制字符串，高位补零
            String binary = String.format("%8s", Integer.toBinaryString(b & 0xFF))
                    .replace(' ', '0');
            binaryString.append(binary).append(" "); // 添加空格分隔每个字节
        }

        return binaryString.toString().trim(); // 去除末尾多余的空格
    }
}
