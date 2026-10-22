package org.apache.seatunnel.transform.sequence;

import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.api.table.connector.TableTransform;
import org.apache.seatunnel.api.table.factory.Factory;
import org.apache.seatunnel.api.table.factory.TableTransformFactory;
import org.apache.seatunnel.api.table.factory.TableTransformFactoryContext;
import org.apache.seatunnel.transform.common.TransformCommonOptions;

import com.google.auto.service.AutoService;

@AutoService(Factory.class)
public class SequenceTransformFactory implements TableTransformFactory {

    @Override
    public String factoryIdentifier() {
        return "Sequence";
    }

    @Override
    public OptionRule optionRule() {
        return OptionRule.builder()
                .required(SequenceTransformConfig.KEY_FIELD_NAME)
                .optional(
                        SequenceTransformConfig.KEY_START_VALUE,
                        SequenceTransformConfig.KEY_ADD_VALUE,
                        SequenceTransformConfig.KEY_MAX_VALUE)
                .optional(TransformCommonOptions.MULTI_TABLES)
                .optional(TransformCommonOptions.TABLE_MATCH_REGEX)
                .build();
    }

    @Override
    public TableTransform createTransform(TableTransformFactoryContext context) {
        return () -> new SequenceMultiCatalogTransform(context.getCatalogTables(), context.getOptions());
    }
}
