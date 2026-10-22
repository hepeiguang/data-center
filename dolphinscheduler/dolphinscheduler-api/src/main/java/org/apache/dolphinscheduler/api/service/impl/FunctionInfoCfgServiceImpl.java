
package org.apache.dolphinscheduler.api.service.impl;
import com.aliyun.oss.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.service.FunctionInfoCfgService;
import org.apache.dolphinscheduler.common.constants.Constants;
import org.apache.dolphinscheduler.dao.entity.CategoryGroup;
import org.apache.dolphinscheduler.dao.entity.FunctionInfoCfg;
import org.apache.dolphinscheduler.dao.mapper.FunctionInfoCfgMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * (FunctionInfoCfg)表服务实现类
 *
 * @author makejava
 * @since 2025-04-27 18:57:45
 */
@Service
@Slf4j
public class FunctionInfoCfgServiceImpl extends BaseServiceImpl implements FunctionInfoCfgService {
    @Resource
    private FunctionInfoCfgMapper functionInfoCfgMapper;

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public FunctionInfoCfg queryById(Integer id) {
        return this.functionInfoCfgMapper.selectById(id);
    }

    /**
     * 分页查询
     *
     * @param functionInfoCfg 筛选条件
     * @return 查询结果
     */
    @Override
    public Map<String,Object> queryByInfo(FunctionInfoCfg functionInfoCfg) {
        Map<String,Object> result  =new HashMap<>();
        List<FunctionInfoCfg> lts = this.functionInfoCfgMapper.queryAllByLimit(functionInfoCfg);
        List<FunctionInfoCfg> updatedList = lts.stream()
                .map(item -> {
                    // 创建新对象或修改现有对象
                    FunctionInfoCfg newItem = new FunctionInfoCfg();
                    // 复制原有属性
                    newItem.setId(item.getId());
                    newItem.setFunctionName(item.getFunctionName());
                    newItem.setFunctionCode(item.getFunctionCode());
                    newItem.setTypeName(item.getTypeName());
                    // 添加新字段
                    newItem.setLabel(item.getFunctionName());
                    newItem.setValue(item.getFunctionCode());
                    if(StringUtils.isNullOrEmpty(item.getFunctionAttribute())||"[]".equals(item.getFunctionAttribute().replaceAll("\\s+", ""))){
                        newItem.setHasParam(false);
                    }else{
                        newItem.setHasParam(true);
                    }
                    return newItem;
                })
                .collect(Collectors.toList());
        List<CategoryGroup> oneResult = updatedList.stream()
                .collect(Collectors.groupingBy(FunctionInfoCfg::getTypeName))
                .entrySet().stream()
                .map(entry -> new CategoryGroup(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        List<CategoryGroup> results = oneResult.stream()
                .map(item -> {
                    // 创建新对象或修改现有对象
                    CategoryGroup newItem = new CategoryGroup();
                    newItem.setLabel(item.getLabel());
                    newItem.setValue(item.getLabel());
                    newItem.setchildren(item.getchildren());
                    return newItem;
                })
                .collect(Collectors.toList());
        if (lts.isEmpty()) {
            result.put(Constants.DATA_LIST, Collections.emptyList());
            putMsg(result, Status.SUCCESS);
            return result;
        }
        result.put(Constants.DATA_LIST,results);
        putMsg(result, Status.SUCCESS);
        return result;
    }

    @Override
    public FunctionInfoCfg queryByCode(String code){
        log.info("queryByCode code:{}",code);
        return functionInfoCfgMapper.queryByCode(code);
    }
}
