package org.spideruci.asserttracker;

import java.util.ArrayList;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ExceptionVisitor2 extends MethodVisitor {

    private boolean isBeforeEachPresent = false;
    private boolean isAfterEachPresent = false;
    private boolean isTestAnnotationPresent = false;
    private boolean isDisabled = false;
    private boolean isBeforeClassPresent = false;
    private boolean isAfterClassPresent = false;
    private boolean afterPresent = false;
    private boolean beforePresent = false;

    private Label firstLabel = null;
    private Label endLabel = null;
    private Label handlerLabel = null;
    private int lastVisitedOffset = 0;
    private Label lastVisitedLabel = null;
    private String methodName;


    public ExceptionVisitor2(int api, MethodVisitor methodVisitor) {
        super(api, methodVisitor);
    }

    public ExceptionVisitor2(int api, MethodVisitor methodWriter, 
        int access, String name, String descriptor, boolean isTestClass,
        String testClassName, Boolean isJunitTestcase) {
        super(api, methodWriter);

        this.methodName = name + descriptor;
    }

    public void visitCode() {
        System.out.println("Running ExceptionVisitor2 on " + methodName);
    }

    @Override
    public void visitLdcInsn(Object val) {
        if (val instanceof String && ((String)val).equals(ExceptionVisitor.LDC_MARKER)) {
            endLabel = lastVisitedLabel;
        }

        // this will introduce some deadcode
        super.visitLdcInsn(val);
    }

    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);

        if (firstLabel == null) {
            // the method visit does a straight line traversal of the bytecode
            // in which case, the very first label that it visits, it should be
            // the first label of the method, Label offset 0.
            firstLabel = label;
        }

        if (endLabel != null) {
            // the label that comes after the end label is set, is the handler block's label
            handlerLabel =  label;
        }

        lastVisitedLabel = label;
        lastVisitedOffset = label.getOffset();

        System.out.println(label.getOffset());
    }

    @Override
    public void visitEnd() {

        mv.visitTryCatchBlock(firstLabel, endLabel, handlerLabel, "java/lang/Throwable");
        
        super.visitEnd();
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
}
