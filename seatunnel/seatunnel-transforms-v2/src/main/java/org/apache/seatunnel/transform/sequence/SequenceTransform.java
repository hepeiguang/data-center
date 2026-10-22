package org.apache.seatunnel.transform.sequence;

import org.apache.seatunnel.api.table.catalog.CatalogTable;
import org.apache.seatunnel.api.table.catalog.Column;
import org.apache.seatunnel.api.table.catalog.PhysicalColumn;
import org.apache.seatunnel.api.table.type.BasicType;
import org.apache.seatunnel.api.table.type.SeaTunnelRowAccessor;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.transform.common.MultipleFieldOutputTransform;

import lombok.NonNull;


public class SequenceTransform extends MultipleFieldOutputTransform {
    public static final String PLUGIN_NAME = "Sequence";
    private final SequenceTransformConfig config;
    private long currentId; // 每个 TaskManager 实例独立维护自己的计数器

    public SequenceTransform(
            @NonNull SequenceTransformConfig config,
            @NonNull CatalogTable catalogTable) {
        super(catalogTable);
        this.config = config;
        this.currentId = config.getStartValue();

        // 新增字段到 schema 中
        CatalogTable inputCatalogTable = catalogTable;
        boolean fieldExists = false;
        for (Column column : inputCatalogTable.getTableSchema().getColumns()) {
            if (config.getFieldName().equals(column.getName())) {
                fieldExists = true;
                break;
            }
        }
        if (fieldExists) {
            throw new IllegalArgumentException("字段已存在: " + config.getFieldName());
        }

        this.outputCatalogTable = getProducedCatalogTable();
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    protected Column[] getOutputColumns() {
        return new Column[]{
            PhysicalColumn.of(config.getFieldName(), BasicType.LONG_TYPE,11, true,null, "自增序列")
        };
    }

    @Override
    protected Object[] getOutputFieldValues(SeaTunnelRowAccessor inputRow) {
        long id = currentId;
        currentId += config.getAddValue();

        if (config.getMaxValue() != null && id > config.getMaxValue()) {
            throw new IllegalStateException("达到最大值限制: " + config.getMaxValue());
        }

        return new Object[]{id};
    }


}
