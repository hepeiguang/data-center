package org.apache.seatunnel.transform.splitColumn;

import lombok.extern.slf4j.Slf4j;
import org.apache.seatunnel.api.table.catalog.*;
import org.apache.seatunnel.api.table.type.BasicType;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.transform.common.AbstractCatalogSupportFlatMapTransform;
import org.apache.seatunnel.transform.exception.TransformCommonError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class SplitColumnTransform extends AbstractCatalogSupportFlatMapTransform {

    public static String PLUGIN_NAME = "SplitColumn";

    private final SplitColumnTransformConfig config;
    private int splitFieldIndex;     // 查找要拆分的字段索引



    public SplitColumnTransform(SplitColumnTransformConfig config, CatalogTable catalogTable) {
        super(catalogTable);
        this.config = config;
        SeaTunnelRowType seaTunnelRowType = catalogTable.getTableSchema().toPhysicalRowDataType();
        try {
            splitFieldIndex = seaTunnelRowType.indexOf(config.getSplitField());
        } catch (IllegalArgumentException e) {
            throw TransformCommonError.cannotFindInputFieldError(getPluginName(), config.getSplitField());
        }

    }

    @Override
    public String getPluginName() {
        return SplitColumnTransform.PLUGIN_NAME;
    }


    @Override
    protected List<SeaTunnelRow> transformRow(SeaTunnelRow inputRow) {
        Object[] inputFields = inputRow.getFields();
        Object fieldValue = String.valueOf(inputFields[splitFieldIndex]);

        if (fieldValue == null) {
            return Collections.emptyList();
        }

        String[] parts;
        if (config.getRegex() != null && config.getRegex() == 1) {
            // 使用正则拆分
            parts = fieldValue.toString().split(config.getSeparator());
        } else {
            // 使用普通分隔符拆分（注意转义）
            parts = fieldValue.toString().split(Pattern.quote(config.getSeparator()));
        }


        List<SeaTunnelRow> result = new ArrayList<>();
        for (String val : parts) {
            Object[] newFields = new Object[inputFields.length];  // 保留原字段
            System.arraycopy(inputFields, 0, newFields, 0, inputFields.length);
            newFields[splitFieldIndex] = val; // 替换为拆分出的单个值

            result.add(new SeaTunnelRow(newFields));
        }
        return result;
    }

    @Override
    protected TableSchema transformTableSchema() {
        TableSchema.Builder schemaBuilder = TableSchema.builder();

        // 添加原始字段
        List<Column> columns =
                inputCatalogTable.getTableSchema().getColumns().stream()
                        .map(Column::copy)
                        .collect(Collectors.toList());

        // 替换为新字段
        columns.set(splitFieldIndex,PhysicalColumn.of(config.getNewField(), BasicType.STRING_TYPE, 0, true, null, null) );

        schemaBuilder.columns(columns);

        log.info("transformTableSchema end ");
        return schemaBuilder.build();
    }

    @Override
    protected TableIdentifier transformTableIdentifier() {
        return inputCatalogTable.copy().getTableId();
    }
}
