package org.apache.seatunnel.transform.splitColumn;

import com.google.auto.service.AutoService;
import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.api.table.connector.TableTransform;
import org.apache.seatunnel.api.table.factory.Factory;
import org.apache.seatunnel.api.table.factory.TableTransformFactory;
import org.apache.seatunnel.api.table.factory.TableTransformFactoryContext;
import org.apache.seatunnel.transform.common.TransformCommonOptions;

/**
 * @Author： 王涛
 * @DATE： 2025/5/13 17:07
 */
@AutoService(Factory.class)
public class SplitColumnTransformFactory implements TableTransformFactory {
    @Override
    public String factoryIdentifier() {
        return SplitColumnTransform.PLUGIN_NAME;
    }

    @Override
    public OptionRule optionRule() {
        return OptionRule.builder()
                .optional(
                        SplitColumnTransformConfig.KEY_SEPARATOR,
                        SplitColumnTransformConfig.KEY_SPLIT_FIELD,
                        SplitColumnTransformConfig.KEY_REGEX,
                        SplitColumnTransformConfig.KEY_NEW_FIELD)
                .optional(TransformCommonOptions.MULTI_TABLES)
                .optional(TransformCommonOptions.TABLE_MATCH_REGEX)
                .build();
    }

    @Override
    public TableTransform createTransform(TableTransformFactoryContext context) {
        return () ->
                new SplitColumnMultiCatalogTransform(context.getCatalogTables(), context.getOptions());
    }
}