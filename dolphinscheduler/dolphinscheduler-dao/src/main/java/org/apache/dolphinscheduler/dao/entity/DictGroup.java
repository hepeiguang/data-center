package org.apache.dolphinscheduler.dao.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
public class DictGroup {
    private String label;
    private String value;
    private String apiUrl;
    private List<DictInfoCfg> children;

    // 构造方法
    public DictGroup(String label, List<DictInfoCfg> children) {
        this.label = label;
        this.children = children;
    }


    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<DictInfoCfg> getchildren() {
        return children;
    }

    public void setchildren(List<DictInfoCfg> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return "DictGroup{" +
                "label='" + label + '\'' +
                ", value='" +  value + '\'' +
                ", apiUrl='" +  apiUrl + '\'' +
                ", children=" + children +
                '}';
    }

}
