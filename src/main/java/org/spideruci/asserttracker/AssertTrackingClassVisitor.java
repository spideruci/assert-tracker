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
import static org.objectweb.asm.Opcodes.ACC_STATIC;


public class AssertTrackingClassVisitor extends ClassVisitor {

    private boolean isTestClass;
    private String testClassName;
    private Boolean isJunitTest4;
    private HashMap<String, ArrayList<ArrayList<LocalVariable>>> localVariableInfo;

    public AssertTrackingClassVisitor(int api) {
        super(api);
        this.isTestClass=false;
        this.testClassName = "unknown";
        this.isJunitTest4=false;
    }

    public AssertTrackingClassVisitor(int api, ClassWriter cw) {
        super(api, cw);
        this.isTestClass=false;
        this.testClassName = "unknown";
        this.isJunitTest4=false;
    }

    public AssertTrackingClassVisitor(int api, ClassWriter cw, HashMap<String, ArrayList<ArrayList<LocalVariable>>> localVariableInfo) {
        super(api, cw);
        this.isTestClass=false;
        this.testClassName = "unknown";
        this.localVariableInfo = localVariableInfo;
        this.isJunitTest4=false;
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
        //instrument a new object array field
//        if(this.isTestClass){
////            this.visitField(access, "_assertions_hit_count","Ljava/lang/Integer;",null,null);
//            this.visitField(ACC_PUBLIC+ACC_STATIC,"_ObjectArray","[Ljava/lang/Object;",null, (Object)null).visitEnd();
//        }
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
        return new AssertVisitor(api, methodWriter, access, name, descriptor, methodLocalVariableInfo, isTestClass, testClassName,isJunitTest4);
    }
}

