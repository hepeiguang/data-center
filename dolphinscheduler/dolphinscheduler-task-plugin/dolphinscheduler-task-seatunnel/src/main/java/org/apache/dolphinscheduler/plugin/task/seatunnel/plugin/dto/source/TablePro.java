package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TablePro{
    @JsonProperty("table_path")
    private String tablePath;
}