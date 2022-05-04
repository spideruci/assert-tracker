package org.spideruci.asserttracker;

import com.thoughtworks.xstream.XStream;

import java.io.FileWriter;

public class DumpObject {

    public static void dumpObjectUsingXml(Object o){
        //customize our dump results here!

        try{
            new XStream().toXML(o,new FileWriter(System.nanoTime()+".xml"));
        }catch(Exception e){
            System.out.println("there is something wrong when using XStream");
        }

    }
}
