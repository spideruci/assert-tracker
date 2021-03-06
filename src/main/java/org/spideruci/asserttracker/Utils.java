package org.spideruci.asserttracker;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static int calculateParaNum(String methodDesc){

        //There is something wrong here, but it doesn't matter for now.
//        String temp = "(ILjava/lang/Boolean;)V";
        Pattern pattern1 = Pattern.compile("\\(\\)");
        Matcher matcher1 = pattern1.matcher(methodDesc);

        if(matcher1.find()){
            return 0;
        }
        return 1;
    }

    public static Boolean isAssertionStatement(String methodName) {
        final Set<String> assertionNames = new HashSet<String>(Arrays.asList("assertArrayEquals", "assertEquals", "assertFalse", "assertNotNull",
                "assertNotSame", "assertNull", "assertSame", "assertThat", "assertTrue", "assertThrows","assertNotEquals",
                "assertIterableEquals","assertLinesMatch","assertTimeout","assertTimeoutpreemptively","fail"));
        if (assertionNames.contains(methodName)  || methodName.startsWith("assertThat")){
            return true;
        }
        return false;
    }

    public static void appendLogAt(String filename, String[] lines){
        try (FileWriter f = new FileWriter(filename, true);
             BufferedWriter b = new BufferedWriter(f);
             PrintWriter p = new PrintWriter(b);)
        {
            for(String line: lines){
                p.println(line);
            }
        } catch (IOException i) {
            i.printStackTrace();
        }
    }



}
