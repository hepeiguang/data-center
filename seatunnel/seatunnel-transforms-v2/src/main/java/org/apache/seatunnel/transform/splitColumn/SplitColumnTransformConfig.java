package org.apache.seatunnel.transform.splitColumn;

import lombok.Getter;
import lombok.Setter;
import org.apache.seatunnel.api.configuration.Option;
import org.apache.seatunnel.api.configuration.Options;
import org.apache.seatunnel.api.configuration.ReadonlyConfig;

import java.io.Serializable;

/**
 * @Author： 王涛
 * @DATE： 2025/5/13 17:02
 */
@Getter
@Setter
public class SplitColumnTransformConfig  implements Serializable {


    //分隔符
    public static final Option<String> KEY_SEPARATOR =
            Options.key("separator")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The separator to split the field");

    //要拆分的字段
    public static final Option<String> KEY_SPLIT_FIELD =
            Options.key("splitField")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The field to be split");

    //是否使用正则表达式 1 = 使用；2 = 不使用
    public static final Option<Integer> KEY_REGEX =
            Options.key("regex")
                    .intType()
                    .noDefaultValue()
                    .withDescription("The field to be regex");
    //新字段名
    public static final Option<String> KEY_NEW_FIELD =
            Options.key("newField")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The field to be newField");

    private String separator;
    private String splitField;
    private Integer regex;
    private String  newField;

    public static SplitColumnTransformConfig of(ReadonlyConfig config) {
        SplitColumnTransformConfig splitColumnTransformConfig = new SplitColumnTransformConfig();
        splitColumnTransformConfig.setSeparator(config.get(KEY_SEPARATOR));
        splitColumnTransformConfig.setSplitField(config.get(KEY_SPLIT_FIELD));
        splitColumnTransformConfig.setRegex(config.get(KEY_REGEX));
        splitColumnTransformConfig.setNewField(config.get(KEY_NEW_FIELD));
        return splitColumnTransformConfig;
    }
}
