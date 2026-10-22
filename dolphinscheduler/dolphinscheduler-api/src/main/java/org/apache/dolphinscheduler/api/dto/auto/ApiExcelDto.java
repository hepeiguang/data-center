package org.apache.dolphinscheduler.api.dto.auto;

import lombok.Data;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunneTransformDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;

import java.util.ArrayList;
import java.util.List;

@Data
public class ApiExcelDto {
    private String seq;            // 序号
    private String parentModule;   // 父模块
    private String module;         // 模块
    private String function;       // 功能
    private String apiName;           // 名称
    private String url;            // 请求地址
    private String method;         // 请求方式
    private String json_contentField;   // 内容获取
    private String params;         // 请求参数
    private String paginationParams; // 分页参数
    private String tableName;      // 表名称
    private String primaryKey;     // 主键列
    private String result;         // 返回结果
    private String depend;         // 依赖项
    private String otherParams;    // 添加到工作流节点中的其他参数
    private String schema;         // 生成的schema

    private List<SeatunnelTaskNodeVO> transforms = new ArrayList<>();
    public boolean hasDepend() {
        return depend != null && !depend.isEmpty();
    }

    public void addTransforms(SeatunnelTaskNodeVO transform) {
        if (transforms == null) {
            transforms = new ArrayList<>();
        }
        transforms.add(transform);
    }
}