package org.apache.dolphinscheduler.dao.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author : wangyongfeng
 * @version V1.0
 * @Project: data-center-platform
 * @Package org.apache.dolphinscheduler.dao.entity
 * @Description: TODO
 * @date Date : 2025年04月28日 13:26
 */
@Data
@NoArgsConstructor
public class CategoryGroup {
    private String label;
    private String value;
    private List<FunctionInfoCfg> children;

    // 构造方法
    public CategoryGroup(String label, List<FunctionInfoCfg> children) {
        this.label = label;
        this.children = children;
    }


    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }


    public List<FunctionInfoCfg> getchildren() {
        return children;
    }

    public void setchildren(List<FunctionInfoCfg> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return "CategoryGroup{" +
                "label='" + label + '\'' +
                ", value='" +  value + '\'' +
                ", children=" + children +
                '}';
    }

}
