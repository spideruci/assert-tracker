package org.spideruci.asserttracker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;
import java.io.File;

public class XStreamBL {
    public static Set<String> testsBlacklist= new HashSet<String>();

    //initialize XStream instrumentation blacklist
    static{
        String file = "XStreamBlacklist.txt";
        if(new File(file).exists()){
            try{
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                String curLine;
                while ((curLine = bufferedReader.readLine()) != null){
                    testsBlacklist.add(curLine.trim());
                }
                bufferedReader.close();
            }catch(Exception e){
            }
        }
    }
}
