

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ZipFileManager {
    /* Define ZIP File System Properies in HashMap */
    public static void main(String[] args) throws Exception {
        Map<String, String> zip_properties = new HashMap<>();
        /* We want to create a non-existing ZIP File, so we set this to True*/
        zip_properties.put("create", "true");
        /* Specify the encoding as UTF -8 */
        zip_properties.put("encoding", "UTF-8");

        //specify the location of zip file to be created
        String currentPath = System.getProperty("user.dir");
        Pattern pattern = Pattern.compile(".*:");
        Matcher matcher = pattern.matcher(currentPath);
        String zipPath = "";
        if(matcher.find()){
            //the path looks like this       jar:file:/Users/勤奋的黑痴/OneDrive/Java/test
            zipPath ="jar:file:"+currentPath.substring(matcher.end()).replace("\\","/")
                    +"/"+"xmlOutput.zip";
        }
        deleteFile("xmlOutput.zip");
        URI zip_loc = URI.create(zipPath);

        try (FileSystem zipfs = FileSystems.newFileSystem(zip_loc, zip_properties)) {
            while(true){
                File file = new File("xmlOutput");
                List<Path> xmlFiles = null;
                try (Stream<Path> fileStream = Files.walk(file.toPath())) {
                    xmlFiles = fileStream.filter(Files::isRegularFile)
                            .filter(p -> p.toFile().getName().endsWith(".xml"))
                            .collect(Collectors.toList());
                }

                for (Path xmlFile : xmlFiles) {
                    Path filePathInZip =zipfs.getPath(xmlFile.toString().substring(9).replace("\\"," "));//.replace("\\","/"));
                    Files.copy(xmlFile,filePathInZip);
                }
                Thread.sleep(3000);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


//        URI zip_loc = URI.create("jar:file:/"+ "Users/勤奋的黑痴/OneDrive/Java/test/niubi.zip");


    }
    public static void deleteFile(String path) throws Exception {
        File aFile = new File(path);
        if(aFile.exists()){
            aFile.delete();
        }else{
            throw new Exception(path+ " file not exists");
        }

    }

}
