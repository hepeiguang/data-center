package org.apache.seatunnel.connectors.seatunnel.udp.config;

import lombok.Data;
import org.apache.seatunnel.api.configuration.ReadonlyConfig;

import java.io.Serializable;

import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.BUFFER_SIZE;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.CHARSET;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.DECODER_CLASS;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.DECODER_INCLUDE_META;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.DECODER_NAME;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.FORMAT;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.HOST;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.OFFSET_TABLE;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.OFFSET_TABLE_PATH;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.PARSE_ERROR_HANDLE_WAY;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.PARSE_MODE;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.PORT;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.RECEIVE_QUEUE_SIZE;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.REPLY_ENABLED;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.REPLY_MESSAGE;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.SCRIPT_CONTENT;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.SCRIPT_PATH;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.SO_RCVBUF;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.TRANSPORT;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.UDF_NAME;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.WORKER_THREADS;

@Data
public class UdpSourceConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private String host;
    private int port;
    private int bufferSize;
    private String charset;
    private boolean replyEnabled;
    private String replyMessage;
    private UdpOptions.Format format;
    private UdpOptions.ParseMode parseMode;
    private String scriptPath;
    private String scriptContent;
    private String offsetTable;
    private String offsetTablePath;
    private String udfName;
    private String decoderClass;
    private String decoderName;
    private boolean decoderIncludeMeta;
    private UdpOptions.ParseErrorHandleWay parseErrorHandleWay;
    private UdpOptions.Transport transport;
    private int workerThreads;
    private int receiveQueueSize;
    private int soRcvBuf;

    public UdpSourceConfig(ReadonlyConfig config) {
        this.port = config.get(PORT);
        this.host = config.get(HOST);
        this.bufferSize = config.get(BUFFER_SIZE);
        this.charset = config.get(CHARSET);
        this.replyEnabled = config.get(REPLY_ENABLED);
        this.replyMessage = config.get(REPLY_MESSAGE);
        this.format = config.get(FORMAT);
        this.parseMode = config.get(PARSE_MODE);
        this.scriptPath = config.getOptional(SCRIPT_PATH).orElse(null);
        this.scriptContent = config.getOptional(SCRIPT_CONTENT).orElse(null);
        this.offsetTable = config.getOptional(OFFSET_TABLE).orElse(null);
        this.offsetTablePath = config.getOptional(OFFSET_TABLE_PATH).orElse(null);
        this.udfName = config.getOptional(UDF_NAME).orElse(null);
        this.decoderClass = config.getOptional(DECODER_CLASS).orElse(null);
        this.decoderName = config.getOptional(DECODER_NAME).orElse(null);
        this.decoderIncludeMeta = config.get(DECODER_INCLUDE_META);
        this.parseErrorHandleWay = config.get(PARSE_ERROR_HANDLE_WAY);
        this.transport = config.get(TRANSPORT);
        this.workerThreads = config.get(WORKER_THREADS);
        this.receiveQueueSize = config.get(RECEIVE_QUEUE_SIZE);
        this.soRcvBuf = config.get(SO_RCVBUF);
    }
}
