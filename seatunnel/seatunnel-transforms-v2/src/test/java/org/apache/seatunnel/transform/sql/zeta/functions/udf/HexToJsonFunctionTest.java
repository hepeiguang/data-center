package org.apache.seatunnel.transform.sql.zeta.functions.udf;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class HexToJsonFunctionTest {
    @Test
    public void test1() {
        String binaryStr = HexToJsonFunctionTest.stringToHex("1323446803093934082#1323446800178892801#元数据详情#metadata:datacolumn:detail");
        HexToJsonFunction function = new HexToJsonFunction();

        System.out.println(function.evaluate(Arrays.asList(binaryStr, "#", "id,parent_id, menu_name ,menu_path,menu_component,menu_redirect,menu_perms")));
    }



    public static String stringToHex(String input) {
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X", b));
        }
        return hexString.toString();
    }
}
