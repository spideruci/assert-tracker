package org.spideruci.asserttracker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

public class ExceptionTracker {
    
    final public File classFile;
    final public ClassReader classReader;

    public ExceptionTracker createInstance(File classfile) throws FileNotFoundException, IOException {
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

        return new ExceptionTracker(classfile);
    }

    protected ExceptionTracker(File file) throws FileNotFoundException, IOException {
        this.classFile = file;
        this.classReader = new ClassReader(new FileInputStream(this.classFile));
    }

    public void trackExceptions() {
        classReader.accept(new AssertTrackingClassVisitor(Opcodes.ASM9), ClassReader.EXPAND_FRAMES);
    }

    public static void main(String[] args) {
        if (args.length <= 0) {
            return;
        }

        File file = new File(args[0]);
        if (!file.exists()) {
            final String errorMessage = MessageFormat.format("file does not exist: {0}", file.getName());
            System.out.println(errorMessage);
            new RuntimeException(errorMessage);
        }

        if (file.exists() && file.isFile()) {
            
        }
    }

}
