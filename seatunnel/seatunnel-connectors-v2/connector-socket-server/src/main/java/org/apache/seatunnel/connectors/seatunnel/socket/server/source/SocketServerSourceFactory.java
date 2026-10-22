package org.apache.seatunnel.connectors.seatunnel.socket.server.source;

import com.google.auto.service.AutoService;
import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.api.source.SeaTunnelSource;
import org.apache.seatunnel.api.source.SourceSplit;
import org.apache.seatunnel.api.table.connector.TableSource;
import org.apache.seatunnel.api.table.factory.Factory;
import org.apache.seatunnel.api.table.factory.TableSourceFactory;
import org.apache.seatunnel.api.table.factory.TableSourceFactoryContext;
import org.apache.seatunnel.connectors.seatunnel.socket.server.config.SocketServerOptions;

import java.io.Serializable;

import static org.apache.seatunnel.connectors.seatunnel.socket.server.config.SocketServerOptions.IDENTIFIER;

/**
 * 负责插件配置解析和组件实例化。
 *
 * <p>协作流程 sequenceDiagram participant Engine participant SocketServerSourceFactory participant SocketServerSource
 *
 * <p>Engine->>SocketServerSourceFactory: factoryIdentifier() 验证插件名称 Engine->>SocketServerSourceFactory: optionRule()
 * 校验配置参数 Engine->>SocketServerSourceFactory: createSource(config) 创建Source实例 SocketServerSourceFactory->>SocketServerSource:
 * 实例化(host, port, bufferSize) Engine->>SocketServerSource: 调用getBoundedness()/createReader()等
 */
@AutoService(Factory.class)
public class SocketServerSourceFactory implements TableSourceFactory {

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
                .required(SocketServerOptions.HOST, SocketServerOptions.PORT)
                .optional(
                        SocketServerOptions.CHARSET,
                        SocketServerOptions.BUFFER_SIZE,
                        SocketServerOptions.REPLY_ENABLED,
                        SocketServerOptions.REPLY_MESSAGE,
                        SocketServerOptions.MAX_CONNECTIONS,
                        SocketServerOptions.SOCKET_TIMEOUT,
                        SocketServerOptions.DELIMITER
                )
                .build();
    }

    /**
     * 根据用户配置（pluginConfig）创建并返回 SeaTunnelSource 实例，是工厂类的核心方法。
     *
     * @param context  TableFactoryContext
     * @param <T>
     * @param <SplitT>
     * @param <StateT>
     * @return
     */
    @Override
    public <T, SplitT extends SourceSplit, StateT extends Serializable> TableSource<T, SplitT, StateT> createSource(TableSourceFactoryContext context) {
        return () ->
                (SeaTunnelSource<T, SplitT, StateT>)
                        new SocketServerSource(context.getOptions());
    }

    /**
     * 返回该工厂类对应的 SeaTunnelSource 实现类的 Class 对象，用于框架反射检查类型一致性。
     *
     * @return
     */
    @Override
    public Class<? extends SeaTunnelSource> getSourceClass() {
        return SocketServerSource.class;
    }
}
