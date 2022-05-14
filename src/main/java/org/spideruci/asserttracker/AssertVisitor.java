package org.spideruci.asserttracker;

import com.thoughtworks.xstream.XStream;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import org.objectweb.asm.commons.AdviceAdapter;

public class AssertVisitor extends AdviceAdapter {

    final public String methodName;
    public Boolean isTestAnnotationPresent;
    boolean isTestClass;
    String testClassName;
    boolean isDisabled;
    ArrayList<ArrayList<LocalVariable>> methodLocalVariableInfo;

    public AssertVisitor(int api, MethodVisitor methodWriter, int access, String name, String descriptor,
                         ArrayList<ArrayList<LocalVariable>> methodLocalVariableInfo, boolean isTestClass,
                         String testClassName) {
        super(api, methodWriter, access, name, descriptor);
        this.methodName = name;
        this.isTestAnnotationPresent= false;
        this.isTestClass = isTestClass;
        this.testClassName = testClassName;
        this.isDisabled = false;
        this.methodLocalVariableInfo = methodLocalVariableInfo;
    }

    @Override
    protected void onMethodEnter() {
//        System.out.println("\nStarting " + methodName + "\n");

         if (this.isTestAnnotationPresent) {
             String content = "recognize an @Test Annotation";
             this.mv.visitLdcInsn(content);
             //invoke InstrumentationUtils.printString(content)
             this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                     "printString", "(Ljava/lang/String;)V", false);
             if(Utils.calculateParaNum(this.methodDesc)!=0){
                 content = "parameterized/multilocale test: run at"+ System.nanoTime();
                 this.mv.visitLdcInsn(content);
                 this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                         "printString", "(Ljava/lang/String;)V", false);
             }
             content = "Start executing outer test method: ";
             if (this.isDisabled == true)
                 content = content +"DisabledMethod";
             else
                 content = content +this.methodName;
             content = content+" TestClassName: "+this.testClassName;
             this.mv.visitLdcInsn(content);
             //invoke InstrumentationUtils.printString(content)
             this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                     "printString", "(Ljava/lang/String;)V", false);
         }
        super.onMethodEnter();
    }

    @Override
    protected void onMethodExit(int opcode) {
//        System.out.println("\n");
         if (this.isTestAnnotationPresent) {
             String content = "No crash or assertion failure! Finish executing outer test method: ";
             // .append(testmethodname)
             if (this.isDisabled == true)
                 content = content+ "DisabledMethod";
             else
                 content = content+this.methodName;
             content = content +" TestClassName: "+this.testClassName;

             this.mv.visitLdcInsn(content);
             //invoke InstrumentationUtils.printString(content)
             this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                     "printString", "(Ljava/lang/String;)V", false);
        }
         super.onMethodExit(opcode);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        //System.out.println("visitAnnotation works");
        // 1. if annotation present, then set the flag to true
        //@org.junit.jupiter.api.Test() is not enough.
        //may be @MultilocaleTest?

        if(desc.endsWith("Test;")){
            //System.out.println("detect @Test");
            isTestAnnotationPresent = true;
        }
        if(desc.endsWith("Disabled;")){
            //System.out.println("detect @Disabled");
            this.isDisabled = true;
        }

        return super.visitAnnotation(desc, visible);
    }


    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {

        Boolean isAssert = Utils.isAssertionStatement(name);

        if (isAssert && isTestAnnotationPresent) {
            // only consider the first layer of assertion statements, i.e., the assertion statements in the test case method labeled with "@Test" annotation
            // sometimes, public test case method invokes private method where it holds some assertion statements, we do not do instrumentation
            // since they usually accept some parameters from the outer method. If those parameters are tainted, they would be tainted to the private method

            String message = "\t + Compiled at " + Instant.now().toEpochMilli() + " start:" + this.methodName + " " + name + " ";
            System.out.println(message);
            insertPrintingProbe(message);
            insertXstreamProbe();
        }

        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);

        if (isAssert && isTestAnnotationPresent) {
            String message = "\t end:" + this.methodName + " " + name;
            System.out.println(message);
            insertPrintingProbe(message);
        }
    }

    protected void insertXstreamProbe(){
        if (methodLocalVariableInfo.size()==0){
            return;
        }
        ArrayList<LocalVariable> localVariables = methodLocalVariableInfo.get(0);
        String variableNames = "";
        for(LocalVariable v: localVariables){
            variableNames = variableNames + " "+ v.name;
        }
        System.out.println("\t\t Going to use Xstream dump "+ variableNames +" before this assertion statement.");

        //instrumentation for using XML to dump local variables
        for(LocalVariable v: localVariables) {
            //load the localVariable and convert it to objects if necessary
            this.mv.visitVarInsn(v.loadOpcode(),v.varIndex);//variables.get(0).index
            switch(v.loadOpcode()){
                // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;    21
                // Method java/lang/Double.valueOf:(D)Ljava/lang/Double;      24
                // Method java/lang/Long.valueOf:(J)Ljava/lang/Long;          22
                // Method java/lang/Float.valueOf:(F)Ljava/lang/Float;        23
                case 21:
                    this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf","(I)Ljava/lang/Integer;",false);
                    break;
                case 22:
                    this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf","(J)Ljava/lang/Long;",false);
                    break;
                case 23:
                    this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf","(F)Ljava/lang/Float;",false);
                    break;
                case 24:
                    this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf","(D)Ljava/lang/Double;",false);
            }
            this.mv.visitLdcInsn(v.name);
            this.mv.visitLdcInsn(this.testClassName);
            if(Utils.calculateParaNum(methodDesc)!=0){
                this.mv.visitLdcInsn(this.methodName+ File.separator+"para "+System.nanoTime());
            }else{
                this.mv.visitLdcInsn(this.methodName);
            }
            this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                    "dumpObjectUsingXml", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false);
       }

        methodLocalVariableInfo.remove(0);
//        this.visitMaxs();
    }
    protected void insertPrintingProbe(String str) {
        if (this.mv == null) {
            return;
        }
        String content = "\t" +Instant.now().toEpochMilli()+"//"+System.nanoTime()+str+" testClassName: "+this.testClassName;
        this.mv.visitLdcInsn(content);
        //invoke InstrumentationUtils.printString(content)
        this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                "printString", "(Ljava/lang/String;)V", false);
    }
    
}
