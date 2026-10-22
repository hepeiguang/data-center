package org.apache.seatunnel.transform.sequence;

import org.apache.seatunnel.api.configuration.Option;
import org.apache.seatunnel.api.configuration.Options;
import org.apache.seatunnel.api.configuration.ReadonlyConfig;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class SequenceTransformConfig implements Serializable {
    public static final Option<String> KEY_FIELD_NAME =
            Options.key("field_name").stringType().noDefaultValue().withDescription("需要自增的字段名");

    public static final Option<Long> KEY_START_VALUE =
            Options.key("start_value").longType().defaultValue(1L).withDescription("起始值");

    public static final Option<Long> KEY_ADD_VALUE =
            Options.key("add_value").longType().defaultValue(1L).withDescription("每次增加的值");

    public static final Option<Long> KEY_MAX_VALUE =
            Options.key("max_value").longType().noDefaultValue().withDescription("最大值，可选");

    private String fieldName;
    private long startValue;
    private long addValue;
    private Long maxValue;

    public static SequenceTransformConfig of(ReadonlyConfig config) {
        SequenceTransformConfig result = new SequenceTransformConfig();
        result.setFieldName(config.get(KEY_FIELD_NAME));
        result.setStartValue(config.get(KEY_START_VALUE));
        result.setAddValue(config.get(KEY_ADD_VALUE));
        result.setMaxValue(config.get(KEY_MAX_VALUE));
        return result;
    }
}
