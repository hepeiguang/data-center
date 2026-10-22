package org.apache.seatunnel.connectors.seatunnel.http.server.source;

import com.google.auto.service.AutoService;
import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.api.source.SeaTunnelSource;
import org.apache.seatunnel.api.source.SourceSplit;
import org.apache.seatunnel.api.table.connector.TableSource;
import org.apache.seatunnel.api.table.factory.Factory;
import org.apache.seatunnel.api.table.factory.TableSourceFactory;
import org.apache.seatunnel.api.table.factory.TableSourceFactoryContext;
import org.apache.seatunnel.connectors.seatunnel.http.server.config.HttpServerOptions;

import java.io.Serializable;

import static org.apache.seatunnel.connectors.seatunnel.http.server.config.HttpServerOptions.*;

@AutoService(Factory.class)
public class HttpServerSourceFactory implements TableSourceFactory {

    @Override
    public Class<? extends SeaTunnelSource> getSourceClass() {
        return HttpServerSource.class;
    }

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public OptionRule optionRule() {

        return OptionRule.builder()
                // Required basic configuration
                .required(HttpServerOptions.HOST, HttpServerOptions.PORT, ENDPOINT)

                // Optional configuration
                .optional(HttpServerOptions.METHOD, HttpServerOptions.TIMEOUT_MS,
                        HttpServerOptions.MAX_CONTENT_LENGTH, FORMAT, MAX_THREADS, QUEUE_CAPACITY)
                .build();
    }

    @Override
    public <T, SplitT extends SourceSplit, StateT extends Serializable> TableSource<T, SplitT, StateT> createSource(TableSourceFactoryContext context) {
        return () ->
                (SeaTunnelSource<T, SplitT, StateT>)
                        new HttpServerSource(context.getOptions());
    }
}
