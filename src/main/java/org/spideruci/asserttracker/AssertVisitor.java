package org.spideruci.asserttracker;

import java.time.Instant;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class AssertVisitor extends MethodVisitor {

    final public String methodName;

    public AssertVisitor(int api, String methodName, MethodVisitor methodWriter) {
        super(api, methodWriter);
        this.methodName = methodName;
    }

    public AssertVisitor(int api, String methodName) {
        super(api);
        this.methodName = methodName;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {

        Boolean isAssert = name.toLowerCase().startsWith("assert");

        if (isAssert) {
            String message = "\t + Compiled at " + Instant.now().toEpochMilli() + " start:" + this.methodName + " " + name;
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

    protected void insertPrintingProbe(String str) {
        if (this.mv == null) {
            return;
        }

        // System.out
        this.mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");

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

        // .toString()
        this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "toString", "()Ljava/lang/String;", false);
        
        this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    }
    
}
