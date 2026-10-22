package org.apache.dolphinscheduler.api.utils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.util.Arrays;
import java.util.List;

public class PinyinUtil {

    /**
     * 获取中文拼音首字母（小写），保留汉字、英文字母和数字
     * @param input 输入字符串
     * @return 拼音首字母（小写），包含字母和数字
     */
    public static String getFirstLetter(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);

        char[] chars = input.toCharArray();
        for (char c : chars) {
            // 判断是否为中文字符
            if (Character.toString(c).matches("[\\u4E00-\\u9FA5]")) {
                try {
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, format);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        result.append(pinyinArray[0].charAt(0)); // 取拼音首字母
                    }
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    e.printStackTrace();
                }
            }
            // 判断是否为英文字母
            else if (Character.isLetter(c)) {
                result.append(Character.toLowerCase(c)); // 英文转小写
            }
            // 判断是否为数字
            else if (Character.isDigit(c)) {
                result.append(c); // 数字直接保留
            }
            // 其他字符（空格、标点等）跳过
        }
        return result.toString();
    }

    /**
     * 获取中文全拼音（小写），保留汉字、英文字母和数字
     * @param input 输入字符串
     * @return 全拼音（小写），包含字母和数字
     */
    public static String getFullPinyin(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);

        char[] chars = input.toCharArray();
        for (char c : chars) {
            // 判断是否为中文字符
            if (Character.toString(c).matches("[\\u4E00-\\u9FA5]")) {
                try {
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, format);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        result.append(pinyinArray[0]); // 全拼音
                    }
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    e.printStackTrace();
                }
            }
            // 判断是否为英文字母
            else if (Character.isLetter(c)) {
                result.append(Character.toLowerCase(c)); // 英文转小写
            }
            // 判断是否为数字
            else if (Character.isDigit(c)) {
                result.append(c); // 数字直接保留
            }
            // 其他字符（空格、标点等）跳过
        }
        return result.toString();
    }

    /**
     * 根据分割值对分词列表进行混合拼音转换，用下划线分隔
     * 前 splitIndex 个词使用全拼音，后面的词使用拼音首字母
     */
    public static String getMixedPinyin(List<String> words, int splitIndex) {
        if (words == null || words.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);

            if (i > 0) {
                result.append("_");
            }

            if (i < splitIndex && splitIndex > 0) {
                result.append(getFullPinyin(word));
            } else {
                result.append(getFirstLetter(word));
            }
        }

        return result.toString();
    }

    /**
     * 全拼音转换，用下划线分隔
     */
    public static String getFullPinyinWithUnderscore(List<String> words) {
        if (words == null || words.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            if (i > 0) {
                result.append("_");
            }
            result.append(getFullPinyin(words.get(i)));
        }
        return result.toString();
    }

    /**
     * 首字母转换，用下划线分隔
     */
    public static String getFirstLetterWithUnderscore(List<String> words) {
        if (words == null || words.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            if (i > 0) {
                result.append("_");
            }
            result.append(getFirstLetter(words.get(i)));
        }
        return result.toString();
    }

    /**
     * 获取合适的表名拼音（根据总长度限制智能选择）
     * 规则：如果全拼音不超过限制，用全拼音；否则逐渐将后面的词改为首字母
     */
    public static String getSmartTableNamePinyin(List<String> words, int maxLength) {
        if (words == null || words.isEmpty()) {
            return "";
        }

        // 1. 如果全拼音不超过限制，直接用全拼音
        String fullPinyin = getFullPinyinWithUnderscore(words);
        if (fullPinyin.length() <= maxLength) {
            return fullPinyin;
        }

        // 2. 需要混合：从后往前逐渐将词改为首字母
        for (int splitIndex = words.size() - 1; splitIndex >= 0; splitIndex--) {
            String mixed = getMixedPinyin(words, splitIndex);
            if (mixed.length() <= maxLength) {
                return mixed;
            }
        }

        // 3. 如果还不行，尝试构建缩短版本（不截断单词）
        return buildShortenedVersion(words, maxLength);
    }

    /**
     * 构建缩短版本，确保不在单词中间截断
     */
    private static String buildShortenedVersion(List<String> words, int maxLength) {
        if (words == null || words.isEmpty()) {
            return "";
        }

        // 尝试只用前几个词的首字母
        for (int wordCount = words.size(); wordCount >= 1; wordCount--) {
            List<String> subWords = words.subList(0, wordCount);
            String firstLetter = getFirstLetterWithUnderscore(subWords);
            if (firstLetter.length() <= maxLength) {
                return firstLetter;
            }
        }

        // 如果连一个词的首字母都放不下，返回第一个词的首字母（最多maxLength长度）
        String firstWordLetter = getFirstLetter(words.get(0));
        return firstWordLetter.substring(0, Math.min(firstWordLetter.length(), maxLength));
    }


//    public static void main(String[] args) {
//        // 测试用例
//        List<String> test1 = Arrays.asList("成本", "分摊");
//        List<String> test2 = Arrays.asList("设备", "使用", "登记", "结算单");
//        List<String> test3 = Arrays.asList("安全", "检查", "记录");
//        List<String> test4 = Arrays.asList("项目", "成本", "管理", "系统", "报表");
//
//        System.out.println("=== 测试基本功能 ===");
//        System.out.println();
//
//        System.out.println("测试1 - 成本分摊:");
//        System.out.println("分词: " + test1);
//        System.out.println("全拼音: " + PinyinUtil.getFullPinyinWithUnderscore(test1));
//        System.out.println("首字母: " + PinyinUtil.getFirstLetterWithUnderscore(test1));
//        System.out.println("混合(split=1): " + PinyinUtil.getMixedPinyin(test1, 1));
//        System.out.println();
//
//        System.out.println("测试2 - 设备使用登记结算单:");
//        System.out.println("分词: " + test2);
//        System.out.println("全拼音: " + PinyinUtil.getFullPinyinWithUnderscore(test2));
//        System.out.println("首字母: " + PinyinUtil.getFirstLetterWithUnderscore(test2));
//        System.out.println("混合(split=2): " + PinyinUtil.getMixedPinyin(test2, 2));
//        System.out.println();
//
//        System.out.println("=== 测试智能表名生成 ===");
//        System.out.println();
//
//        System.out.println("测试 - 成本分摊 (不同长度限制):");
//        System.out.println("分词: " + test1);
//        System.out.println("maxLength=5: '" + PinyinUtil.getSmartTableNamePinyin(test1, 5) + "'");
//        System.out.println("maxLength=10: '" + PinyinUtil.getSmartTableNamePinyin(test1, 10) + "'");
//        System.out.println("maxLength=15: '" + PinyinUtil.getSmartTableNamePinyin(test1, 15) + "'");
//        System.out.println("maxLength=20: '" + PinyinUtil.getSmartTableNamePinyin(test1, 20) + "'");
//        System.out.println();
//
//        System.out.println("测试 - 设备使用登记结算单 (不同长度限制):");
//        System.out.println("分词: " + test2);
//        System.out.println("全拼音长度: " + PinyinUtil.getFullPinyinWithUnderscore(test2).length());
//        System.out.println("maxLength=5: '" + PinyinUtil.getSmartTableNamePinyin(test2, 5) + "'");
//        System.out.println("maxLength=10: '" + PinyinUtil.getSmartTableNamePinyin(test2, 10) + "'");
//        System.out.println("maxLength=15: '" + PinyinUtil.getSmartTableNamePinyin(test2, 15) + "'");
//        System.out.println("maxLength=20: '" + PinyinUtil.getSmartTableNamePinyin(test2, 20) + "'");
//        System.out.println("maxLength=25: '" + PinyinUtil.getSmartTableNamePinyin(test2, 25) + "'");
//        System.out.println("maxLength=30: '" + PinyinUtil.getSmartTableNamePinyin(test2, 30) + "'");
//        System.out.println();
//
//        System.out.println("测试 - 项目成本管理系统报表 (不同长度限制):");
//        System.out.println("分词: " + test4);
//        System.out.println("全拼音长度: " + PinyinUtil.getFullPinyinWithUnderscore(test4).length());
//        System.out.println("maxLength=10: '" + PinyinUtil.getSmartTableNamePinyin(test4, 10) + "'");
//        System.out.println("maxLength=15: '" + PinyinUtil.getSmartTableNamePinyin(test4, 15) + "'");
//        System.out.println("maxLength=20: '" + PinyinUtil.getSmartTableNamePinyin(test4, 20) + "'");
//        System.out.println("maxLength=25: '" + PinyinUtil.getSmartTableNamePinyin(test4, 25) + "'");
//        System.out.println();
//
//        System.out.println("=== 实际表名生成示例 ===");
//        System.out.println();
//    }
}