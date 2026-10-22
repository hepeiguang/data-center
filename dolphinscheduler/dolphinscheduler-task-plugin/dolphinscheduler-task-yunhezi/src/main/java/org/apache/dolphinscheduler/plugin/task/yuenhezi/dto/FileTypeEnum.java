package org.apache.dolphinscheduler.plugin.task.yuenhezi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileTypeEnum {

    //文本文件
    TXT("txt","2", "1","结构化"),
    DOC("doc","10", "3","非结构化"),
    DOCX("docx","11", "3","非结构化"),
    PDF("pdf","8", "3","非结构化"),
    RTF("rtf","12", "3","非结构化"),
    WORD("word","7", "3","非结构化"),


    //表格文件

    XLSX("xlsx","1", "1","结构化"),
    XLS("xls","4", "1","结构化"),
    CSV("csv","5", "1","结构化"),
    NUMBERS("numbers","13", "1","结构化"),

    //演示文稿
    PPT("ppt","14", "3","非结构化"),
    PPTX("pptx","15", "3","非结构化"),
    KEY("key","16", "3","非结构化"),


    //电子书
    EPUB("epub","17", "3","非结构化"),
    MOBI("mobi","18", "3","非结构化"),
    AZW("azw","19", "3","非结构化"),

    //图像文件
    PNG("png","20", "3","非结构化"),
    JPEG("jpeg","21", "3","非结构化"),
    GIF("gif","22", "3","非结构化"),
    BMP("bmp","23", "3","非结构化"),
    TIF("tif","24", "3","非结构化"),
    ICO("ico","25", "3","非结构化"),
    WEBP("webp","26", "3","非结构化"),
    SVG("svg","27", "3","非结构化"),
    AI("ai","28", "3","非结构化"),
    EPS("eps","29", "3","非结构化"),
    PS("ps","30", "3","非结构化"),
    CDR("cdr","31", "3","非结构化"),
    PSD("psd","32", "3","非结构化"),
    TIFF("tiff","33", "3","非结构化"),
    DWG("dwg","34", "3","非结构化"),
    DXF("dxf","35", "3","非结构化"),
    JPG("jpg","9", "3","非结构化"),
    RAW("raw","36", "3","非结构化"),
    PDFX("pdfx","37", "3","非结构化"),
    PSDX("psdx","38", "3","非结构化"),
    //音频文件
    MP3("mp3","40", "3","非结构化"),
    WAV("wav","41", "3","非结构化"),
    AAC("aac","42", "3","非结构化"),
    FLAC("flac","43", "3","非结构化"),
    M4A("m4a","44", "3","非结构化"),
    WMA("wma","45", "3","非结构化"),
    MP4("mp4","46", "3","非结构化"),
    M4P("m4p","47", "3","非结构化"),
    AVI("avi","48", "3","非结构化"),
    MPG("mpg","49", "3","非结构化"),
    MPEG("mpeg","50", "3","非结构化"),
    WMV("wmv","51", "3","非结构化"),
    MOV("mov","52", "3","非结构化"),
    MKV("mkv","53", "3","非结构化"),
    WAV_3GP("3gp","54", "3","非结构化"),
    MP2("mp2","55", "3","非结构化"),
    MIDI("midi","56", "3","非结构化"),
    MID("mid","57", "3","非结构化"),
    OGG("ogg","58", "3","非结构化"),
    WMA_WMZ("wma","59", "3","非结构化"),
    FLV("flv","60", "3","非结构化"),
    WEBM("webm","61", "3","非结构化"),
    M4V("m4v","62", "3","非结构化"),


    //压缩文件
    ZIP("zip","160", "3","非结构化"),
    RAR("rar","161", "3","非结构化"),
    zip_7Z("7z","162", "3","非结构化"),
    TAR("tar","163", "3","非结构化"),
    GZ("gz","164", "3","非结构化"),
    BZ2("bz2","165", "3","非结构化"),
    XZ("xz","166", "3","非结构化"),
    Z("z","167", "3","非结构化"),

    //可执行文件
    EXE("exe","63", "3","非结构化"),
    msi("msi","64", "3","非结构化"),
    BAT("bat","65", "3","非结构化"),
    SH("sh","66", "3","非结构化"),
    PY("py","67", "3","非结构化"),
    CLASS("class","68", "3","非结构化"),
    BIN("bin","69", "3","非结构化"),
    DEB("deb","70", "3","非结构化"),
    RPM("rpm","71", "3","非结构化"),
    APP("app","72", "3","非结构化"),
    DMG("dmg","73", "3","非结构化"),
    PKG("pkg","74", "3","非结构化"),
    JAVA("java","75", "3","非结构化"),
    SWF("swf","76", "3","非结构化"),
    VBS("vbs","77", "3","非结构化"),
    JS("js","78", "3","非结构化"),
    CSS("css","79", "3","非结构化"),
    HTML("html","80", "3","非结构化"),
    PHP("php","81", "3","非结构化"),
    TS ("ts","92", "3","非结构化"),
    VUE ("vue","93", "3","非结构化"),
    GO ("go","94", "3","非结构化"),
    R ("r","95", "3","非结构化"),
    RUBY ("ruby","96", "3","非结构化"),
    PERL ("perl","97", "3","非结构化"),
    LUA ("lua","98", "3","非结构化"),
    JAR ("jar","145", "3","非结构化"),


    //项目配置文件
    XML("xml","82", "3","非结构化"),
    JSON("json","83", "3","非结构化"),
    SQL("sql","84", "3","非结构化"),
    C("c","85", "3","非结构化"),
    CPP("cpp","86", "3","非结构化"),
    C_SHARP("c#","87", "3","非结构化"),
    JAVA_SCRIPT("java script","88", "3","非结构化"),
    YAML("yaml","89", "3","非结构化"),
    YML("yml","90", "3","非结构化"),
    Properties("properties","91", "3","非结构化"),
    GRADLE ("gradle","134", "3","非结构化"),

    //系统文件
    DLL ("dll","99", "3","非结构化"),
    SYS ("sys","100", "3","非结构化"),
    INI ("ini","101", "3","非结构化"),
    CONF ("conf","102", "3","非结构化"),
    REG ("reg","103", "3","非结构化"),
    SO("so","104", "3","非结构化"),
    LIB("lib","105", "3","非结构化"),
    SERVICE("service","106", "3","非结构化"),
    CFG ("cfg","107", "3","非结构化"),
    CONFIG("config","108", "3","非结构化"),

    //数据库文件
    MDF("mdf","109", "1","结构化"),
    LDF("ldf","110", "1","结构化"),
    FRM("frm","111", "1","结构化"),
    DB("db","112", "1","结构化"),
    SQLITE("sqlite","113", "1","结构化"),
    MDB("mdb","114", "1","结构化"),
    ORC("orc","143", "1","结构化"),
    AVRO("avro","144", "1","结构化"),


    //虚拟化文件
    VHD("vhd","115", "1","结构化"),
    VHDX("vhdx","116", "1","结构化"),
    VMDK("vmdk","117", "1","结构化"),
    VDI("vdi","118", "1","结构化"),
    VHDL("vhdl","119", "1","结构化"),
    DOCKFILE ("dockerfile","120", "3","非结构化"),

    //设计创作
    DRAWIO("drawio","121", "3","非结构化"),
    AEP ("aep","122", "3","非结构化"),
    PRPROJ ("prproj","123", "3","非结构化"),
    PRX ("prx","124", "3","非结构化"),
    BLEND ("blend","125", "3","非结构化"),
    MAX("max","126", "3","非结构化"),
    MB ("mb","127", "3","非结构化"),

    //工程制图
    SCH ("sch","128", "3","非结构化"),
    PCB ("pcb","129", "3","非结构化"),

    //科研数据
    SAV ("sav","130", "3","非结构化"),
    MAT ("mat","131", "3","非结构化"),
    SHP ("shp","132", "3","非结构化"),
    KML ("kml","133", "3","非结构化"),

    //版本控制文件
    GIT_IGNORE("gitignore","135", "3","非结构化"),
    GIT_ATTRIBUTES("gitattributes","136", "3","非结构化"),
    SVN("svn","137", "3","非结构化"),

    //日志文件
    LOG("log","6", "2","半结构化"),
    OUT ("out","138", "2","半结构化"),

    //备份文件
    BAK("bak","139", "2","半结构化"),
    TMP("tmp","140", "2","半结构化"),
    OLD ("old","141", "2","半结构化"),
    BACKUP ("backup","142", "2","半结构化"),



    OTHER("other","1000", "3","非结构化");

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
