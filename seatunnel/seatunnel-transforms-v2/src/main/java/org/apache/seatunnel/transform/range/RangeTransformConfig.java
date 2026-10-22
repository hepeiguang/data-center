package org.apache.seatunnel.transform.range;

import org.apache.seatunnel.api.configuration.Option;
import org.apache.seatunnel.api.configuration.Options;
import org.apache.seatunnel.api.configuration.ReadonlyConfig;

import lombok.Getter;
import lombok.Setter;
import org.apache.seatunnel.shade.com.fasterxml.jackson.annotation.JsonAlias;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class RangeTransformConfig implements Serializable {
    public static final Option<String> KEY_RANGE_TYPE =
            Options.key("range_type")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The type of range comparison (greater_than_or_equal or less_than_or_equal)");

    public static final Option<String> KEY_RANGE_VALUE_DEFAULT =
            Options.key("range_value_default")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Default value when no range matches");

    public static final Option<String> KEY_RANGE_FIELD =
            Options.key("range_field")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("range_field cannot be empty");

    // range_to_field
    public static final Option<String> KEY_RANGE_TO_FIELD =
            Options.key("range_to_field")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("range_to_field cannot be empty");

    // range_to_field_type
    public static final Option<String> KEY_RANGE_TO_FIELD_TYPE =
            Options.key("range_to_field_type")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("range_to_field_type cannot be empty");

    public static final Option<List<RangeMapping>> KEY_RANGE_MAPPINGS =
            Options.key("range_mappings")
                    .listType(RangeMapping.class)
                    .noDefaultValue()
                    .withDescription("List of range mappings");

    private String rangeType;
    private String rangeValueDefault;
    private String rangeField;
    private String rangeToField;
    private String rangeToFieldType;
    private List<RangeMapping> rangeMappings;

    public static RangeTransformConfig of(ReadonlyConfig config) {
        RangeTransformConfig rangeTransformConfig = new RangeTransformConfig();
        rangeTransformConfig.setRangeType(config.get(KEY_RANGE_TYPE));
        rangeTransformConfig.setRangeValueDefault(config.get(KEY_RANGE_VALUE_DEFAULT));
        rangeTransformConfig.setRangeField(config.get(KEY_RANGE_FIELD));
        rangeTransformConfig.setRangeToField(config.get(KEY_RANGE_TO_FIELD));
        rangeTransformConfig.setRangeToFieldType(config.get(KEY_RANGE_TO_FIELD_TYPE));
        rangeTransformConfig.setRangeMappings(config.get(KEY_RANGE_MAPPINGS));
        return rangeTransformConfig;
    }
}