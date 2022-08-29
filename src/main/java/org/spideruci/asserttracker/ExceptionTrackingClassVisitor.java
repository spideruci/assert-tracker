package org.spideruci.asserttracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

public class ExceptionTrackingClassVisitor extends ClassVisitor {

    private boolean isTestClass;
    private String testClassName;
    private Boolean isJunitTest4;
    final private int pass;

    public ExceptionTrackingClassVisitor(int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
        pass = 0;
    }

    public ExceptionTrackingClassVisitor(int api) {
        super(api);
        pass = 0;
    }

    public ExceptionTrackingClassVisitor(int api, ClassWriter cw) {
        super(api, cw);
        this.isTestClass=false;
        this.testClassName = "unknown";
        this.isJunitTest4=false;
        pass = 0;
    }

    public ExceptionTrackingClassVisitor(int api, ClassWriter cw, int pass) {
        super(api, cw);
        this.isTestClass=false;
        this.testClassName = "unknown";
        this.isJunitTest4=false;
        this.pass = pass;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        //sometimes they are not names for test classes, but it doesn't matter. Since we only print out assertion info
        //it indicates that it's a test class name
        if(superName.endsWith("TestCase")){
            this.isTestClass = true;
            this.isJunitTest4 = true;
        }

        Pattern testclasspattern = Pattern.compile("test");
        Matcher testclassmatcher = testclasspattern.matcher(name.toLowerCase());
        
        if(testclassmatcher.find()){
            this.isTestClass = true;
        }
        
        if(name.endsWith("IT")||name.startsWith("IT")||name.endsWith("ITCase")){
            this.isTestClass = true;
        }

        //Sometimes there are inner classes within a Test class Their names are like "xxxxTest$XXXX"
        //exclude those classes whose name contain "$"
        Pattern testclassexcludepattern = Pattern.compile("\\$");
        Matcher testclassexcludematcher = testclassexcludepattern.matcher(name.toLowerCase());
        
        if(testclassexcludematcher.find()){
            this.isTestClass = false;
        }

        this.testClassName = name.replace("/",".");

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor methodWriter = this.cv == null ? null : this.cv.visitMethod(access, name, descriptor, signature, exceptions);
        if (isTestClass) {
            if (pass == 0) {
                return new ExceptionVisitor(api, methodWriter, access, name, descriptor, isTestClass, testClassName, isJunitTest4);
            } else if (pass == 1) {
                return new ExceptionVisitor2(api, methodWriter, access, name, descriptor, isTestClass, testClassName, isJunitTest4);
            } else {
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
            
        } else {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
    }

}
