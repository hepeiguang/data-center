package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunneTransformDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.CompileLanguageEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.CompilePatternEnum;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TransFormJavaDTO extends BaseSeatunneTransformDTO {

    @JsonProperty("source_code")
    private String sourceCode;

    @JsonProperty("compile_language")
    private CompileLanguageEnum compileLanguage;

    @JsonProperty("compile_pattern")
    private CompilePatternEnum compilePattern = CompilePatternEnum.SOURCE_CODE;

    @JsonProperty("absolute_path")
    private String absolutePath;

    @JsonProperty("plugin_input")
    private String pluginInput;

    @JsonProperty("plugin_output")
    private String pluginOutput;

}
