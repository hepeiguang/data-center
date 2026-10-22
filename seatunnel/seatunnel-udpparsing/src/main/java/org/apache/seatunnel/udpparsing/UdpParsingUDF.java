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
        // ZetaSQLType
        // 类型
        String type  = (String) args.get(1);
        log.info(" =====udp type=====:" + type);
        Object value  =  args.get(0);
        // 值
        log.info(" =====udp value=====:" + value);
//        if (value == null || StringUtils.isBlank(String.valueOf(value))) {
//            return null;
//        }
//        // 判断类型是否为空
//        if (StringUtils.isNoneBlank(type)) {
//            try {
//                switch (SqlType.valueOf(type.toUpperCase())) {
//                    case STRING:
//                        return value.toString();
//                    case BOOLEAN:
//                        return new Boolean(String.valueOf(value)) ;
//                    case TINYINT:
//                        return (Byte) value;
//                    case SMALLINT:
//                        return (Short) value;
//                    case INT:
//                        return new Integer(String.valueOf(value)) ;
//                    case BIGINT:
//                        return (Long) value;
//                    case FLOAT:
//                        return (Float) value;
//                    case DATE:
//                        return LocalDate.parse(value.toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
//                    case TIME:
//                        return LocalTime.parse(value.toString());
//                    case TIMESTAMP:
//                        return LocalDateTime.parse(value.toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
////                case TIMESTAMP_TZ:
////                    return (Integer)value;
////                case BINARY_VECTOR:
////                    return (Integer)value;
////                case FLOAT_VECTOR:
////                    return (Integer)value;
////                case FLOAT16_VECTOR:
////                    return (Integer)value;
////                case BFLOAT16_VECTOR:
////                    return (Integer)value;
////                case SPARSE_FLOAT_VECTOR:
////                    return (Integer)value;
//                    case ROW:
//                    case MULTIPLE_ROW:
//                    case ARRAY:
//                    case MAP:
//                    default:
//                        return JsonUtils.toJsonString(value);
//                }
//
//            } catch (Exception e) {
//                log.error(" =====e.getMessage()=====:" + e.getMessage());
//            }
//        }
//        if(value instanceof Byte){
//            return (Byte) value;
//        } else if(value instanceof Boolean){
//            return (Boolean) value;
//        } else if(value instanceof Short){
//            return (Short) value;
//        } else if(value instanceof Char){
//            return (Char) value;
//        } else if(value instanceof Integer){
//            return (Integer) value;
//        } else if(value instanceof Long){
//            return (Long) value;
//        } else if(value instanceof Float){
//            return (Float) value;
//        } else if(value instanceof Double){
//            return (Double) value;
//        }
        return value;
    }


}
