package org.apache.seatunnel.transform.sequence;

import org.apache.seatunnel.api.configuration.ReadonlyConfig;
import org.apache.seatunnel.api.table.catalog.CatalogTable;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.transform.SeaTunnelTransform;
import org.apache.seatunnel.transform.common.AbstractMultiCatalogMapTransform;

import java.util.List;

public class SequenceMultiCatalogTransform extends AbstractMultiCatalogMapTransform {

    public SequenceMultiCatalogTransform(List<CatalogTable> inputCatalogTables, ReadonlyConfig config) {
        super(inputCatalogTables, config);
    }

    @Override
    public String getPluginName() {
        return SequenceTransform.PLUGIN_NAME;
    }

    @Override
    protected SeaTunnelTransform<SeaTunnelRow> buildTransform(CatalogTable inputCatalogTable, ReadonlyConfig config) {
        return new SequenceTransform(
                SequenceTransformConfig.of(config),
                inputCatalogTable);
    }
}
