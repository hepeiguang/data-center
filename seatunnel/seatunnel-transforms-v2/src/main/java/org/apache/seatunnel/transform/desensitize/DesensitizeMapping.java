package org.apache.seatunnel.transform.desensitize;

import lombok.Data;
import org.apache.seatunnel.shade.com.fasterxml.jackson.annotation.JsonAlias;

import java.io.Serializable;

@Data
public class DesensitizeMapping implements Serializable {
    @JsonAlias("desensitize_field")
    private String desensitizeField;

    @JsonAlias("desensitize_type")
    private String desensitizeType;

}