package org.apache.dolphinscheduler.plugin.task.yuenhezi.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class YunheziFileInfoDto {
    /**
     * 文件或者文件夹id
     */
    @JsonProperty("fid")
    private Long fid;
    /*
     * 文件类型(1:文件,2:文件夹)
     */
    @JsonProperty("fType")
    private Integer fType;
    @JsonProperty("ownerId")
    private Integer ownerId;
    /**
     * 创建者id
     */
    @JsonProperty("creatorId")
    private Integer creatorId;
    @JsonProperty("fName")
    private String fName;
    @JsonProperty("extName")
    private String extName;
    @JsonProperty("version")
    private String version;
    @JsonProperty("fileSize")
    private Long fileSize;
    @JsonProperty("lastOperatorId")
    private Integer lastOperatorId;
    @JsonProperty("lastOperatorName")
    private String lastOperatorName;
    @JsonFormat(pattern = "MMM dd, yyyy h:mm:ss a", locale = "en")
    private Date lastOperateTime;
    @JsonProperty("editorId")
    private Integer editorId;
    @JsonProperty("editorName")
    private String editorName;
    @JsonFormat(pattern = "MMM dd, yyyy h:mm:ss a", locale = "en")
    private Date  editTime;
    @JsonProperty("parentFolderId")
    private Long parentFolderId;
    @JsonProperty("path")
    private String path;
    @JsonProperty("externalNum")
    private Integer externalNum;
    @JsonProperty("isInheritExterna")
    private Boolean isInheritExterna;
    @JsonProperty("taskNum")
    private Integer taskNum;
    @JsonProperty("processId")
    private Long processId;
    @JsonProperty("processState")
    private Integer processState;
    @JsonProperty("processNodeName")
    private String processNodeName;
    @JsonProperty("emailId")
    private Long emailId;
    @JsonProperty("interestType")
    private Integer interestType;
    @JsonProperty("rights")
    private Integer rights;
    @JsonProperty("isDeleted")
    private Boolean isDeleted;
    @JsonProperty("isConflict")
    private Boolean isConflict;
    @JsonProperty("isFavorite")
    private Boolean isFavorite;
    @JsonProperty("isShareSetting")
    private Boolean isShareSetting;
    @JsonProperty("isLimitSetting")
    private Boolean isLimitSetting;
    @JsonProperty("isConfidential")
    private Boolean isConfidential;

    public String getFName() {
        return fName;
    }
    public void setFName(String fName) {
        this.fName = fName;
    }
}
