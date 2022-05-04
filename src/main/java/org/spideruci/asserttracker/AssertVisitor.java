package org.spideruci.asserttracker;

import com.thoughtworks.xstream.XStream;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

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

             //System.err.print("recognize an @Test Annotation");
             this.mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
             this.mv.visitTypeInsn(Opcodes.NEW, Type.getInternalName(StringBuilder.class)); //  "java/lang/StringBuilder"
             this.mv.visitInsn(Opcodes.DUP);
             this.mv.visitLdcInsn("recognize an @Test Annotation");
             this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(StringBuilder.class), "<init>", "(Ljava/lang/String;)V", false);
             mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "toString", "()Ljava/lang/String;", false);
             mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

             //System.err.println("Start executing outer test method: XXXXXXXX")
             this.mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
             this.mv.visitTypeInsn(Opcodes.NEW, Type.getInternalName(StringBuilder.class)); //  "java/lang/StringBuilder"
             this.mv.visitInsn(Opcodes.DUP);
             this.mv.visitLdcInsn("Start executing outer test method: ");
             this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(StringBuilder.class), "<init>", "(Ljava/lang/String;)V", false);

             // .append(testmethodname)
             if (this.isDisabled == true)
                 this.mv.visitLdcInsn("DisabledMethod");
             else
                 this.mv.visitLdcInsn(this.methodName);
             this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

             // .append(" ")
             this.mv.visitLdcInsn(" TestClassName: ");
             this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

             // .append(testClassName)
             this.mv.visitLdcInsn(this.testClassName);
             this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

             mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "toString", "()Ljava/lang/String;", false);
             mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

//             super.visitCode();
         }
        super.onMethodEnter();
    }

    @Override
    protected void onMethodExit(int opcode) {
//        System.out.println("\n");
         if (this.isTestAnnotationPresent) {
             this.mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
             this.mv.visitTypeInsn(Opcodes.NEW, Type.getInternalName(StringBuilder.class)); //  "java/lang/StringBuilder"
             this.mv.visitInsn(Opcodes.DUP);
             this.mv.visitLdcInsn("No crash or assertion failure! Finish executing outer test method: ");
             this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(StringBuilder.class), "<init>", "(Ljava/lang/String;)V", false);

             // .append(testmethodname)
             if (this.isDisabled == true)
                 this.mv.visitLdcInsn("DisabledMethod");
             else
                 this.mv.visitLdcInsn(this.methodName);
             this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

             // .append(" ")
             this.mv.visitLdcInsn(" TestClassName: ");
             this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

             // .append(testClassName)
             this.mv.visitLdcInsn(this.testClassName);
             this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

             mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "toString", "()Ljava/lang/String;", false);
             mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
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


    public static Boolean isAssertionStatement(String methodName) {
        final String[] assertionNames = new String[]{"assertArrayEquals", "assertEquals", "assertFalse", "assertNotNull", "assertNotSame", "assertNull", "assertSame", "assertThat", "assertTrue", "assertThrows","assertNotEquals"};
        for(String s : assertionNames){
            if (s.equalsIgnoreCase(methodName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {

        Boolean isAssert = isAssertionStatement(name);

        if (isAssert) {
            String message = "\t + Compiled at " + Instant.now().toEpochMilli() + " start:" + this.methodName + " " + name + " ";
            System.out.println(message);
            insertPrintingProbe(message);
            insertXstreamProbe();
        }

        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);

        if (isAssert) {
            String message = "\t end:" + this.methodName + " " + name;
            System.out.println(message);
            insertPrintingProbe(message);
        }
    }

    protected void insertXstreamProbe(){
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
            this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/spideruci/asserttracker/DumpObject",
                    "dumpObjectUsingXml", "(Ljava/lang/Object;)V", false);




       }

        //
        methodLocalVariableInfo.remove(0);
//        this.visitMaxs();
    }
    protected void insertPrintingProbe(String str) {
        if (this.mv == null) {
            return;
        }

        // System.err
        this.mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");

        // 22: new           #51                 // class java/lang/StringBuilder
        // 25: dup
        // 26: ldc           #53                 // String \t +
        // 28: invokespecial #55                 // Method java/lang/StringBuilder."<init>":(Ljava/lang/String;)V
        // 31: invokestatic  #58                 // Method java/time/Instant.now:()Ljava/time/Instant;
        // 34: invokevirtual #64                 // Method java/time/Instant.toEpochMilli:()J
        // 37: invokevirtual #68                 // Method java/lang/StringBuilder.append:(J)Ljava/lang/StringBuilder;
        // 40: ldc           #72                 // String  start:
        // 42: invokevirtual #74                 // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        // 61: invokevirtual #79                 // Method java/lang/StringBuilder.toString:()Ljava/lang/String;

        // new StringBuilder("\t")
        this.mv.visitTypeInsn(Opcodes.NEW, Type.getInternalName(StringBuilder.class)); //  "java/lang/StringBuilder"
        this.mv.visitInsn(Opcodes.DUP);
        this.mv.visitLdcInsn("\t");
        this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(StringBuilder.class), "<init>", "(Ljava/lang/String;)V", false);

        // Instant.now().toEpochMilli()
        this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Instant.class), "now", "()Ljava/time/Instant;", false);
        this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Instant.class), "toEpochMilli", "()J", false);

        // .append(Instant.now().toEpochMilli())
        this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "append", "(J)Ljava/lang/StringBuilder;", false);

        // .append("//")
        this.mv.visitLdcInsn("//");
        this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

        // System.nanoTime()
        this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(System.class), "nanoTime", "()J", false);
        
        // .append(System.nanoTime())
        this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "append", "(J)Ljava/lang/StringBuilder;", false);

        // .append(`str`)
        this.mv.visitLdcInsn(str);
        this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

        // .append(" ")
        this.mv.visitLdcInsn(" testClassName: ");
        this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

        // .append(testClassName)
        this.mv.visitLdcInsn(this.testClassName);
        this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

        // .toString()
        this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "toString", "()Ljava/lang/String;", false);
        
        this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    }
    
}
