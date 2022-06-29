package org.spideruci.asserttracker;

import com.thoughtworks.xstream.XStream;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import org.objectweb.asm.commons.AdviceAdapter;

import static org.objectweb.asm.Opcodes.*;

public class AssertVisitor extends MethodVisitor {

    final public String methodName;
    public Boolean isTestAnnotationPresent;
    boolean isTestClass;
    boolean isBeforeEachPresent;
    boolean isAfterEachPresent;

    boolean isBeforeAllPresent;
    boolean isAfterAllPresent;
    String testClassName;
    boolean isDisabled;
    ArrayList<ArrayList<LocalVariable>> methodLocalVariableInfo;
    public String classOwner;
    boolean hasAssertions;

    public String methodDesc;

    public boolean initPresent;
    public boolean isJunitTestcase;
    public boolean isReturnVoid;
    public boolean beforePresent;
    public boolean afterPresent;

    public boolean isStatic;

    public boolean isSetUpTearDown;

    public AssertVisitor(int api, MethodVisitor methodWriter, int access, String name, String descriptor,
                         ArrayList<ArrayList<LocalVariable>> methodLocalVariableInfo, boolean isTestClass,
                         String testClassName, Boolean isJunitTestcase) {
        super(api, methodWriter);
//        System.out.println(name+" "+descriptor+" "+"hahahahahah");
//        super(api, methodWriter, access, name, descriptor);
        this.methodName = name;
        this.isTestAnnotationPresent= false;
        this.isTestClass = isTestClass;
        this.testClassName = testClassName;
        this.isDisabled = false;
        this.methodLocalVariableInfo = methodLocalVariableInfo;
        this.classOwner=null;
        this.hasAssertions = false;
        this.methodDesc = descriptor;
        this.isBeforeEachPresent=false;
        this.isAfterEachPresent = false;
        this.isReturnVoid = descriptor.endsWith("()V");
        //protected/public+(final)+setup/tearDown
        this.isSetUpTearDown=false;
        if(this.methodName.equals("setUp") || this.methodName.equals("tearDown")){
            if(access==ACC_PUBLIC || access==ACC_PUBLIC+ACC_FINAL ||access==ACC_PROTECTED||access==ACC_PROTECTED+ACC_FINAL){
                this.isSetUpTearDown=true;
            }
        }
        this.isJunitTestcase = isJunitTestcase && isReturnVoid && (access==1 ||access==17) && methodName.startsWith("test");
        this.isJunitTestcase = this.isJunitTestcase || isSetUpTearDown;
        //this is only for junit4
//        this.isJunitTestcase = isJunitTestcase && methodName.startsWith("test") && (access==ACC_PUBLIC+ACC_FINAL || access==ACC_PUBLIC);
        this.isStatic = access==ACC_PUBLIC+ACC_STATIC ||access==ACC_PUBLIC+ACC_STATIC+ACC_FINAL||access==ACC_PRIVATE+ACC_STATIC+ACC_FINAL
                        || access==ACC_PRIVATE+ACC_STATIC;
        initPresent = false;
        beforePresent = false;
        afterPresent = false;

        if(name.equals("<init>")){
            initPresent=true;
        }
    }
//
//    @Override
//    public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
//        System.out.println("haah"+" "+name+" "+descriptor+" "+signature+" "+start+" "+end+" "+index);
//        super.visitLocalVariable(name, descriptor, signature, start, end, index);
//    }

    @Override
    public void visitCode(){
        if(!methodName.equals("access$000")){
            if(this.beforePresent){
                String content = "enter Before method ";
                this.mv.visitLdcInsn(content);
                //invoke InstrumentationUtils.printString(content)
                this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                        "printString", "(Ljava/lang/String;)V", false);
            }else if(this.afterPresent){
                String content = "enter After method ";
                this.mv.visitLdcInsn(content);
                //invoke InstrumentationUtils.printString(content)
                this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                        "printString", "(Ljava/lang/String;)V", false);
            }else if (this.isBeforeEachPresent){
                String content = "enter BeforeEach method ";
                this.mv.visitLdcInsn(content);
                //invoke InstrumentationUtils.printString(content)
                this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                        "printString", "(Ljava/lang/String;)V", false);
            }else if (this.isAfterEachPresent){
                String content = "enter AfterEach method ";
                this.mv.visitLdcInsn(content);
                //invoke InstrumentationUtils.printString(content)
                this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                        "printString", "(Ljava/lang/String;)V", false);
            }else if (this.isBeforeAllPresent){
                String content = "enter BeforeAll method ";
                this.mv.visitLdcInsn(content);
                //invoke InstrumentationUtils.printString(content)
                this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                        "printString", "(Ljava/lang/String;)V", false);
            }else if (this.isAfterAllPresent){
                String content = "enter AfterAll method ";
                this.mv.visitLdcInsn(content);
                //invoke InstrumentationUtils.printString(content)
                this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                        "printString", "(Ljava/lang/String;)V", false);
            }else if(this.initPresent && isTestClass && !testClassName.endsWith("JexlTestCase")){
                String content = "Enter Junit4 Constructor ";
                this.mv.visitLdcInsn(content);
                //invoke InstrumentationUtils.printString(content)
                this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                        "printString", "(Ljava/lang/String;)V", false);
            }else if ((this.isTestAnnotationPresent || isJunitTestcase)) {
                this.cleanObjectarray();
                String content = "recognize an @Test Annotation ";
                this.mv.visitLdcInsn(content);
                //invoke InstrumentationUtils.printString(content)
                this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                        "printString", "(Ljava/lang/String;)V", false);
                if(Utils.calculateParaNum(this.methodDesc)!=0){
                    content = "parameterized/multilocale test: run at ";
                    this.mv.visitLdcInsn(content);
                    this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                            "printString", "(Ljava/lang/String;)V", false);
                }
                content = "Start executing outer test method: ";
                if (this.isDisabled == true)
                    content = content +"DisabledMethod";
                else
                    content = content +this.methodName;
                content = content+" TestClassName: "+this.testClassName+" ";
                this.mv.visitLdcInsn(content);
                //invoke InstrumentationUtils.printString(content)
                this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                        "printString", "(Ljava/lang/String;)V", false);
            }else if(isTestClass && methodName.equals("<clinit>")){
                String content = "Enter Static Constructor ";
                this.mv.visitLdcInsn(content);
                //invoke InstrumentationUtils.printString(content)
                this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                        "printString", "(Ljava/lang/String;)V", false);
            }
        }

        super.visitCode();

    }

    @Override
    public void visitInsn(int opcode) {
        if(!methodName.equals("access$000") &&
                ( isJunitTestcase||isTestAnnotationPresent || isBeforeEachPresent ||
                isBeforeAllPresent || isAfterEachPresent || isAfterAllPresent ||
                (initPresent&& isTestClass) || beforePresent||afterPresent||(isTestClass && methodName.equals("<clinit>")))){
            switch (opcode) {
                case Opcodes.IRETURN:
                case Opcodes.FRETURN:
                case Opcodes.ARETURN:
                case Opcodes.LRETURN:
                case Opcodes.DRETURN:
                case Opcodes.RETURN:

                    //before and after comes before testcase methods
                    if(beforePresent){
                        this.mv.visitLdcInsn("exit Before Method ");
                        //invoke InstrumentationUtils.printString(content)
                        this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                                "printString", "(Ljava/lang/String;)V", false);
                    }else if(afterPresent){
                        this.mv.visitLdcInsn("exit After Method ");
                        //invoke InstrumentationUtils.printString(content)
                        this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                                "printString", "(Ljava/lang/String;)V", false);
                    }else if(initPresent && isTestClass && !testClassName.endsWith("JexlTestCase")) {
                        this.mv.visitLdcInsn("exit Constructor Method ");
                        //invoke InstrumentationUtils.printString(content)
                        this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                                "printString", "(Ljava/lang/String;)V", false);
                    }else if(isTestAnnotationPresent || isJunitTestcase){
                        this.cleanObjectarray();
                        String content = "No crash or assertion failure! Finish executing outer test method: ";
                        // .append(testmethodname)
                        if (this.isDisabled == true)
                            content = content+ "DisabledMethod";
                        else
                            content = content+this.methodName;
                        content = content +" TestClassName: "+this.testClassName+ " ";

                        this.mv.visitLdcInsn(content);
                        //invoke InstrumentationUtils.printString(content)
                        this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                                "printString", "(Ljava/lang/String;)V", false);
                    }else if(isBeforeEachPresent){
                        this.mv.visitLdcInsn("exit BeforeEach Method ");
                        //invoke InstrumentationUtils.printString(content)
                        this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                                "printString", "(Ljava/lang/String;)V", false);
                    }else if(isAfterEachPresent){
                        this.mv.visitLdcInsn("exit AfterEach Method ");
                        //invoke InstrumentationUtils.printString(content)
                        this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                                "printString", "(Ljava/lang/String;)V", false);
                    }else if(isBeforeAllPresent){
                        this.mv.visitLdcInsn("exit BeforeAll Method ");
                        //invoke InstrumentationUtils.printString(content)
                        this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                                "printString", "(Ljava/lang/String;)V", false);
                    }else if(isAfterAllPresent){
                        this.mv.visitLdcInsn("exit AfterAll Method ");
                        //invoke InstrumentationUtils.printString(content)
                        this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                                "printString", "(Ljava/lang/String;)V", false);
                    }else if(isTestClass && methodName.equals("<clinit>")){
                        this.mv.visitLdcInsn("exit Static Method ");
                        //invoke InstrumentationUtils.printString(content)
                        this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                                "printString", "(Ljava/lang/String;)V", false);
                    }
                    break;
                default: // do nothing
            }
        }

//        //whenever we find a RETURN and if its method is annotated with @Test, insert something
//        if(this.isTestAnnotationPresent) {
//            switch (opcode) {
//                case Opcodes.IRETURN:
//                case Opcodes.FRETURN:
//                case Opcodes.ARETURN:
//                case Opcodes.LRETURN:
//                case Opcodes.DRETURN:
//                case Opcodes.RETURN:
//                    if (this.isTestAnnotationPresent) {
//                        if (this.hasAssertions==false){
//                            Utils.appendLogAt("target/No_Assertions.txt",new String[]{testClassName+" "+methodName});
//                        }
//                        this.cleanObjectarray();
//                        String content = "No crash or assertion failure! Finish executing outer test method: ";
//                        // .append(testmethodname)
//                        if (this.isDisabled == true)
//                            content = content+ "DisabledMethod";
//                        else
//                            content = content+this.methodName;
//                        content = content +" TestClassName: "+this.testClassName+ " ";
//
//                        this.mv.visitLdcInsn(content);
//                        //invoke InstrumentationUtils.printString(content)
//                        this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
//                                "printString", "(Ljava/lang/String;)V", false);
//                    }
//                    break;
//                default: // do nothing
//            }
//        }
        super.visitInsn(opcode);
    }
    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        //System.out.println("visitAnnotation works");
        // 1. if annotation present, then set the flag to true
        //@org.junit.jupiter.api.Test() is not enough.
        //may be @MultilocaleTest?

        if(desc.endsWith("BeforeEach;")){
            isBeforeEachPresent = true;
        }
        if(desc.endsWith("AfterEach;")){
            isAfterEachPresent = true;
        }
        if(desc.endsWith("Test;")){
            isTestAnnotationPresent = true;
        }
        if(desc.endsWith("BeforeAll;")){
            isBeforeAllPresent = true;
        }
        if(desc.endsWith("AfterAll;")){
            isAfterAllPresent = true;
        }
        if(desc.endsWith("Disabled;")){
            //System.out.println("detect @Disabled");
            this.isDisabled = true;
        }
        if(desc.endsWith("After;")){
            afterPresent=true;
        }
        if(desc.endsWith("Before;")){
            beforePresent=true;
        }

        return super.visitAnnotation(desc, visible);
    }


    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {

        Boolean isAssert = Utils.isAssertionStatement(name);

//        if (isAssert && isTestAnnotationPresent) {
//            this.hasAssertions = true;
//            // only consider the first layer of assertion statements, i.e., the assertion statements in the test case method labeled with "@Test" annotation
//            // sometimes, public test case method invokes private method where it holds some assertion statements, we do not do instrumentation
//            // since they usually accept some parameters from the outer method. If those parameters are tainted, they would be tainted to the private method
//
//            String message = "\t + Compiled at " + Instant.now().toEpochMilli() + " start:" + this.methodName + " " + name + " ";
//            System.out.println(message);
//            cleanObjectarray();
//            insertPrintingProbe(message);
//            if(this.isTestClass){
//                insertXstreamProbe();
//            }
//        }else if(isAssert){
//            String message = "\t + nested method Compiled at " + Instant.now().toEpochMilli() + " start:" + this.methodName + " " + name + " ";
//            System.out.println(message);
//            insertPrintingProbe(message);
//        }

        if(isAssert ){
            String message = "\t + Compiled at " + Instant.now().toEpochMilli() + " start:" + this.methodName + " " + name + " ";
            System.out.println(message);
            cleanObjectarray();
            insertPrintingProbe(message);
            insertXstreamProbe();
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);

        if(isAssert){
            String message = "\t end:" + this.methodName + " " + name;
            System.out.println(message);
            insertPrintingProbe(message);
        }
//        if (isAssert && isTestAnnotationPresent) {
//            String message = "\t end:" + this.methodName + " " + name;
//            System.out.println(message);
//            insertPrintingProbe(message);
//        }else if(isAssert){
//            String message = "\t nested method end:" + this.methodName + " " + name;
//            System.out.println(message);
//            insertPrintingProbe(message);
//        }
    }

    protected void insertXstreamProbe(){

        if (methodLocalVariableInfo.size()==0 || XStreamBL.testsBlacklist.contains(this.methodName+" "+this.testClassName)){
            return;
        }

        ArrayList<LocalVariable> localVariables = methodLocalVariableInfo.get(0);
        String variableNames = "";
        for(LocalVariable v: localVariables){
            variableNames = variableNames + " "+ v.name;
        }
        System.out.println("\t\t Going to use Xstream dump "+ variableNames +" before this assertion statement.");

        // make an object array

        //aload 0: load "this"
//        this.mv.visitVarInsn(Opcodes.ALOAD,0);
        //iconst_x: get the size of the object array
        this.mv.visitLdcInsn(localVariables.size());
        //anewarray: create a new array
        this.mv.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(Object.class));
        //putfield
        this.mv.visitFieldInsn(Opcodes.PUTSTATIC,"AssertDumper/LocalVariableDumper","_ObjectArray","[Ljava/lang/Object;");

        //stuff local variables into this object array
        int index = 0;
        String variables_name = "";
        for(LocalVariable v: localVariables) {
            //aload 0: load "this"
//            if(!isStatic){
//                this.mv.visitVarInsn(Opcodes.ALOAD,0);
//            }
            this.mv.visitFieldInsn(Opcodes.GETSTATIC,"AssertDumper/LocalVariableDumper","_ObjectArray","[Ljava/lang/Object;");
            //iconst_x: get the xth local variable
//            this.mv.visitLdcInsn(index);

//            this.mv.visitVarInsn(Opcodes.IConst);
//            this.mv.visitVarInsn(Opcodes.ICONST_M1,0);
            //load the local variable to be stored
            this.visitIntInsn(Opcodes.BIPUSH,index);
//            this.visitVarInsn(Opcodes.BIPUSH,index);
            this.mv.visitVarInsn(v.loadOpcode(),v.varIndex);//variables.get(0).index
            // transfrom some primitive types as objects
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
            this.mv.visitInsn(Opcodes.AASTORE);
            index = index+1;
            variables_name = variables_name +v.name+" ";
        }

        //get the object array object
//        this.mv.visitVarInsn(Opcodes.ALOAD,0);
        this.mv.visitFieldInsn(Opcodes.GETSTATIC,"AssertDumper/LocalVariableDumper","_ObjectArray","[Ljava/lang/Object;");

        //dump the object array using XStream
        this.mv.visitLdcInsn(variables_name);
        this.mv.visitLdcInsn(this.testClassName);
        this.mv.visitLdcInsn(this.methodName);
        this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                "dumpObjectUsingXml", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false);



//
//
//
//        //instrumentation for using XML to dump local variables
//        for(LocalVariable v: localVariables) {
//            //load the localVariable and convert it to objects if necessary
//            this.mv.visitVarInsn(v.loadOpcode(),v.varIndex);//variables.get(0).index
//            switch(v.loadOpcode()){
//                // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;    21
//                // Method java/lang/Double.valueOf:(D)Ljava/lang/Double;      24
//                // Method java/lang/Long.valueOf:(J)Ljava/lang/Long;          22
//                // Method java/lang/Float.valueOf:(F)Ljava/lang/Float;        23
//                case 21:
//                    this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf","(I)Ljava/lang/Integer;",false);
//                    break;
//                case 22:
//                    this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf","(J)Ljava/lang/Long;",false);
//                    break;
//                case 23:
//                    this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf","(F)Ljava/lang/Float;",false);
//                    break;
//                case 24:
//                    this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf","(D)Ljava/lang/Double;",false);
//            }
//            this.mv.visitLdcInsn(v.name);
//            this.mv.visitLdcInsn(this.testClassName);
//            if(Utils.calculateParaNum(methodDesc)!=0){
//                this.mv.visitLdcInsn(this.methodName);
//            }else{
//                this.mv.visitLdcInsn(this.methodName);
//            }
//            this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
//                    "dumpObjectUsingXml", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false);
//       }

        methodLocalVariableInfo.remove(0);
    }
    protected void insertPrintingProbe(String str) {

        if (this.mv == null) {
            return;
        }

        String content = "\t"+str+" testClassName: "+this.testClassName+" ";
        this.mv.visitLdcInsn(content);
        //invoke InstrumentationUtils.printString(content)
        this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "InstrumentationUtils",
                "printString", "(Ljava/lang/String;)V", false);
    }

    protected void cleanObjectarray(){

        //load "this"
//        this.mv.visitVarInsn(Opcodes.ALOAD,0);
//        aload_0
//        34: aconst_null
//        35: putfield      #89                 // Field _ObjectArray:[Ljava/lang/Object;
        this.mv.visitInsn(Opcodes.ACONST_NULL);
//        this.mv.visitLdcInsn(Opcodes.ACONST_NULL);
        this.mv.visitFieldInsn(Opcodes.PUTSTATIC,"AssertDumper/LocalVariableDumper","_ObjectArray","[Ljava/lang/Object;");

    }

}
