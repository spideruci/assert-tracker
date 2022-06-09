import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.FieldDictionary;
import com.thoughtworks.xstream.converters.reflection.SunUnsafeReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.XStream12FieldKeySorter;

import java.io.*;


public class InstrumentationUtils {
    public static String file_base = "target";

    public static void dumpObjectUsingXml(Object o,String name,String testClassName, String testMethodName){
        //Dump xml output to xmlOutput/testClassName/testMethodName/variableName nanotime.xml

        String base = "xmloutput"+File.separator+testClassName+File.separator+testMethodName;
        try{
            File directory = new File(file_base+File.separator+base);
            directory.mkdirs();
        }catch(Exception e){
            System.err.println("failed to make directory for"+testClassName+" "+testMethodName);
        }
        try{
            XStream12FieldKeySorter sorter = new XStream12FieldKeySorter();
            XStream xstream = new XStream(new SunUnsafeReflectionProvider(new FieldDictionary(sorter)));
            xstream.setMode(XStream.ID_REFERENCES);
            String path = file_base+File.separator+base+File.separator+name+" "+System.nanoTime()+".xml";
            FileWriter fw = new FileWriter(path);
            xstream.toXML(o,fw);
        }catch(Exception e){
            System.err.println("there is something wrong when using XStream at "+System.nanoTime()+" for "
                +base+File.separator+name+" "+System.nanoTime());
            try (FileWriter f = new FileWriter(file_base+File.separator+"Non-Serializable.txt", true);
                 BufferedWriter b = new BufferedWriter(f);
                 PrintWriter p = new PrintWriter(b);)
            {
                 p.println("Failed to serialize objects in "+testClassName+" "+testMethodName);
                 p.println("Local variables are : "+ name);
            } catch (IOException i) {
                i.printStackTrace();
            }
       }
    }

    public static void printString(String content){
        System.err.println(content+System.nanoTime());
    }

    public static void dumpAssertionCount(Integer value){
        try (FileWriter f = new FileWriter("target/assertions.txt", true);
             BufferedWriter b = new BufferedWriter(f);
             PrintWriter p = new PrintWriter(b);)
        {
            p.println(value.toString());
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    public static void dumpAssertionStr(String value){
        try (FileWriter f = new FileWriter("target/assertions.txt", true);
             BufferedWriter b = new BufferedWriter(f);
             PrintWriter p = new PrintWriter(b);)
        {
            p.println(value);
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

}



