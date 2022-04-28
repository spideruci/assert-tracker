package org.spideruci.asserttracker;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.time.Instant;

import org.objectweb.asm.commons.AdviceAdapter;

public class AssertVisitor extends AdviceAdapter {

    final public String methodName;
    public Boolean isTestAnnotationPresent;
    boolean isTestClass;
    String testClassName;
    boolean isDisabled;

    public AssertVisitor(int api, MethodVisitor methodWriter, int access, String name, String descriptor, boolean isTestClass, String testClassName) {
        super(api, methodWriter, access, name, descriptor);
        this.methodName = name;
        this.isTestAnnotationPresent= false;
        this.isTestClass = isTestClass;
        this.testClassName = testClassName;
        this.isDisabled = false;
    }

    @Override
    protected void onMethodEnter() {
        System.out.println("\n");
        super.onMethodEnter();
    }

    @Override
    protected void onMethodExit(int opcode) {
        System.out.println("\n");
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


    // @Override
    // public void visitCode() {

    //     // 1. if we see @Test annotation, then instrument some code

    //     if (this.isTestAnnotationPresent) {

    //         //System.err.print("recognize an @Test Annotation");
    //         this.mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
    //         this.mv.visitTypeInsn(Opcodes.NEW, Type.getInternalName(StringBuilder.class)); //  "java/lang/StringBuilder"
    //         this.mv.visitInsn(Opcodes.DUP);
    //         this.mv.visitLdcInsn("recognize an @Test Annotation");
    //         this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(StringBuilder.class), "<init>", "(Ljava/lang/String;)V", false);
    //         mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "toString", "()Ljava/lang/String;", false);
    //         mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

    //         //System.err.println("Start executing outer test method: XXXXXXXX")
    //         this.mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
    //         this.mv.visitTypeInsn(Opcodes.NEW, Type.getInternalName(StringBuilder.class)); //  "java/lang/StringBuilder"
    //         this.mv.visitInsn(Opcodes.DUP);
    //         this.mv.visitLdcInsn("Start executing outer test method: ");
    //         this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(StringBuilder.class), "<init>", "(Ljava/lang/String;)V", false);

    //         // .append(testmethodname)
    //         if (this.isDisabled == true)
    //             this.mv.visitLdcInsn("DisabledMethod");
    //         else
    //             this.mv.visitLdcInsn(this.methodName);
    //         this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

    //         // .append(" ")
    //         this.mv.visitLdcInsn(" TestClassName: ");
    //         this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

    //         // .append(testClassName)
    //         this.mv.visitLdcInsn(this.testClassName);
    //         this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

    //         mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "toString", "()Ljava/lang/String;", false);
    //         mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

    //         super.visitCode();
    //     }
    // }

    public Boolean isAssertionStatement(String statementName) {
        String[] assertionNames = new String[]{"assertArrayEquals", "assertEquals", "assertFalse", "assertNotNull", "assertNotSame", "assertNull", "assertSame", "assertThat", "assertTrue", "assertThrows","assertNotEquals"};
        for(String s:assertionNames){
            if (s.equalsIgnoreCase(statementName)){
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
        }

        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);

        if (isAssert) {
            String message = "\t end:" + this.methodName + " " + name;
            System.out.println(message);
            insertPrintingProbe(message);
        }
    }

    // @Override
    // public void visitInsn(int opcode) {
    //     //whenever we find a RETURN and if its method is annotated with @Test, insert something
    //     if(this.isTestAnnotationPresent) {
    //         switch (opcode) {
    //             case Opcodes.IRETURN:
    //             case Opcodes.FRETURN:
    //             case Opcodes.ARETURN:
    //             case Opcodes.LRETURN:
    //             case Opcodes.DRETURN:
    //             case Opcodes.RETURN:
    //                 //System.err.println("No crash or assertion failure! Finish executing outer test method: XXXXXXXX")
    //                 this.mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
    //                 this.mv.visitTypeInsn(Opcodes.NEW, Type.getInternalName(StringBuilder.class)); //  "java/lang/StringBuilder"
    //                 this.mv.visitInsn(Opcodes.DUP);
    //                 this.mv.visitLdcInsn("No crash or assertion failure! Finish executing outer test method: ");
    //                 this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(StringBuilder.class), "<init>", "(Ljava/lang/String;)V", false);

    //                 // .append(testmethodname)
    //                 if (this.isDisabled == true)
    //                     this.mv.visitLdcInsn("DisabledMethod");
    //                 else
    //                     this.mv.visitLdcInsn(this.methodName);
    //                 this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

    //                 // .append(" ")
    //                 this.mv.visitLdcInsn(" TestClassName: ");
    //                 this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

    //                 // .append(testClassName)
    //                 this.mv.visitLdcInsn(this.testClassName);
    //                 this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

    //                 mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "toString", "()Ljava/lang/String;", false);
    //                 mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    //                 break;
    //             default: // do nothing
    //         }
    //     }
    //     super.visitInsn(opcode);
    // }

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
