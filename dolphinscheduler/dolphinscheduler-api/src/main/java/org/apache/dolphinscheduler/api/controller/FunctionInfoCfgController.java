package org.apache.dolphinscheduler.api.controller;

import org.apache.dolphinscheduler.api.service.FunctionInfoCfgService;
import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.dao.entity.FunctionInfoCfg;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * (FunctionInfoCfg)表控制层
 *
 * @author makejava
 * @since 2025-04-27 18:57:18
 */
@RestController
@RequestMapping("functionInfoCfg")
public class FunctionInfoCfgController extends BaseController {
    /**
     * 服务对象
     */
    @Resource
    private FunctionInfoCfgService functionInfoCfgService;

    /**
     * 分页查询
     *
     * @param functionInfoCfg 筛选条件
     * @return 查询结果
     */
    @GetMapping(value = "/queryInfo")
    public Result queryByInfo(FunctionInfoCfg functionInfoCfg) {
        Map<String, Object> result =  functionInfoCfgService.queryByInfo(functionInfoCfg);
        return returnDataList(result);
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping(value = "/queryById")
    public ResponseEntity<FunctionInfoCfg> queryById( @RequestParam("id") Integer id) {
        return ResponseEntity.ok(functionInfoCfgService.queryById(id));
    }

    /**
     * 通过code查询单条数据
     *
     * @param code
     * @return 单条数据
     */
    @GetMapping(value = "/queryByCode")
    public ResponseEntity<FunctionInfoCfg> queryByCode( @RequestParam("code") String code) {
        return ResponseEntity.ok(functionInfoCfgService.queryByCode(code));
    }

}

