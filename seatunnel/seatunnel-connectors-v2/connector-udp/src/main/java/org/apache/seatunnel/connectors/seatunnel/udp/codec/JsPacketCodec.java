package org.apache.seatunnel.connectors.seatunnel.udp.codec;

import org.apache.seatunnel.common.utils.JsonUtils;

import lombok.extern.slf4j.Slf4j;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JS codec: loads script from path, invokes {@code parse(payload, meta)}, hot-reloads on mtime
 * change.
 */
@Slf4j
public class JsPacketCodec implements PacketCodec {

    private final Path scriptPath;
    private final String inlineScript;
    private final Object lock = new Object();

    private volatile long loadedMtime = -1L;
    private volatile Invocable invocable;

    public JsPacketCodec(String scriptPath, String inlineScript) {
        this.scriptPath =
                scriptPath == null || scriptPath.trim().isEmpty()
                        ? null
                        : Paths.get(scriptPath);
        this.inlineScript = inlineScript;
        if (this.scriptPath == null
                && (inlineScript == null || inlineScript.trim().isEmpty())) {
            throw new IllegalArgumentException("js codec requires scriptPath or inline script");
        }
    }

    @Override
    public List<Map<String, Object>> decode(byte[] payload, PacketMeta meta) throws Exception {
        ensureEngine();
        Object result;
        synchronized (lock) {
            result = invocable.invokeFunction("parse", payload, meta.toMap());
        }
        return normalizeResult(result);
    }

    private void ensureEngine() throws Exception {
        if (scriptPath == null) {
            if (invocable == null) {
                synchronized (lock) {
                    if (invocable == null) {
                        invocable = compile(inlineScript);
                    }
                }
            }
            return;
        }
        long mtime = Files.getLastModifiedTime(scriptPath).toMillis();
        if (invocable == null || mtime != loadedMtime) {
            synchronized (lock) {
                mtime = Files.getLastModifiedTime(scriptPath).toMillis();
                if (invocable == null || mtime != loadedMtime) {
                    String code = new String(Files.readAllBytes(scriptPath), StandardCharsets.UTF_8);
                    invocable = compile(code);
                    loadedMtime = mtime;
                    log.info("Loaded JS codec from {} mtime={}", scriptPath, mtime);
                }
            }
        }
    }

    private static Invocable compile(String code) throws Exception {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("nashorn");
        if (engine == null) {
            engine = manager.getEngineByName("javascript");
        }
        if (engine == null) {
            throw new IllegalStateException(
                    "No JS ScriptEngine available (need Nashorn on JDK 11)");
        }
        engine.eval(code);
        if (!(engine instanceof Invocable)) {
            throw new IllegalStateException("JS engine is not Invocable");
        }
        return (Invocable) engine;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> normalizeResult(Object result) throws Exception {
        if (result == null) {
            return Collections.emptyList();
        }
        // Prefer JSON round-trip to flatten ScriptObjectMirror / Bindings
        String json = JsonUtils.toJsonString(toJava(result));
        Object parsed = JsonUtils.parseObject(json, Object.class);
        if (parsed instanceof List) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Object o : (List<?>) parsed) {
                rows.add((Map<String, Object>) o);
            }
            return rows;
        }
        if (parsed instanceof Map) {
            return Collections.singletonList((Map<String, Object>) parsed);
        }
        Map<String, Object> wrap = new LinkedHashMap<>();
        wrap.put("value", parsed);
        return Collections.singletonList(wrap);
    }

    private static Object toJava(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map) {
            Map<?, ?> src = (Map<?, ?>) value;
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : src.entrySet()) {
                out.put(String.valueOf(e.getKey()), toJava(e.getValue()));
            }
            return out;
        }
        if (value instanceof List) {
            List<?> src = (List<?>) value;
            List<Object> out = new ArrayList<>(src.size());
            for (Object o : src) {
                out.add(toJava(o));
            }
            return out;
        }
        // Nashorn ScriptObjectMirror implements Map; also handle JS arrays via Map with numeric keys
        if (value.getClass().getName().contains("ScriptObjectMirror")) {
            try {
                // treat as Map
                Map<?, ?> src = (Map<?, ?>) value;
                boolean arrayLike =
                        src.containsKey("length") && src.get("length") instanceof Number;
                if (arrayLike) {
                    int len = ((Number) src.get("length")).intValue();
                    List<Object> out = new ArrayList<>(len);
                    for (int i = 0; i < len; i++) {
                        out.add(toJava(src.get(String.valueOf(i))));
                    }
                    return out;
                }
                Map<String, Object> out = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : src.entrySet()) {
                    String k = String.valueOf(e.getKey());
                    if ("length".equals(k)) {
                        continue;
                    }
                    out.put(k, toJava(e.getValue()));
                }
                return out;
            } catch (ClassCastException ignore) {
                return String.valueOf(value);
            }
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof String) {
            return value;
        }
        if (value instanceof byte[]) {
            return value;
        }
        return String.valueOf(value);
    }
}
