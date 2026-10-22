package org.apache.dolphinscheduler.api.utils;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PinyinUtilTest {

    @Test
    public void getFirstLetter() {
        List<String> list = Arrays.asList("合同管理", "物料领用", "财务-报销付款", "营销领用", "碳运供应商管理", "员工福利", "工程证章管理", "考卷：进销存管理搭建-任慧", "采购管理", "【建筑工程项目管理-通用版2.0】", "测试版", "【建筑工程项目管理-通用版2.0】", "后勤（出差、快递、办公用品）", "固定资产管理", "库存物资管理", "数据采集应用", "车辆管理");
        for (String s : list) {
            System.out.println(s + "     " + PinyinUtil.getFirstLetter(s));
        }
    }
}