package org.apache.dolphinscheduler.api.utils;

import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.StandardTokenizer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分词工具类
 */
public class WordSegmentUtils {

    private WordSegmentUtils() {
        // 工具类，防止实例化
    }

    /**
     * 分词：移除开头的数字版本号，然后分词
     */
    public static List<String> segment(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String cleaned = text.trim().replaceAll("^\\d+(\\.\\d+)*\\.?\\s*", "");
        List<Term> terms = StandardTokenizer.segment(cleaned);
        List<String> result = new ArrayList<>();
        for (Term term : terms) {
            String word = term.word.trim();
            if (!word.isEmpty() && !word.matches("\\d+(\\.\\d+)*")) {
                result.add(word);
            }
        }
        return result;
    }

    /**
     * 测试
     */
//    public static void main(String[] args) {
//        String[] testCases = {
//                "2.5.期初管理",
//                "8.1.专业分包合同",
//                "出入库流水",
//                "9.6.月报",
//                "2.6.项目成本",
//                "档案信息",
//                "营销物品进销存报表",
//                "已报销发票",
//                "索赔记录",
//                "物料档案",
//                "11.4.劳务薪资发放",
//                "3.3.机械预算",
//                "4.1.劳务分包合同",
//                "1.3.中标登记",
//                "其他入库",
//                "付款单",
//                "11.11.成本分摊",
//                "11.1.开票申请",
//                "4.4.劳务点工/结算单",
//                "11.3.其他收款",
//                "用车申请",
//                "6.2.领料出库",
//                "计量单位",
//                "12.1.签约公司",
//                "7.3.机械租赁合同",
//                "9.1.进度计划",
//                "5.1.采购申请",
//                "10.8.安全事故记录",
//                "4.5.自有劳务薪资统计",
//                "1.4.保证金退回"
//        };
//
//        for (String text : testCases) {
//            System.out.println("原文: " + text);
//            System.out.println("分词: " + segment(text));
//            System.out.println();
//        }
//    }
}