package org.apache.seatunnel.connectors.seatunnel.udp.codec;

import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.JsonNode;
import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Cursor-based binary parser supporting read / loop / switch / skip / assert.
 *
 * <p>See docs/UDP_CODEC.md for DSL. Type {@code bytes} is emitted as Base64 string for JSON
 * safety; use {@code hex} for hex text.
 */
public class OffsetTableParser implements PacketCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_LOOP_COUNT = 1_000_000;

    private final JsonNode root;
    private final ByteOrder byteOrder;
    private final Charset charset;

    public OffsetTableParser(String offsetTableJson) throws Exception {
        this.root = MAPPER.readTree(offsetTableJson);
        String order = root.path("byteOrder").asText("BIG_ENDIAN").toUpperCase(Locale.ROOT);
        this.byteOrder =
                "LITTLE_ENDIAN".equals(order) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        this.charset = Charset.forName(root.path("charset").asText("UTF-8"));
    }

    @Override
    public List<Map<String, Object>> decode(byte[] payload, PacketMeta meta) throws Exception {
        ByteBuffer buf = ByteBuffer.wrap(payload).order(byteOrder);
        Map<String, Object> vars = new LinkedHashMap<>();
        if (meta != null) {
            vars.put("_meta", meta.toMap());
        }
        JsonNode steps = root.get("steps");
        if (steps == null || !steps.isArray()) {
            throw new IllegalArgumentException("offset_table requires array field 'steps'");
        }
        runSteps(steps, buf, vars);
        vars.remove("_meta");
        return Collections.singletonList(vars);
    }

    private void runSteps(JsonNode steps, ByteBuffer buf, Map<String, Object> vars)
            throws Exception {
        for (JsonNode step : steps) {
            runStep(step, buf, vars);
        }
    }

    private void runStep(JsonNode step, ByteBuffer buf, Map<String, Object> vars) throws Exception {
        String op = step.path("op").asText("").toLowerCase(Locale.ROOT);
        switch (op) {
            case "read":
                doRead(step, buf, vars);
                break;
            case "loop":
                doLoop(step, buf, vars);
                break;
            case "switch":
                doSwitch(step, buf, vars);
                break;
            case "skip":
                doSkip(step, buf, vars);
                break;
            case "assert":
                doAssert(step, vars);
                break;
            default:
                throw new IllegalArgumentException("unsupported op: " + op);
        }
    }

    private void doRead(JsonNode step, ByteBuffer buf, Map<String, Object> vars) {
        String name = requiredText(step, "name");
        String type = requiredText(step, "type").toLowerCase(Locale.ROOT);
        if (step.has("offset")) {
            int absolute = step.get("offset").asInt();
            if (absolute < 0 || absolute > buf.limit()) {
                throw new IllegalArgumentException(
                        "invalid absolute offset=" + absolute + " limit=" + buf.limit());
            }
            buf.position(absolute);
        }
        vars.put(name, readValue(type, step, buf, vars));
    }

    private Object readValue(String type, JsonNode step, ByteBuffer buf, Map<String, Object> vars) {
        switch (type) {
            case "uint8":
                ensureRemaining(buf, 1);
                return buf.get() & 0xFF;
            case "int8":
                ensureRemaining(buf, 1);
                return (int) buf.get();
            case "uint16":
                ensureRemaining(buf, 2);
                return buf.getShort() & 0xFFFF;
            case "int16":
                ensureRemaining(buf, 2);
                return (int) buf.getShort();
            case "uint32":
                ensureRemaining(buf, 4);
                return buf.getInt() & 0xFFFFFFFFL;
            case "int32":
                ensureRemaining(buf, 4);
                return buf.getInt();
            case "int64":
                ensureRemaining(buf, 8);
                return buf.getLong();
            case "float32":
                ensureRemaining(buf, 4);
                return buf.getFloat();
            case "float64":
                ensureRemaining(buf, 8);
                return buf.getDouble();
            case "bytes": {
                int len = resolveLength(step, vars);
                ensureRemaining(buf, len);
                byte[] b = new byte[len];
                buf.get(b);
                return Base64.getEncoder().encodeToString(b);
            }
            case "hex": {
                int len = resolveLength(step, vars);
                ensureRemaining(buf, len);
                byte[] b = new byte[len];
                buf.get(b);
                return toHex(b);
            }
            case "string": {
                int len = resolveLength(step, vars);
                ensureRemaining(buf, len);
                byte[] b = new byte[len];
                buf.get(b);
                return new String(b, charset);
            }
            default:
                throw new IllegalArgumentException("unsupported type: " + type);
        }
    }

    private int resolveLength(JsonNode step, Map<String, Object> vars) {
        int len;
        if (step.has("length")) {
            len = step.get("length").asInt();
        } else if (step.has("lengthFrom")) {
            String ref = step.get("lengthFrom").asText();
            Object v = vars.get(ref);
            if (v == null) {
                throw new IllegalArgumentException("lengthFrom field not found: " + ref);
            }
            if (!(v instanceof Number)) {
                throw new IllegalArgumentException("lengthFrom field is not a number: " + ref);
            }
            len = ((Number) v).intValue();
        } else {
            throw new IllegalArgumentException("bytes/string/hex require length or lengthFrom");
        }
        if (len < 0) {
            throw new IllegalArgumentException("length must be >= 0, got " + len);
        }
        return len;
    }

    private void doLoop(JsonNode step, ByteBuffer buf, Map<String, Object> vars) throws Exception {
        int count;
        if (step.has("count")) {
            count = step.get("count").asInt();
        } else if (step.has("countFrom")) {
            String ref = step.get("countFrom").asText();
            Object v = vars.get(ref);
            if (v == null) {
                throw new IllegalArgumentException("countFrom field not found: " + ref);
            }
            if (!(v instanceof Number)) {
                throw new IllegalArgumentException("countFrom field is not a number: " + ref);
            }
            count = ((Number) v).intValue();
        } else {
            throw new IllegalArgumentException("loop requires count or countFrom");
        }
        if (count < 0) {
            throw new IllegalArgumentException("loop count must be >= 0, got " + count);
        }
        if (count > MAX_LOOP_COUNT) {
            throw new IllegalArgumentException(
                    "loop count exceeds max " + MAX_LOOP_COUNT + ": " + count);
        }
        String as = step.path("as").asText("items");
        JsonNode body = step.get("steps");
        if (body == null || !body.isArray()) {
            throw new IllegalArgumentException("loop requires steps array");
        }
        List<Map<String, Object>> items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            runSteps(body, buf, new ScopedMap(item, vars));
            items.add(item);
        }
        vars.put(as, items);
    }

    private void doSwitch(JsonNode step, ByteBuffer buf, Map<String, Object> vars)
            throws Exception {
        String on = requiredText(step, "on");
        Object key = vars.get(on);
        if (key == null) {
            throw new IllegalArgumentException("switch on field not found: " + on);
        }
        JsonNode cases = step.get("cases");
        if (cases == null || !cases.isArray()) {
            throw new IllegalArgumentException("switch requires cases array");
        }
        JsonNode matched = null;
        JsonNode defaultCase = null;
        for (JsonNode c : cases) {
            if (c.path("default").asBoolean(false)) {
                defaultCase = c;
                continue;
            }
            if (c.has("when") && valuesEqual(key, c.get("when"))) {
                matched = c;
                break;
            }
        }
        JsonNode chosen = matched != null ? matched : defaultCase;
        if (chosen == null) {
            throw new IllegalArgumentException(
                    "no switch case matched for " + on + "=" + key + " and no default");
        }
        JsonNode body = chosen.get("steps");
        if (body != null && body.isArray()) {
            runSteps(body, buf, vars);
        }
    }

    private void doSkip(JsonNode step, ByteBuffer buf, Map<String, Object> vars) {
        int len;
        if (step.has("length")) {
            len = step.get("length").asInt();
        } else if (step.has("lengthFrom")) {
            Object v = vars.get(step.get("lengthFrom").asText());
            if (v == null) {
                throw new IllegalArgumentException(
                        "lengthFrom field not found: " + step.get("lengthFrom").asText());
            }
            if (!(v instanceof Number)) {
                throw new IllegalArgumentException(
                        "lengthFrom field is not a number: " + step.get("lengthFrom").asText());
            }
            len = ((Number) v).intValue();
        } else {
            throw new IllegalArgumentException("skip requires length or lengthFrom");
        }
        if (len < 0) {
            throw new IllegalArgumentException("skip length must be >= 0, got " + len);
        }
        ensureRemaining(buf, len);
        buf.position(buf.position() + len);
    }

    private void doAssert(JsonNode step, Map<String, Object> vars) {
        String field = requiredText(step, "field");
        Object actual = vars.get(field);
        if (!step.has("eq")) {
            throw new IllegalArgumentException("assert requires eq");
        }
        if (!valuesEqual(actual, step.get("eq"))) {
            throw new IllegalStateException(
                    "assert failed: field="
                            + field
                            + " expected="
                            + step.get("eq")
                            + " actual="
                            + actual);
        }
    }

    private static void ensureRemaining(ByteBuffer buf, int needed) {
        if (needed < 0) {
            throw new IllegalArgumentException("negative length: " + needed);
        }
        if (buf.remaining() < needed) {
            throw new IllegalArgumentException(
                    "packet truncated: need " + needed + " bytes, remaining " + buf.remaining());
        }
    }

    private static boolean valuesEqual(Object actual, JsonNode expected) {
        if (actual == null) {
            return expected.isNull();
        }
        if (expected.isIntegralNumber()) {
            long e = expected.asLong();
            if (actual instanceof Number) {
                return ((Number) actual).longValue() == e;
            }
            if (actual instanceof String) {
                String s = ((String) actual).trim();
                if (s.startsWith("0x") || s.startsWith("0X")) {
                    return Long.parseLong(s.substring(2), 16) == e;
                }
                return Long.parseLong(s) == e;
            }
            return false;
        }
        if (expected.isFloatingPointNumber() && actual instanceof Number) {
            return Double.compare(((Number) actual).doubleValue(), expected.asDouble()) == 0;
        }
        if (expected.isTextual()) {
            return expected.asText().equals(String.valueOf(actual));
        }
        if (expected.isBoolean() && actual instanceof Boolean) {
            return expected.asBoolean() == (Boolean) actual;
        }
        return expected.asText().equals(String.valueOf(actual));
    }

    private static String requiredText(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).asText().isEmpty()) {
            throw new IllegalArgumentException("missing required field: " + field);
        }
        return node.get(field).asText();
    }

    private static String toHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte value : b) {
            sb.append(String.format("%02X", value));
        }
        return sb.toString();
    }

    /** Writes to local item map; reads fall back to parent for countFrom/lengthFrom. */
    private static final class ScopedMap extends LinkedHashMap<String, Object> {
        private final Map<String, Object> local;
        private final Map<String, Object> parent;

        ScopedMap(Map<String, Object> local, Map<String, Object> parent) {
            this.local = local;
            this.parent = parent;
        }

        @Override
        public Object put(String key, Object value) {
            return local.put(key, value);
        }

        @Override
        public Object get(Object key) {
            if (local.containsKey(key)) {
                return local.get(key);
            }
            return parent.get(key);
        }

        @Override
        public boolean containsKey(Object key) {
            return local.containsKey(key) || parent.containsKey(key);
        }
    }
}
