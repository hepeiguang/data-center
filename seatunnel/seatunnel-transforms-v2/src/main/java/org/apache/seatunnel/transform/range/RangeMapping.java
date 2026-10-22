package org.apache.seatunnel.transform.range;

import org.apache.seatunnel.shade.com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Data;


import java.io.Serializable;

@Data
public class RangeMapping implements Serializable {
    @JsonAlias("range_start")
    private double rangeStart;

    @JsonAlias("range_end")
    private double rangeEnd;

    @JsonAlias("range_value")
    private String rangeValue;

}