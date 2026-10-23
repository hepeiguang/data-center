package org.apache.seatunnel.connectors.seatunnel.udp.codec;

import org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions;
import org.apache.seatunnel.connectors.seatunnel.udp.config.UdpSourceConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ServiceLoader;

public final class PacketCodecFactory {

    private PacketCodecFactory() {}

    public static PacketCodec create(UdpSourceConfig config) throws Exception {
        UdpOptions.ParseMode mode = config.getParseMode();
        if (mode == null || mode == UdpOptions.ParseMode.none) {
            return null;
        }
        switch (mode) {
            case js:
                return new JsPacketCodec(resolveJsPath(config), config.getScriptContent());
            case offset_table:
                return createOffsetTableCodec(config);
            case string:
                return loadByProviderName("string", config);
            case bytes:
                return loadByProviderName("bytes", config);
            case custom:
                return createCustomCodec(config);
            default:
                return null;
        }
    }

    private static PacketCodec createOffsetTableCodec(UdpSourceConfig config) throws Exception {
        String table = config.getOffsetTable();
        String path = resolveOffsetTablePath(config);
        if ((table == null || table.trim().isEmpty()) && path != null && !path.trim().isEmpty()) {
            table = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        }
        if (table == null || table.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "offset_table mode requires offsetTable, offsetTablePath, or udfName");
        }
        return new OffsetTableParser(table);
    }

    private static String resolveJsPath(UdpSourceConfig config) {
        if (config.getScriptPath() != null && !config.getScriptPath().trim().isEmpty()) {
            return config.getScriptPath().trim();
        }
        return resolveUdfFile(config.getUdfName(), ".js");
    }

    private static String resolveOffsetTablePath(UdpSourceConfig config) {
        if (config.getOffsetTablePath() != null && !config.getOffsetTablePath().trim().isEmpty()) {
            return config.getOffsetTablePath().trim();
        }
        return resolveUdfFile(config.getUdfName(), ".json");
    }

    /** Resolve {@code $SEATUNNEL_HOME/udf/udp-codec/<name><ext>}. */
    private static String resolveUdfFile(String udfName, String ext) {
        if (udfName == null || udfName.trim().isEmpty()) {
            return null;
        }
        String base = udfName.trim().replace("\\", "/");
        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        if (base.toLowerCase().endsWith(".js") || base.toLowerCase().endsWith(".json")) {
            base = base.substring(0, base.lastIndexOf('.'));
        }
        if (base.isEmpty() || base.contains("..")) {
            throw new IllegalArgumentException("invalid udfName: " + udfName);
        }
        String home = System.getenv("SEATUNNEL_HOME");
        if (home == null || home.trim().isEmpty()) {
            home = System.getProperty("SEATUNNEL_HOME");
        }
        if (home == null || home.trim().isEmpty()) {
            home = System.getProperty("seatunnel.home");
        }
        if (home == null || home.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "udfName=" + udfName + " requires SEATUNNEL_HOME (udf/udp-codec/" + base + ext + ")");
        }
        Path file = Paths.get(home.trim(), "udf", "udp-codec", base + ext);
        return file.toAbsolutePath().toString();
    }

    private static PacketCodec createCustomCodec(UdpSourceConfig config) throws Exception {
        String className = config.getDecoderClass();
        String decoderName = config.getDecoderName();

        if (className != null && !className.trim().isEmpty()) {
            Class<?> clazz =
                    Class.forName(
                            className.trim(),
                            true,
                            Thread.currentThread().getContextClassLoader());
            if (PacketCodecProvider.class.isAssignableFrom(clazz)) {
                PacketCodecProvider provider =
                        (PacketCodecProvider) clazz.getDeclaredConstructor().newInstance();
                return provider.create(config);
            }
            if (PacketCodec.class.isAssignableFrom(clazz)) {
                return (PacketCodec) clazz.getDeclaredConstructor().newInstance();
            }
            throw new IllegalArgumentException(
                    "decoderClass must implement PacketCodec or PacketCodecProvider: " + className);
        }

        if (decoderName != null && !decoderName.trim().isEmpty()) {
            return loadByProviderName(decoderName.trim(), config);
        }

        throw new IllegalArgumentException(
                "parseMode=custom requires decoderClass (FQCN) or decoderName (SPI name)");
    }

    private static PacketCodec loadByProviderName(String name, UdpSourceConfig config)
            throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        for (PacketCodecProvider provider : ServiceLoader.load(PacketCodecProvider.class, cl)) {
            if (name.equalsIgnoreCase(provider.name())) {
                return provider.create(config);
            }
        }
        // Fallback without SPI file (direct built-ins)
        if ("string".equalsIgnoreCase(name)) {
            return new StringPacketCodecProvider().create(config);
        }
        if ("bytes".equalsIgnoreCase(name)) {
            return new BytesPacketCodecProvider().create(config);
        }
        throw new IllegalArgumentException(
                "No PacketCodecProvider found for name='"
                        + name
                        + "'. Register META-INF/services/"
                        + PacketCodecProvider.class.getName()
                        + " or use decoderClass.");
    }
}
