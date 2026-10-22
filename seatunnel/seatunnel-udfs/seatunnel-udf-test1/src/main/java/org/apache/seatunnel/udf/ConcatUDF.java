package org.apache.seatunnel.udf;


import com.google.auto.service.AutoService;
import org.apache.seatunnel.api.table.type.BasicType;
import org.apache.seatunnel.api.table.type.SeaTunnelDataType;
import org.apache.seatunnel.transform.sql.zeta.ZetaUDF;

import java.util.List;

@AutoService(ZetaUDF.class)
public class ConcatUDF implements ZetaUDF {
    @Override
    public String functionName() {
        return "CONCAT_STR";
    }

    @Override
    public SeaTunnelDataType<?> resultType(List<SeaTunnelDataType<?>> argsType) {
        return BasicType.STRING_TYPE;
    }

    @Override
    public Object evaluate(List<Object> args) {
        String arg = (String) args.get(0);
        String position = (String) args.get(1);
        String val = (String) args.get(2);
        if (arg == null) return null;
        if ("begin".equals(position)) {
            return val + arg;
        } else if ("end".equals(position)) {
            return arg + val;
        } else {
            return val + arg;
        }
    }
}
