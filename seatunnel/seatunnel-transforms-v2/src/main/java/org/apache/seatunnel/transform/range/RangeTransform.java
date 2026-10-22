package org.apache.seatunnel.transform.range;

import org.apache.seatunnel.api.table.catalog.CatalogTable;
import org.apache.seatunnel.api.table.catalog.Column;
import org.apache.seatunnel.api.table.catalog.PhysicalColumn;
import org.apache.seatunnel.api.table.type.BasicType;
import org.apache.seatunnel.api.table.type.SeaTunnelRowAccessor;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.transform.common.MultipleFieldOutputTransform;

import lombok.NonNull;
import org.apache.seatunnel.transform.exception.TransformCommonError;


public class RangeTransform extends MultipleFieldOutputTransform {
    public static String PLUGIN_NAME = "Range";
    private final RangeTransformConfig config;

    private final int fieldIndex;

    public RangeTransform(
            @NonNull RangeTransformConfig config,
            @NonNull CatalogTable catalogTable) {
        super(catalogTable);
        this.config = config;
        SeaTunnelRowType seaTunnelRowType = catalogTable.getTableSchema().toPhysicalRowDataType();
        try {
            fieldIndex = seaTunnelRowType.indexOf(config.getRangeField());
        } catch (IllegalArgumentException e) {
            throw TransformCommonError.cannotFindInputFieldError(
                    getPluginName(), config.getRangeField());
        }

        this.outputCatalogTable = getProducedCatalogTable();
    }


    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    protected Column[] getOutputColumns() {
        switch (config.getRangeToFieldType()) {
            case "STRING":
                return new Column[]{PhysicalColumn.of(config.getRangeToField(), BasicType.STRING_TYPE, 200L, true, null, "")};
            case "INT":
                return new Column[]{PhysicalColumn.of(config.getRangeToField(), BasicType.INT_TYPE, 11L, true, null, "")};
            case "LONG":
                return new Column[]{PhysicalColumn.of(config.getRangeToField(), BasicType.LONG_TYPE, 11L, true, null, "")};
            case "FLOAT":
                return new Column[]{PhysicalColumn.of(config.getRangeToField(), BasicType.FLOAT_TYPE, 11L, true, null, "")};
            case "DOUBLE":
                return new Column[]{PhysicalColumn.of(config.getRangeToField(), BasicType.DOUBLE_TYPE, 11L, true, null, "")};
            case "BOOLEAN":
                return new Column[]{PhysicalColumn.of(config.getRangeToField(), BasicType.BOOLEAN_TYPE, 1L, true, null, "")};
            default:
                throw new IllegalArgumentException("不支持的类型，创建字段" + config.getRangeToField() + "失败");
        }
    }

    @Override
    protected Object[] getOutputFieldValues(SeaTunnelRowAccessor inputRow) {
        Object rangeFieldValue = inputRow.getField(fieldIndex);
        if (rangeFieldValue == null) {
            return new Object[]{convertValue(config.getRangeValueDefault(), config.getRangeToFieldType())};
        }

        double value = Double.parseDouble(rangeFieldValue.toString());
        for (RangeMapping mapping : config.getRangeMappings()) {
            if ("greater_than_or_equal".equals(config.getRangeType())) {
                if (value >= mapping.getRangeStart() && value < mapping.getRangeEnd()) {
                    return new Object[]{convertValue(mapping.getRangeValue(), config.getRangeToFieldType())};
                }
            } else if ("less_than_or_equal".equals(config.getRangeType())) {
                if (value <= mapping.getRangeStart() && value > mapping.getRangeEnd()) {
                    return new Object[]{convertValue(mapping.getRangeValue(), config.getRangeToFieldType())};
                }
            }
        }
        return new Object[]{convertValue(config.getRangeValueDefault(), config.getRangeToFieldType())};
    }

    private Object convertValue(String value, String targetType) {
        switch (targetType) {
            case "STRING":
                return value;
            case "INT":
                return Integer.parseInt(value);
            case "LONG":
                return Long.parseLong(value);
            case "FLOAT":
                return Float.parseFloat(value);
            case "DOUBLE":
                return Double.parseDouble(value);
            case "BOOLEAN":
                return Boolean.parseBoolean(value);
            default:
                throw new IllegalArgumentException("Unsupported target type: " + targetType);
        }
    }


}