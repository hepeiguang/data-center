package org.apache.dolphinscheduler.api.service;



import org.apache.dolphinscheduler.dao.entity.FunctionInfoCfg;
import java.util.Map;

/**
 * (FunctionInfoCfg)表服务接口
 *
 * @author makejava
 * @since 2025-04-27 18:57:40
 */
public interface FunctionInfoCfgService {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    FunctionInfoCfg queryById(Integer id);

    /**
     * 分页查询
     *
     * @param functionInfoCfg 筛选条件
     * @return 查询结果
     */
    Map<String, Object> queryByInfo(FunctionInfoCfg functionInfoCfg);


    /**
     * 通过code查询单条数据
     *
     * @param code
     * @return 实例对象
     */
    FunctionInfoCfg queryByCode(String code);

}
