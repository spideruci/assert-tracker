package org.spideruci.asserttracker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.apache.commons.io.FileUtils;
/**
 * Hello world!
 *
 */
public class AssertTracker
{
    public static void main( String[] args ) throws FileNotFoundException, IOException
    {
        if (args.length <= 0) {
            return;
        }

        String base_loc = "target";
        File xmlDirectory = new File( base_loc+File.separator+"xmlOutput");
        if (xmlDirectory.exists()){
            FileUtils.forceDelete(xmlDirectory);
        }

        File file = new File(args[0]);
        if (!file.exists()) {
            final String errorMessage = MessageFormat.format("file does not exist: {0}", file.getName());
            System.out.println(errorMessage);
            new RuntimeException(errorMessage);
        }

        if (file.isDirectory()) {
            // do nothing
            System.out.println(MessageFormat.format("Directory: {0}", args[0]));

            List<Path> classFiles = null;

            try (Stream<Path> fileStream = Files.walk(file.toPath())) {
                classFiles = fileStream.filter(Files::isRegularFile)
                                        .filter(p -> p.toFile().getName().endsWith(".class"))
                                        .collect(Collectors.toList());
            }

//            ZipFileManager zfm = new ZipFileManager("xmlOutput.zip");
            for (Path classFile : classFiles) {
                System.out.println("## " + classFile.toAbsolutePath().getFileName());

                AssertTracker tracker = new AssertTracker(classFile.toFile());

                HashMap<String, ArrayList<ArrayList<LocalVariable>>> localVariableInfo = tracker.traceLocalVariables();
                tracker.instrumentCode(localVariableInfo);
            }
//            zfm.closeZipFile();

        } else if (file.isFile()) {
            System.out.println(MessageFormat.format("File: {0}", args[0]));
            HashMap<String, ArrayList<ArrayList<LocalVariable>>> localVariableInfo = new AssertTracker(file).traceLocalVariables();
            new AssertTracker(file).instrumentCode(localVariableInfo);
        }

    }

    public AssertTracker createInstance(File classfile) throws FileNotFoundException, IOException {
        String absolutePathString = classfile.getAbsolutePath();

        if (!classfile.exists()) {
            throw new RuntimeException(MessageFormat.format("Does not exist: {0}.", absolutePathString));
        }

        if (!classfile.isFile()) {
            throw new RuntimeException(MessageFormat.format("Not a file: {0}.", absolutePathString));
        }

        if (!classfile.getName().endsWith(".class")) {
            throw new RuntimeException(MessageFormat.format("Not a classfile: {0}.", absolutePathString));
        }

        return new AssertTracker(classfile);
    }

    final public File classFile;
    final public ClassReader classReader;

    protected AssertTracker(File file) throws FileNotFoundException, IOException {
        this.classFile = file;
        this.classReader = new ClassReader(new FileInputStream(this.classFile));
    }

    public void traceAsserts() {
        classReader.accept(new AssertTrackingClassVisitor(Opcodes.ASM9), ClassReader.EXPAND_FRAMES);
    }

    public HashMap<String, ArrayList<ArrayList<LocalVariable>>> traceLocalVariables() {
        // passing in an emptyWriter to generate Label offsets.
        // we are not actually changing/rewriting the bytecode with the VariableTrackingClassVisitor.
        ClassWriter emptyWriter = new ClassWriter(ClassReader.EXPAND_FRAMES);
        VariableTrackingClassVisitor variableTrackingClassVisitor = new VariableTrackingClassVisitor(Opcodes.ASM9, emptyWriter);
        classReader.accept(variableTrackingClassVisitor, ClassReader.EXPAND_FRAMES);
//        classReader.accept(variableTrackingClassVisitor,ClassReader.SKIP_FRAMES);
        HashMap<String,ArrayList<ArrayList<LocalVariable>>> trackedVariablesPerMethod = variableTrackingClassVisitor.liveVariablesAtAsserts;

        for (Map.Entry<String, ArrayList<ArrayList<LocalVariable>>> trackedVariables : trackedVariablesPerMethod.entrySet()) {
            String methodName = trackedVariables.getKey();
            ArrayList<ArrayList<LocalVariable>> liveVariables = trackedVariables.getValue();

            System.out.println(">> " + methodName);
            int index = 0;
            for (ArrayList<LocalVariable> localVariables : liveVariables) {
                System.out.println("\t>> Assert Index:" + index);

                for (LocalVariable variable : localVariables) {
                    System.out.println("\t\t>> " + variable.loadOpcodeName() + " " + variable.varIndex + " " + variable.name + " " + variable.desc);
                }
                index += 1;
            }
        }
        return trackedVariablesPerMethod;
    }

    public byte[] fetchIntrumentedCode(HashMap<String, ArrayList<ArrayList<LocalVariable>>> localVariableInfo) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classReader.accept(new AssertTrackingClassVisitor(Opcodes.ASM9, writer,localVariableInfo), ClassReader.EXPAND_FRAMES);
                return writer.toByteArray();
                }

public void instrumentCode(HashMap<String, ArrayList<ArrayList<LocalVariable>>> localVariableInfo) throws IOException {
        byte[] code = fetchIntrumentedCode(localVariableInfo);
        replaceOriginalCode(code);
        }

public void replaceOriginalCode(byte[] code) throws IOException {
        PrintStream byteStream = new PrintStream(this.classFile.getAbsolutePath());
        byteStream.write(code);
        byteStream.close();
        }

        }

