import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.HashMap;
import java.util.LinkedList;
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
//        deleteFile("xmloutput.zip");
        URI zip_loc = URI.create(zipPath);
        try (FileSystem zipfs = FileSystems.newFileSystem(zip_loc, zip_properties)) {
        } catch (IOException e) {
            e.printStackTrace();
        }

        zip_properties.put("create","false");
        int count= 0;
        while(true){
            File file = new File("xmloutput");
            List<Path> xmlFiles = null;
            try (Stream<Path> fileStream = Files.walk(file.toPath())) {
                xmlFiles = fileStream.filter(Files::isRegularFile)
                        .filter(p -> p.toFile().getName().endsWith(".xml"))
                        .collect(Collectors.toList());
            }

            List<Path> deleteFiles = new LinkedList<Path>();
            try (FileSystem zipfs = FileSystems.newFileSystem(zip_loc, zip_properties)) {
                for (Path xmlFile : xmlFiles) {
                    File nextFile = new File(xmlFile.toString());
                    long currentTime = System.nanoTime();
                    int size = xmlFile.toString().length();
                    int length = xmlFile.toString().split(" ")[1].length()-4;
                    long time = new Long(xmlFile.toString().substring(size-4-length,size-4));
                    //move the file 30 seconds after it was created
                    if ((currentTime-time)/1000000000>30 && nextFile.canRead() && nextFile.canWrite()){
//                        System.out.println(xmlFile);
                        deleteFiles.add(xmlFile);
                        Path filePathInZip = zipfs.getPath(xmlFile.toString().substring(9).replace("\\", " "));//.replace("\\","/"));
                        Files.copy(xmlFile, filePathInZip,StandardCopyOption.REPLACE_EXISTING);//
                        count = count+1;
                        if(count%1000==0){
                            System.out.println("moved "+count+" files");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            for(Path p: deleteFiles){
                deleteFile(p.toString());
            }
            Thread.sleep(1000);
        }
    }
    public static void deleteFile(String path) {
        File aFile = new File(path);
        int count = 0;
        if (aFile.exists()) {
            aFile.delete();
        }
    }

}
