package org.apache.seatunnel.connectors.seatunnel.udp.codec;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class JsPacketCodecTest {

    @TempDir Path tempDir;

    @Test
    public void testInlineJsParse() throws Exception {
        String script =
                "function parse(payload, meta) {\n"
                        + "  var a = payload[0] & 0xff;\n"
                        + "  var b = payload[1] & 0xff;\n"
                        + "  return { a: a, b: b, host: meta.remoteHost };\n"
                        + "}\n";
        JsPacketCodec codec = new JsPacketCodec(null, script);
        List<Map<String, Object>> rows =
                codec.decode(
                        new byte[] {0x0A, 0x14},
                        new PacketMeta("127.0.0.1", 1234, 2, 1L));
        Assertions.assertEquals(1, rows.size());
        Assertions.assertEquals(10, ((Number) rows.get(0).get("a")).intValue());
        Assertions.assertEquals(20, ((Number) rows.get(0).get("b")).intValue());
        Assertions.assertEquals("127.0.0.1", rows.get(0).get("host"));
    }

    @Test
    public void testHotReloadByMtime() throws Exception {
        Path scriptFile = tempDir.resolve("udp_parse.js");
        Files.write(
                scriptFile,
                ("function parse(payload, meta) { return { v: payload[0] & 0xff }; }\n")
                        .getBytes());
        JsPacketCodec codec = new JsPacketCodec(scriptFile.toString(), null);

        Map<String, Object> r1 =
                codec.decode(new byte[] {7}, new PacketMeta("h", 1, 1, 0)).get(0);
        Assertions.assertEquals(7, ((Number) r1.get("v")).intValue());

        // ensure mtime changes on Windows
        Thread.sleep(1100);
        Files.write(
                scriptFile,
                ("function parse(payload, meta) { return { v: (payload[0] & 0xff) * 10 }; }\n")
                        .getBytes());

        Map<String, Object> r2 =
                codec.decode(new byte[] {7}, new PacketMeta("h", 1, 1, 0)).get(0);
        Assertions.assertEquals(70, ((Number) r2.get("v")).intValue());
    }
}
