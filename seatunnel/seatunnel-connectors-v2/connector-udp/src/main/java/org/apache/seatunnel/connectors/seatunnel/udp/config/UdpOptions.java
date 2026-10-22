package org.apache.seatunnel.connectors.seatunnel.udp.config;

import org.apache.seatunnel.api.configuration.Option;
import org.apache.seatunnel.api.configuration.Options;

public class UdpOptions {

    public static final String IDENTIFIER = "UDP";

    // common option
    public static final Option<String> HOST =
            Options.key("host")
                    .stringType()
                    .defaultValue("0.0.0.0")
                    .withDescription("udp server address.");

    public static final Option<Integer> PORT =
            Options.key("port").intType().noDefaultValue().withDescription("udp server port.");

    public static final Option<Integer> BUFFER_SIZE =
            Options.key("bufferSize")
                    .intType()
                    .defaultValue(4096)
                    .withDescription("udp pkg buffer size");

    public static final Option<String> CHARSET =
            Options.key("charset")
                    .stringType()
                    .defaultValue("UTF-8")
                    .withDescription("charset for text format / reply payload");
    public static final Option<Boolean> REPLY_ENABLED =
            Options.key("replyEnabled")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("是否回复消息");
    public static final Option<String> REPLY_MESSAGE =
            Options.key("replyMessage")
                    .stringType()
                    .defaultValue("ACK")
                    .withDescription("回复消息内容");
    public static final Option<UdpOptions.Format> FORMAT =
            Options.key("format")
                    .enumType(UdpOptions.Format.class)
                    .defaultValue(UdpOptions.Format.TEXT)
                    .withDescription(
                            "legacy format when parseMode=none: json or text.");

    public static final Option<UdpOptions.ParseMode> PARSE_MODE =
            Options.key("parseMode")
                    .enumType(UdpOptions.ParseMode.class)
                    .defaultValue(UdpOptions.ParseMode.none)
                    .withDescription("none | js | offset_table");

    public static final Option<UdpOptions.ParseErrorHandleWay> PARSE_ERROR_HANDLE_WAY =
            Options.key("parseErrorHandleWay")
                    .enumType(UdpOptions.ParseErrorHandleWay.class)
                    .defaultValue(UdpOptions.ParseErrorHandleWay.skip)
                    .withDescription(
                            "When codec parse fails: skip packet (default) or fail the job.");

    public static final Option<String> SCRIPT_PATH =
            Options.key("scriptPath")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("JS script file path (hot-reload by mtime)");

    public static final Option<String> SCRIPT_CONTENT =
            Options.key("scriptContent")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Inline JS script (parse function)");

    public static final Option<String> OFFSET_TABLE =
            Options.key("offsetTable")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Inline offset table JSON DSL");

    public static final Option<String> OFFSET_TABLE_PATH =
            Options.key("offsetTablePath")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Offset table JSON file path");

    public static final Option<Integer> MAX_RETRIES =
            Options.key("maxRetries")
                    .intType()
                    .defaultValue(3)
                    .withDescription("发送消息最大尝试次数");
    public static final Option<Integer> TIMEOUT_MS =
            Options.key("timeoutMs")
                    .intType()
                    .defaultValue(1000)
                    .withDescription("发送消息超时时间");
    public static final Option<Boolean> ENABLE_MESSAGE_REPLY =
            Options.key("enableMessageReply")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("是否等待UDP服务回复消息");
    public static final Option<Integer> RETRY_INTERVAL_MS =
            Options.key("retryIntervalMs")
                    .intType()
                    .defaultValue(1000)
                    .withDescription("失败重试间隔毫秒");
    public static final Option<Integer> BATCH_SIZE =
            Options.key("batchSize")
                    .intType()
                    .defaultValue(100)
                    .withDescription("批量队列大小");
    public static final Option<Integer> BATCH_INTERVAL_MS =
            Options.key("batchIntervalMs")
                    .intType()
                    .defaultValue(1000)
                    .withDescription("批量时间");
    public static final Option<Boolean> ASYNC_ENABLED =
            Options.key("asyncEnabled")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription("是否启用异步");
    public static final Option<Integer> ASYNC_THREADS =
            Options.key("asyncThreads")
                    .intType()
                    .defaultValue(2)
                    .withDescription("异步线程数");
    public static final Option<Integer> MAX_QUEUE_SIZE =
            Options.key("maxQueueSize")
                    .intType()
                    .defaultValue(10000)
                    .withDescription("异步队列大小");
    public static final Option<Integer> MAX_WAIT_MS =
            Options.key("maxWaitMs")
                    .intType()
                    .defaultValue(10000)
                    .withDescription("异步处理等待时长");



    public enum Format {
        JSON,
        TEXT,
    }

    public enum ParseMode {
        none,
        js,
        offset_table
    }

    public enum ParseErrorHandleWay {
        /** Log and drop the bad packet; keep receiving. */
        skip,
        /** Rethrow and fail the source reader / job. */
        fail
    }
}
