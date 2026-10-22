package org.apache.dolphinscheduler.api.service;


import org.apache.dolphinscheduler.dao.entity.DictInfoCfg;

import java.util.List;
import java.util.Map;

/**
 * (FunctionInfoCfg)表服务接口
 *
 * @author makejava
 * @since 2025-04-27 18:57:40
 */
public interface DictInfoCfgService {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    DictInfoCfg queryById(Integer id);

    /**
     *
     * @param dictInfoCfg
     * @param token
     * @return
     */
    Map<String, Object> queryByInfo(DictInfoCfg dictInfoCfg,String token);


    /**
     * 通过code查询单条数据
     *
     * @param code
     * @return 实例对象
     */
    DictInfoCfg queryByCode(String code);

    /**
     *
     * @param id
     * @param token
     * @return
     */
    public DictInfoCfg queryById(String id,String token);


    /**
     *
     * @param dictInfoCfg
     * @param token
     * @return
     */
    List<DictInfoCfg> callApi(DictInfoCfg dictInfoCfg ,String token);


}
