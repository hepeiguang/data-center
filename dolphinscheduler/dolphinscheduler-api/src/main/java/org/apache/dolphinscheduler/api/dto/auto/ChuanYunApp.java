package org.apache.dolphinscheduler.api.dto.auto;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
public class ChuanYunApp {
    @SerializedName("ObjectId")
    private String objectId;
    @SerializedName("Code")
    private String code;
    @SerializedName("AppCode")
    private String appCode;
    @SerializedName("ParentCode")
    private String parentCode;
    @SerializedName("DisplayName")
    private String displayName;
    @SerializedName("SortKey")
    private Integer sortKey;
    @SerializedName("State")
    private Integer state;
    @SerializedName("NodeVisible")
    private Integer nodeVisible;
    @SerializedName("NodeType")
    private Integer nodeType;
    @SerializedName("IconCss")
    private String iconCss;
    @SerializedName("IconColorIndex")
    private Integer iconColorIndex;
    @SerializedName("Url")
    private String url;
    @SerializedName("EnableDataAcl")
    private Integer enableDataAcl;
    @SerializedName("ParentObjectId")
    private String parentObjectId;
    @SerializedName("ParentPropertyName")
    private String parentPropertyName;
    @SerializedName("ParentIndex")
    private Integer parentIndex;
    @SerializedName("Summary")
    private String summary;
    @SerializedName("pinyin_initials")
    private String pinyin_initials;
}