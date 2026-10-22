package org.apache.seatunnel.transform.splitColumn;

import org.apache.seatunnel.api.configuration.ReadonlyConfig;
import org.apache.seatunnel.api.table.catalog.CatalogTable;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.transform.SeaTunnelTransform;
import org.apache.seatunnel.transform.common.AbstractMultiCatalogFlatMapTransform;

import java.util.List;

/**
 * @Author： 王涛
 * @DATE： 2025/5/13 17:09
 */
public class SplitColumnMultiCatalogTransform extends AbstractMultiCatalogFlatMapTransform {
    public SplitColumnMultiCatalogTransform(
            List<CatalogTable> inputCatalogTables, ReadonlyConfig config) {
        super(inputCatalogTables, config);
    }

    @Override
    public String getPluginName() {
        return SplitColumnTransform.PLUGIN_NAME;
    }

    @Override
    protected SeaTunnelTransform<SeaTunnelRow> buildTransform(
            CatalogTable inputCatalogTable, ReadonlyConfig config) {
        return new SplitColumnTransform(SplitColumnTransformConfig.of(config), inputCatalogTable);
    }
}
