package org.spideruci.asserttracker;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;


public class AssertTrackingClassVisitor extends ClassVisitor {

    private boolean isTestClass;
    private String testClassName;
    private HashMap<String, ArrayList<ArrayList<LocalVariable>>> localVariableInfo;

    public AssertTrackingClassVisitor(int api) {
        super(api);
        this.isTestClass=false;
        this.testClassName = "unknown";
    }

    public AssertTrackingClassVisitor(int api, ClassWriter cw) {
        super(api, cw);
        this.isTestClass=false;
        this.testClassName = "unknown";
    }

    public AssertTrackingClassVisitor(int api, ClassWriter cw, HashMap<String, ArrayList<ArrayList<LocalVariable>>> localVariableInfo) {
        super(api, cw);
        this.isTestClass=false;
        this.testClassName = "unknown";
        this.localVariableInfo = localVariableInfo;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        //sometimes they are not names for test classes, but it doesn't matter. Since we only print out assertion info
        //it indicates that it's a test class name
        Pattern testclasspattern = Pattern.compile("test");
        Matcher testclassmatcher = testclasspattern.matcher(name.toLowerCase());
        if(testclassmatcher.find()){
            this.isTestClass = true;
        }
        this.testClassName = name.replace("/",".");
        //instrument a new object array field
        if(this.isTestClass){
            this.visitField(access,"_ObjectArray","[Ljava/lang/Object;",null, (Object)null);
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        return super.visitField(access, name, descriptor, signature, value);
    }


    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor methodWriter = this.cv == null ? null : this.cv.visitMethod(access, name, descriptor, signature, exceptions);
        ArrayList<ArrayList<LocalVariable>> methodLocalVariableInfo = localVariableInfo.getOrDefault(name+descriptor+access,null);
        return new AssertVisitor(api, methodWriter, access, name, descriptor, methodLocalVariableInfo, isTestClass, testClassName);
    }
}

