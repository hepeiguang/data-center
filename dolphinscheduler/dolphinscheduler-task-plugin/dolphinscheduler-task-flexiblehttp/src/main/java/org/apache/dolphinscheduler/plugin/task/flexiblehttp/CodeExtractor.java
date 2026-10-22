package org.apache.dolphinscheduler.plugin.task.flexiblehttp;

import java.util.ArrayList;
import java.util.List;

public class CodeExtractor {

    public static List<String> extractCodeBlocks(String input) {
        List<String> blocks = new ArrayList<>();
        int start = -1;
        int braceCount = 0;
        char[] chars = input.toCharArray();
        for(int i=0; i<chars.length; i++) {
            if(i < chars.length - 1 && chars[i] == '#' && chars[i+1] == '{') {
                if(start == -1) {
                    start = i + 2;
                    braceCount = 1;
                    i++;
                }
            }else if(start != -1) {
                if(chars[i] == '{') {
                    braceCount++;
                }else if(chars[i] == '}') {
                    braceCount--;
                    if(braceCount == 0){
                        blocks.add(input.substring(start, i));
                        start = -1;
                    }
                }
            }
        }

        return blocks;
    }

    public static void main(String[] args) {
        String input = "#{  \n" +
                "def jsonResponse = this.binding.getVariable('sjdd.response')\n" +
                "    def data = json.parse(jsonResponse)\n" +
                "    def newList = []\n" +
                "    data.each {\n" +
                "       outerObj -> outerObj.each { key, innerArray ->\n" +
                "          if(innerArray instanceof List) {\n" +
                "            newList.add(innerArray[0])\n" +
                "         }\n" +
                "       }\n" +
                "    }\n" +
                "    json.encode(newList)\n" +
                "}";
        List<String> stringList = extractCodeBlocks(input);
        System.out.println(stringList);
    }

}
