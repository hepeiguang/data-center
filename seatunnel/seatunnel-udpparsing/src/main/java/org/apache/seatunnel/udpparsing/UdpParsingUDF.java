package org.apache.seatunnel.udpparsing;

import com.google.auto.service.AutoService;
import org.apache.seatunnel.api.table.type.*;
import org.apache.seatunnel.transform.sql.zeta.ZetaUDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@AutoService(ZetaUDF.class)
public class UdpParsingUDF implements ZetaUDF {
    private static final Logger log = LoggerFactory.getLogger(UdpParsingUDF.class);
    @Override
    public String functionName() {
        return "UDP_PARSING_UDF";
    }

    @Override
    public SeaTunnelDataType<?> resultType(List<SeaTunnelDataType<?>> argsType) {
        for (int i = 0; i < argsType.size(); i++) {
            return argsType.get(i);
        }
        return  BasicType.STRING_TYPE;
    }

    @Override
    public Object evaluate(List<Object> args) {
        // R12 security fix: removed sensitive data logging (payload was logged at INFO level)
        // The type-conversion logic is commented out (see PRD R12 for full decision).
        // Currently returns raw value passthrough.
        String type = (String) args.get(1);
        Object value = args.get(0);

        if (value == null) {
            return null;
        }

        log.debug("UDP_PARSING_UDF evaluate: type={}, valueLength={}",
                type, value != null ? String.valueOf(value).length() : 0);

        return value;
    }


}
