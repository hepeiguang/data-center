package org.apache.dolphinscheduler.api.controller;

import org.apache.dolphinscheduler.api.service.DictInfoCfgService;
import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.dao.entity.DictInfoCfg;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * (DictInfoCfg)表控制层
 *
 * @author makejava
 * @since 2025-04-27 18:57:18
 */
@RestController
@RequestMapping("dictInfoCfg")
public class DictInfoCfgController extends BaseController {
    /**
     * 服务对象
     */
    @Resource
    private DictInfoCfgService dictInfoCfgService;

    /**
     * 分页查询
     *
     * @param dictInfoCfg 筛选条件
     * @return 查询结果
     */
    @GetMapping(value = "/queryInfo")
    public Result queryByInfo(HttpServletRequest request,DictInfoCfg dictInfoCfg) {
        Map<String, Object> result =  dictInfoCfgService.queryByInfo(dictInfoCfg,null);
        return returnDataList(result);
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping(value = "/queryById")
    public ResponseEntity<DictInfoCfg> queryById( @RequestParam("id") Integer id) {
        return ResponseEntity.ok(dictInfoCfgService.queryById(id));
    }

    /**
     * 通过code查询单条数据
     *
     * @param code
     * @return 单条数据
     */
    @GetMapping(value = "/queryByCode")
    public ResponseEntity<DictInfoCfg> queryByCode( @RequestParam("code") String code) {
        return ResponseEntity.ok(dictInfoCfgService.queryByCode(code));
    }

}

