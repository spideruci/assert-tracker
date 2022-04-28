package org.spideruci.asserttracker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

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

        File file = new File(args[0]);
//        Path path = Path.of(args[0]);
//        File file = path.toFile();

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

            for (Path classFile : classFiles) {
                System.out.println("## " + classFile.toAbsolutePath().getFileName());

                AssertTracker tracker = new AssertTracker(classFile.toFile());

//                tracker.traceAsserts();
                 tracker.instrumentCode();
            }

        } else if (file.isFile()) {
            System.out.println(MessageFormat.format("File: {0}", args[0]));
//            new AssertTracker(file).traceAsserts();
            new AssertTracker(file).instrumentCode();
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

    public byte[] fetchIntrumentedCode() {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classReader.accept(new AssertTrackingClassVisitor(Opcodes.ASM9, writer), ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    public void instrumentCode() throws IOException {
        byte[] code = fetchIntrumentedCode();
        replaceOriginalCode(code);
    }

    public void replaceOriginalCode(byte[] code) throws IOException {
        PrintStream byteStream = new PrintStream(this.classFile.getAbsolutePath());
        byteStream.write(code);
        byteStream.close();
    }

}

