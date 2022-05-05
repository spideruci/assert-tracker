package org.spideruci.asserttracker;

import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.io.FileWriter;

public class DumpObject {

    public static void dumpObjectUsingXml(Object o,String name,String testClassName, String testMethodName){
        //Dump xml output to xmlOutput/testClassName/testMethodName/variableName nanotime.xml


        String base = "xmloutput"+File.separator+testClassName+File.separator+testMethodName;
        try{
            File directory = new File(base);
            directory.mkdirs();
        }catch(Exception e){
            System.err.println("failed to make directory for"+testClassName+" "+testMethodName);
        }
        try{
            new XStream().toXML(o,new FileWriter(base+File.separator+name+" "+System.nanoTime()+".xml"));
        }catch(Exception e){
            System.err.println("there is something wrong when using XStream at "+System.nanoTime());
        }
    }
}
