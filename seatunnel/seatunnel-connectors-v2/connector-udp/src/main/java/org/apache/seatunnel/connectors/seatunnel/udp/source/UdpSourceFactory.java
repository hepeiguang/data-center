package org.apache.seatunnel.connectors.seatunnel.udp.source;

import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.api.source.SeaTunnelSource;
import org.apache.seatunnel.api.source.SourceSplit;
import org.apache.seatunnel.api.table.connector.TableSource;
import org.apache.seatunnel.api.table.factory.Factory;
import org.apache.seatunnel.api.table.factory.TableSourceFactory;
import org.apache.seatunnel.api.table.factory.TableSourceFactoryContext;

import com.google.auto.service.AutoService;

import java.io.Serializable;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.BUFFER_SIZE;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.CHARSET;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.DECODER_CLASS;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.DECODER_INCLUDE_META;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.DECODER_NAME;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.FORMAT;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.HOST;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.IDENTIFIER;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.OFFSET_TABLE;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.OFFSET_TABLE_PATH;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.PARSE_ERROR_HANDLE_WAY;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.PARSE_MODE;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.PORT;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.RECEIVE_QUEUE_SIZE;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.SCRIPT_CONTENT;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.SCRIPT_PATH;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.SO_RCVBUF;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.TRANSPORT;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.UDF_NAME;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.WORKER_THREADS;

/**
 * 负责插件配置解析和组件实例化。
 *
 * <p>协作流程 sequenceDiagram participant Engine participant UdpSourceFactory participant UdpSource
 *
 * <p>Engine->>UdpSourceFactory: factoryIdentifier() 验证插件名称 Engine->>UdpSourceFactory: optionRule()
 * 校验配置参数 Engine->>UdpSourceFactory: createSource(config) 创建Source实例 UdpSourceFactory->>UdpSource:
 * 实例化(host, port, bufferSize) Engine->>UdpSource: 调用getBoundedness()/createReader()等
 */
@AutoService(Factory.class)
public class UdpSourceFactory implements TableSourceFactory {

    /**
     * 返回该工厂类的唯一标识符，与配置文件中的 plugin_name 字段匹配，用于 SeaTunnel 引擎识别并加载对应的插件。
     *
     * <p>注意： 必须与用户配置中的 plugin_name 严格一致（大小写敏感），否则引擎会报插件未找到错误。
     *
     * @return
     */
    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    /**
     * 定义该插件的配置参数规则，包括必填参数、可选参数及其校验逻辑（如类型、取值范围）。 SeaTunnel 会在初始化时自动校验用户配置是否符合规则。
     *
     * @return
     */
    @Override
    public OptionRule optionRule() {
        return OptionRule.builder()
                .required(HOST, PORT)
                .optional(
                        BUFFER_SIZE,
                        CHARSET,
                        FORMAT,
                        PARSE_MODE,
                        SCRIPT_PATH,
                        SCRIPT_CONTENT,
                        UDF_NAME,
                        OFFSET_TABLE,
                        OFFSET_TABLE_PATH,
                        DECODER_CLASS,
                        DECODER_NAME,
                        DECODER_INCLUDE_META,
                        PARSE_ERROR_HANDLE_WAY,
                        TRANSPORT,
                        WORKER_THREADS,
                        RECEIVE_QUEUE_SIZE,
                        SO_RCVBUF)
                .build();
    }

    /**
     * 根据用户配置（pluginConfig）创建并返回 SeaTunnelSource 实例，是工厂类的核心方法。
     *
     * @param context TableFactoryContext
     * @param <T>
     * @param <SplitT>
     * @param <StateT>
     * @return
     */
    @Override
    public <T, SplitT extends SourceSplit, StateT extends Serializable>
            TableSource<T, SplitT, StateT> createSource(TableSourceFactoryContext context) {
        return () ->
                (SeaTunnelSource<T, SplitT, StateT>)
                        new UdpSource(context.getOptions());
    }

    /**
     * 返回该工厂类对应的 SeaTunnelSource 实现类的 Class 对象，用于框架反射检查类型一致性。
     *
     * @return
     */
    @Override
    public Class<? extends SeaTunnelSource> getSourceClass() {
        return UdpSource.class;
    }
}
