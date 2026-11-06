package org.apache.seatunnel.connectors.seatunnel.udp.codec;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;

public class OffsetTableParserTest {

    @Test
    public void testControlWordLoopAndSwitch() throws Exception {
        // magic(uint16)=0xABCD, ctrl=1, count=2, then 2 x (id uint16 + val int32)
        ByteBuffer buf = ByteBuffer.allocate(2 + 1 + 1 + 2 * (2 + 4)).order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) 0xABCD);
        buf.put((byte) 1); // ctrl
        buf.put((byte) 2); // count
        buf.putShort((short) 10);
        buf.putInt(100);
        buf.putShort((short) 20);
        buf.putInt(200);

        String dsl =
                "{\n"
                        + "  \"byteOrder\": \"BIG_ENDIAN\",\n"
                        + "  \"steps\": [\n"
                        + "    { \"op\": \"read\", \"name\": \"magic\", \"type\": \"uint16\" },\n"
                        + "    { \"op\": \"read\", \"name\": \"ctrl\", \"type\": \"uint8\" },\n"
                        + "    {\n"
                        + "      \"op\": \"switch\",\n"
                        + "      \"on\": \"ctrl\",\n"
                        + "      \"cases\": [\n"
                        + "        {\n"
                        + "          \"when\": 1,\n"
                        + "          \"steps\": [\n"
                        + "            { \"op\": \"read\", \"name\": \"count\", \"type\": \"uint8\" },\n"
                        + "            {\n"
                        + "              \"op\": \"loop\",\n"
                        + "              \"countFrom\": \"count\",\n"
                        + "              \"as\": \"items\",\n"
                        + "              \"steps\": [\n"
                        + "                { \"op\": \"read\", \"name\": \"id\", \"type\": \"uint16\" },\n"
                        + "                { \"op\": \"read\", \"name\": \"val\", \"type\": \"int32\" }\n"
                        + "              ]\n"
                        + "            }\n"
                        + "          ]\n"
                        + "        },\n"
                        + "        { \"default\": true, \"steps\": [ { \"op\": \"skip\", \"length\": 4 } ] }\n"
                        + "      ]\n"
                        + "    },\n"
                        + "    { \"op\": \"assert\", \"field\": \"magic\", \"eq\": 43981 }\n"
                        + "  ]\n"
                        + "}";

        OffsetTableParser parser = new OffsetTableParser(dsl);
        List<Map<String, Object>> rows =
                parser.decode(
                        buf.array(),
                        new PacketMeta("127.0.0.1", 9999, buf.capacity(), System.currentTimeMillis()));

        Assertions.assertEquals(1, rows.size());
        Map<String, Object> row = rows.get(0);
        Assertions.assertEquals(43981, ((Number) row.get("magic")).intValue());
        Assertions.assertEquals(1, ((Number) row.get("ctrl")).intValue());
        Assertions.assertEquals(2, ((Number) row.get("count")).intValue());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) row.get("items");
        Assertions.assertEquals(2, items.size());
        Assertions.assertEquals(10, ((Number) items.get(0).get("id")).intValue());
        Assertions.assertEquals(100, ((Number) items.get(0).get("val")).intValue());
        Assertions.assertEquals(20, ((Number) items.get(1).get("id")).intValue());
        Assertions.assertEquals(200, ((Number) items.get(1).get("val")).intValue());
    }

    @Test
    public void testSwitchDefaultSkip() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(2 + 1 + 4).order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) 1);
        buf.put((byte) 9); // unknown ctrl -> default skip 4
        buf.putInt(0x11223344);

        String dsl =
                "{"
                        + "\"steps\":["
                        + "{\"op\":\"read\",\"name\":\"magic\",\"type\":\"uint16\"},"
                        + "{\"op\":\"read\",\"name\":\"ctrl\",\"type\":\"uint8\"},"
                        + "{\"op\":\"switch\",\"on\":\"ctrl\",\"cases\":["
                        + "{\"when\":1,\"steps\":[{\"op\":\"read\",\"name\":\"x\",\"type\":\"int32\"}]},"
                        + "{\"default\":true,\"steps\":[{\"op\":\"skip\",\"length\":4}]}"
                        + "]}"
                        + "]}";

        Map<String, Object> row =
                new OffsetTableParser(dsl)
                        .decode(buf.array(), new PacketMeta("h", 1, buf.capacity(), 0))
                        .get(0);
        Assertions.assertEquals(9, ((Number) row.get("ctrl")).intValue());
        Assertions.assertFalse(row.containsKey("x"));
    }

    @Test
    public void testTruncatedPacketRejected() {
        String dsl =
                "{\"steps\":[{\"op\":\"read\",\"name\":\"x\",\"type\":\"int32\"}]}";
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () ->
                        new OffsetTableParser(dsl)
                                .decode(new byte[] {1, 2}, new PacketMeta("h", 1, 2, 0)));
    }

    @Test
    public void testBytesEmittedAsBase64() throws Exception {
        String dsl =
                "{\"steps\":[{\"op\":\"read\",\"name\":\"raw\",\"type\":\"bytes\",\"length\":3}]}";
        Map<String, Object> row =
                new OffsetTableParser(dsl)
                        .decode(new byte[] {0x01, 0x02, 0x03}, new PacketMeta("h", 1, 3, 0))
                        .get(0);
        Assertions.assertEquals("AQID", row.get("raw"));
    }

    @Test
    public void testNegativeLoopCountRejected() {
        ByteBuffer buf = ByteBuffer.allocate(1).order(ByteOrder.BIG_ENDIAN);
        buf.put((byte) -1);
        String dsl =
                "{\"steps\":["
                        + "{\"op\":\"read\",\"name\":\"n\",\"type\":\"int8\"},"
                        + "{\"op\":\"loop\",\"countFrom\":\"n\",\"as\":\"items\",\"steps\":["
                        + "{\"op\":\"read\",\"name\":\"x\",\"type\":\"uint8\"}]}]}";
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () ->
                        new OffsetTableParser(dsl)
                                .decode(buf.array(), new PacketMeta("h", 1, 1, 0)));
    }
}
