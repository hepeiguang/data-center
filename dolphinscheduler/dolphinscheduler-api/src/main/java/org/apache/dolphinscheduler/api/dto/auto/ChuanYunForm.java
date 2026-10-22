package org.apache.dolphinscheduler.api.dto.auto;

import com.facebook.presto.jdbc.internal.jackson.annotation.JsonInclude;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class ChuanYunForm {
    @SerializedName("ObjectId")
    private String objectId;
    @SerializedName("Code")
    private String code;
    @SerializedName("AppCode")
    private String appCode;
    @SerializedName("ParentCode")
    private String parentCode;
    @SerializedName("IsWorkflowNode")
    private boolean isWorkflowNode;
    @SerializedName("IsWorkflowModule")
    private boolean isWorkflowModule;
    @SerializedName("DisplayName")
    private String displayName;
    @SerializedName("SortKey")
    private int sortKey;
    @SerializedName("State")
    private int state;
    @SerializedName("NodeVisible")
    private int nodeVisible;
    @SerializedName("Visible")
    private boolean visible;
    @SerializedName("Removable")
    private boolean removable;
    @SerializedName("IsAppSetting")
    private boolean isAppSetting;
    @SerializedName("NodeType")
    private int nodeType;
    @SerializedName("IconCss")
    private String iconCss;
    @SerializedName("IconColorIndex")
    private int iconColorIndex;
    @SerializedName("Url")
    private String url;
    @SerializedName("EnableDataAcl")
    private boolean enableDataAcl;
    // 不能确定类型
    @SerializedName("Summary")
    private Object summary;
    @SerializedName("Serialized")
    private boolean serialized;
    @SerializedName("ParentObjectId")
    private String parentObjectId;
    @SerializedName("ParentPropertyName")
    private String parentPropertyName;
    @SerializedName("ParentIndex")
    private int parentIndex;
    @SerializedName("DirtyProperties")
    private List<Object> dirtyProperties;
    @SerializedName("schema")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String schema;
}
