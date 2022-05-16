import com.thoughtworks.xstream.XStream;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class InstrumentationUtils {

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
            XStream xstream = new XStream();
            String path = base+File.separator+name+" "+System.nanoTime()+".xml";
            FileWriter fw = new FileWriter(path);
            new XStream().toXML(o,fw);
        }catch(Exception e){
            System.err.println("there is something wrong when using XStream at "+System.nanoTime());
        }



    }

    public static void printString(String content){
        System.err.println(content+System.nanoTime());
    }



}



