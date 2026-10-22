package org.apache.seatunnel.transform.dynamicdict;


import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


import java.io.Serializable;

@Data
@JsonIgnoreProperties
public class DictFieldMapping implements Serializable {
    @JsonProperty("table_name")
    private String table_name;
    @JsonProperty("column_name")
    private String column_name;

    @JsonProperty("source_field")
    private String source_field;

    @JsonProperty("dict_code")
    private String dict_code;
    @JsonProperty("dict_name")
    private String dict_name;
    @JsonProperty("dict_type")
    private String dict_type;
    @JsonProperty("min_num")
    private String min_num;
    @JsonProperty("max_num")
    private String max_num;
    @JsonProperty("time_type")
    private String time_type;
    @JsonProperty("num_accuracy")
    private Integer num_accuracy;

    public String getTable_name() {
        return table_name;
    }

    public void setTable_name(String table_name) {
        this.table_name = table_name;
    }

    public String getColumn_name() {
        return column_name;
    }

    public void setColumn_name(String column_name) {
        this.column_name = column_name;
    }

    public String getSource_field() {
        return source_field;
    }

    public void setSource_field(String source_field) {
        this.source_field = source_field;
    }

    public String getDict_code() {
        return dict_code;
    }

    public void setDict_code(String dict_code) {
        this.dict_code = dict_code;
    }

    public String getDict_name() {
        return dict_name;
    }

    public void setDict_name(String dict_name) {
        this.dict_name = dict_name;
    }

    public String getDict_type() {
        return dict_type;
    }

    public void setDict_type(String dict_type) {
        this.dict_type = dict_type;
    }

    public String getMin_num() {
        return min_num;
    }

    public void setMin_num(String min_num) {
        this.min_num = min_num;
    }

    public String getMax_num() {
        return max_num;
    }

    public void setMax_num(String max_num) {
        this.max_num = max_num;
    }

    public String getTime_type() {
        return time_type;
    }

    public void setTime_type(String time_type) {
        this.time_type = time_type;
    }

    public Integer getNum_accuracy() {
        return num_accuracy;
    }

    public void setNum_accuracy(Integer num_accuracy) {
        this.num_accuracy = num_accuracy;
    }
}
