package org.apache.dolphinscheduler.api.dto.auto;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class ChuanYunSchema {
    public static final String[][] BASE_FIELD = {
            {"CreatedByObject", "map<string,string>"},
            {"ModifiedByObject", "map<string,string>"},
            {"OwnerIdObject", "map<string,string>"},
            {"OwnerDeptIdObject", "map<string,string>"}
    };
    public static final String[][] BASE_FIELD_COMMENT = {
            {"CreatedByObjectId", "创建用户id"},
            {"CreatedByObjectName", "创建用户名"},
            {"ModifiedByObjectId", "修改用户id"},
            {"ModifiedByObjectName", "修改用户名"},
            {"OwnerIdObjectId", "拥有用户id"},
            {"OwnerIdObjectName", "拥有用户名"},
            {"OwnerDeptIdObjectId", "拥有部门id"},
            {"OwnerDeptIdObjectName", "拥有部门名"},
    };
    @SerializedName("appCode")
    private String appCode;
    @SerializedName("formCode")
    private String formCode;
    @SerializedName("schema")
    private Map<String, String> schema;
    @SerializedName("schema")
    private Map<String, String> type;


    public Map<String, String> toSimpleSchema() {
        Map<String, String> fieldMap = new HashMap<>();
        for (Map.Entry<String, String> entry : schema.entrySet()) {
            fieldMap.put(entry.getKey(), getSeatunnelType(entry.getValue()));
        }
        //添加公共字段
        for (String[] field : BASE_FIELD) {
            fieldMap.put(field[0], field[1]);
        }
        return fieldMap;
    }

    public String getSqlColumnString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.schema.keySet().stream().collect(Collectors.joining(", ")));

        for (String[] field : BASE_FIELD) {
            sb.append(", ").append(field[0]).append("Id, ").append(field[0]).append("Name");
        }
        return sb.toString();
    }

    private String getSeatunnelType(String cyDataType) {
        cyDataType = cyDataType.toLowerCase();
        switch (cyDataType) {
            case "unspecified":
                return "string";
            case "bool":
                return "boolean";
            case "datetime":
                return "timestamp";
            case "double":
                return "double";
            case "int":
                return "int";
            case "long":
                return "long";
            case "string":
            case "shortstring":
            case "image":
            case "file":
            case "timespan":
            case "unit":
            case "unitarray":
            case "html":
            case "xml":
            case "association":
            case "formula":
                return "string";
            case "bytearray":
                return "bytes";
            case "bizobject":
            case "bizstructure":
            case "map":
            case "address":
                return "map";
            case "bizobjectarray":
            case "bizstructurearray":
            case "associationarray":
                return "array";
            default:
                return "string";
        }
    }

}
