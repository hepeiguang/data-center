package org.apache.dolphinscheduler.plugin.task.minio.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileTypeEnum {
    XLSX("xlsx","1", "1","结构化"),
    TXT("txt","2", "1","结构化"),
    ORC("orc","3", "1","结构化"),
    XLS("xls","4", "1","结构化"),
    CSV("csv","5", "1","结构化"),
    LOG("log","6", "2","半结构化"),
    WORD("word","7", "3","非结构化"),
    PDF("pdf","8", "3","非结构化"),
    JPG("jpg","9", "3","非结构化"),
    OTHER("other","99", "3","非结构化");

    private final String fileTypeName;
    private final String fileType;
    private final String structureType;
    private final String structureName;

    public static FileTypeEnum getFileType(String fileTypeName) {
        for (FileTypeEnum value : FileTypeEnum.values()) {
            if (value.fileTypeName.equals(fileTypeName)) {
                return value;
            }
        }
        return OTHER;
    }
}
