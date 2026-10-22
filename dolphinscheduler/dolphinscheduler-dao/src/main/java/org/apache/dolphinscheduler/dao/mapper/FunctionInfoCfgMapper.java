package org.apache.dolphinscheduler.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.dolphinscheduler.dao.entity.FunctionInfoCfg;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * (FunctionInfoCfg)表数据库访问层
 *
 * @author makejava
 * @since 2025-04-27 18:57:24
 */
public interface FunctionInfoCfgMapper extends BaseMapper<FunctionInfoCfg>  {

    /**
     * 通过code查询单条数据
     *
     * @param code
     * @return 实例对象
     */
    FunctionInfoCfg queryByCode(@Param("code") String code);

    FunctionInfoCfg queryByUdfsId(@Param("udfsId") Integer udfsId);


    FunctionInfoCfg queryFunctionInfoCfgNumByById(@Param("id") Integer id);
    /**
     * 查询指定行数据
     *
     * @param functionInfoCfg 查询条件
     * @return 对象列表
     */
    List<FunctionInfoCfg> queryAllByLimit(FunctionInfoCfg functionInfoCfg);

    Integer insertFunctionInfoCfg(FunctionInfoCfg functionInfoCfg);

    Integer queryFunctionInfoCfgNumByTypeName( @Param("typeName") String typeName );

    Integer fixFunctionInfoCfg(FunctionInfoCfg functionInfoCfg);

    Integer deleteFunctionInfoCfgByUdfsId(@Param("udfsId") Integer udfsId);


}

