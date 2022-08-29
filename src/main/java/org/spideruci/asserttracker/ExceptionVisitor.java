package org.spideruci.asserttracker;

import java.util.ArrayList;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ExceptionVisitor extends MethodVisitor {

    public static String LDC_MARKER = "spideruci.throwable.handler";

    private boolean isBeforeEachPresent = false;
    private boolean isAfterEachPresent = false;
    private boolean isTestAnnotationPresent = false;
    private boolean isDisabled = false;
    private boolean isBeforeClassPresent = false;
    private boolean isAfterClassPresent = false;
    private boolean afterPresent = false;
    private boolean beforePresent = false;

    private int lastVisitedOffset = 0;
    private Label lastVisitedLabel = null;
    private String methodName;
    private boolean isTestClass;
    private String testClassName;
    private Object classOwner;
    private boolean hasAssertions;
    private String methodDesc;
    private boolean isReturnVoid;
    private boolean isSetUpTearDown;
    private boolean isJunitTestcase;
    private Object isStatic;
    private boolean initPresent;


    public ExceptionVisitor(int api, MethodVisitor methodVisitor) {
        super(api, methodVisitor);
    }

    public ExceptionVisitor(int api, MethodVisitor methodWriter, 
        int access, String name, String descriptor, boolean isTestClass,
        String testClassName, Boolean isJunitTestcase) {
        super(api, methodWriter);

        this.methodName = name;
        this.isTestAnnotationPresent= false;
        this.isTestClass = isTestClass;
        this.testClassName = testClassName;
        this.isDisabled = false;
        this.classOwner=null;
        this.hasAssertions = false;
        this.methodDesc = descriptor;
        this.isBeforeEachPresent=false;
        this.isAfterEachPresent = false;
        this.isReturnVoid = descriptor.endsWith("()V");
        
        this.isSetUpTearDown=false;
        if (this.methodName.equals("setUp") || this.methodName.equals("tearDown")) {
            if (access == Opcodes.ACC_PUBLIC 
                || access == Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL 
                || access==Opcodes.ACC_PROTECTED
                ||access== Opcodes.ACC_PROTECTED + Opcodes.ACC_FINAL) {
                this.isSetUpTearDown=true;
            }
        }
        
        this.isJunitTestcase = isJunitTestcase 
                                && isReturnVoid 
                                && (access==1 || access==17) 
                                && methodName.startsWith("test");
        this.isJunitTestcase = this.isJunitTestcase || isSetUpTearDown;

        this.isStatic = access == Opcodes.ACC_PUBLIC+ Opcodes.ACC_STATIC 
                                    || access == Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC+ Opcodes.ACC_FINAL
                                    || access == Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC + Opcodes.ACC_FINAL
                                    || access == Opcodes. ACC_PRIVATE+ Opcodes.ACC_STATIC;
        initPresent = false;
        beforePresent = false;
        afterPresent = false;

        if(name.equals("<init>")){
            initPresent=true;
        }
    }

    public void visitCode() {
        System.out.println();
    }

    @Override
    public void visitEnd() {

        if (isJunitTestcase || isTestAnnotationPresent) {
            mv.visitLdcInsn(LDC_MARKER);
            mv.visitVarInsn(Opcodes.ASTORE, 1);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitInsn(Opcodes.ATHROW);
            mv.visitInsn(Opcodes.RETURN);
        }
        
        super.visitEnd();
    }

    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);

        lastVisitedLabel = label;
        lastVisitedOffset = label.getOffset();

        System.out.println(label.getOffset());
    }

    @Override
    public void visitInsn(int opcode) {
        super.visitInsn(opcode);

        switch (opcode) {
            case Opcodes.RETURN:
            System.out.println(this.lastVisitedOffset + " " + this.lastVisitedLabel);
            break;
        }
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
        if(desc.endsWith("BeforeClass;")){
            isBeforeClassPresent = true;
        }
        if(desc.endsWith("AfterClass;")){
            isAfterClassPresent = true;
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
}
