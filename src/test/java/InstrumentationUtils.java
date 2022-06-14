import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.FieldDictionary;
import com.thoughtworks.xstream.converters.reflection.SunUnsafeReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.XStream12FieldKeySorter;

import java.io.*;
import java.util.LinkedList;
import java.util.List;


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
        Object[] object_arrays = (Object[]) o;
        List<Object> object_list= new LinkedList<Object>();
        String[] all_names = name.split(" ");
        int index=0;
        List<Integer> non_s_index = new LinkedList<Integer>();
        for (Object e: object_arrays){
            try{
                new XStream().toXML(e);
                object_list.add(e);
            }catch(Exception exc) {
                non_s_index.add(index);
            }catch(Error er){
                non_s_index.add(index);
            }
            index=index+1;
        }
        Object[] serializable_array = new Object[object_list.size()];
        for(int i =0;i<object_list.size();i++){
            serializable_array[i]=object_list.get(i);
        }
        try{

            XStream12FieldKeySorter sorter = new XStream12FieldKeySorter();
            XStream xstream = new XStream(new SunUnsafeReflectionProvider(new FieldDictionary(sorter)));
            xstream.setMode(XStream.ID_REFERENCES);
            String path = file_base+File.separator+base+File.separator+name+" "+System.nanoTime()+".xml";
            FileWriter fw = new FileWriter(path);
//            xstream.toXML(o,fw);
            xstream.toXML(serializable_array,fw);
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

        for(int i:non_s_index){
            try (FileWriter f = new FileWriter(file_base+File.separator+"Non-Serializable.txt", true);
                 BufferedWriter b = new BufferedWriter(f);
                 PrintWriter p = new PrintWriter(b);)
            {
                String content = "";
                for(int ni:non_s_index ){
                    content  = content+all_names[ni]+" ";
                }
                p.println("Failed to serialize objects in "+testClassName+" "+testMethodName);
                p.println("Local variables are : "+ content);
            } catch (IOException e) {
                e.printStackTrace();
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



