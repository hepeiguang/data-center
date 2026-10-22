package org.apache.seatunnel.transform.range;

import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.api.table.connector.TableTransform;
import org.apache.seatunnel.api.table.factory.Factory;
import org.apache.seatunnel.api.table.factory.TableTransformFactory;
import org.apache.seatunnel.api.table.factory.TableTransformFactoryContext;
import org.apache.seatunnel.transform.common.TransformCommonOptions;

import com.google.auto.service.AutoService;

@AutoService(Factory.class)
public class RangeTransformFactory implements TableTransformFactory {
    @Override
    public String factoryIdentifier() {
        return "Range";
    }

    @Override
    public OptionRule optionRule() {
        return OptionRule.builder()
                .optional(
                        RangeTransformConfig.KEY_RANGE_TYPE,
                        RangeTransformConfig.KEY_RANGE_VALUE_DEFAULT,
                        RangeTransformConfig.KEY_RANGE_FIELD,
                        RangeTransformConfig.KEY_RANGE_TO_FIELD,
                        RangeTransformConfig.KEY_RANGE_TO_FIELD_TYPE,
                        RangeTransformConfig.KEY_RANGE_MAPPINGS)
                .optional(TransformCommonOptions.MULTI_TABLES)
                .optional(TransformCommonOptions.TABLE_MATCH_REGEX)
                .build();
    }

    @Override
    public TableTransform createTransform(TableTransformFactoryContext context) {
        return () ->
                new RangeMultiCatalogTransform(context.getCatalogTables(), context.getOptions());
    }
}