package org.spideruci.asserttracker;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static int calculateParaNum(String methodDesc){
//        String temp = "(ILjava/lang/Boolean;)V";
        Pattern pattern1 = Pattern.compile("\\(.*\\)");
        Matcher matcher1 = pattern1.matcher(methodDesc);
        String paras = "";
        if(matcher1.find()){
            if(matcher1.group(0).length()==2)
                return 0;
            paras = matcher1.group(0).substring(1,methodDesc.length()-1);
        }
        Pattern pattern2 = Pattern.compile("\\[*([ZCBSIFJD]|L.*?;)");
        Matcher matcher2 = pattern2.matcher(paras);
        return matcher2.groupCount();
//        return (int) matcher2.results().count();
    }

    public static Boolean isAssertionStatement(String methodName) {
        final String[] assertionNames = new String[]{"assertArrayEquals", "assertEquals", "assertFalse", "assertNotNull", "assertNotSame", "assertNull", "assertSame", "assertThat", "assertTrue", "assertThrows","assertNotEquals"};
        for(String s : assertionNames){
            if (s.equalsIgnoreCase(methodName)) {
                return true;
            }
        }
        return false;
    }

}
