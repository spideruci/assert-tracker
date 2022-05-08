package org.spideruci.asserttracker;

import java.util.ArrayList;
import java.util.HashMap;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;
import org.spideruci.asserttracker.VisitedInsn.Type;

/**
 * VariableTrackingClassVisitor
 */
public class VariableTrackingClassVisitor extends ClassVisitor implements LiveVariableTrackingCallBack {

    public final HashMap<String, ArrayList<ArrayList<LocalVariable>>> liveVariablesAtAsserts = new HashMap<>();
    public VariableTrackingClassVisitor(int api) {
        super(api);
    }
    public VariableTrackingClassVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
            String[] exceptions) {
        System.out.println("visiting local variables for:" + name);
        MethodVisitor mv = this.cv.visitMethod(access, name, descriptor, signature, exceptions);
        return new LocalVariableVisitor(api, mv, access, name, descriptor, this); // super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
    }

    @Override
    public void trackLiveLocalVariablesAtAssert(
        String methodName,
        ArrayList<ArrayList<LocalVariable>> liveVariablesAtAsserts) {
        this.liveVariablesAtAsserts.put(methodName, liveVariablesAtAsserts);
    }
}

interface LiveVariableTrackingCallBack {

    void trackLiveLocalVariablesAtAssert(String methodName, ArrayList<ArrayList<LocalVariable>> liveVariablesAtAsserts);

}

class LocalVariableVisitor extends AdviceAdapter {

    private ArrayList<VisitedInsn> insns = new ArrayList<>();
    private ArrayList<LocalVariable> localVars = new ArrayList<>();
//    public ArrayList<ExceptionRange> tryCatchRanges = new ArrayList<>();
    private final LiveVariableTrackingCallBack liveVariableTracker;
    private final String methodName;

    public LocalVariableVisitor(int api,
        final MethodVisitor mv,
        final int access,
        final String name,
        final String descriptor, 
        LiveVariableTrackingCallBack liveVarTracker) {

        super(api, mv, access, name, descriptor);
        this.liveVariableTracker = liveVarTracker;
        this.methodName = name + descriptor + access;
    }

    @Override
    public void visitLabel(Label label) {
        insns.add(VisitedInsn.makeLabel(label));
        super.visitLabel(label);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {

        if (Utils.isAssertionStatement(name)) {
            insns.add(VisitedAssertInvoke.makeAssertInvoke(opcode, name, descriptor, owner));
        }

        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
        System.out.println("variable"+" "+name+start.getOffset()+" "+end.getOffset());
        super.visitLocalVariable(name, descriptor, signature, start, end, index);
        localVars.add(new LocalVariable(name, descriptor, index, start, end));
    }

    @Override
    public void visitEnd() {
//        System.out.println("end");
        super.visitEnd();
        ArrayList<ArrayList<LocalVariable>> liveVarAtAsserts = this.computeLiveVarsAtAsserts();
        liveVariableTracker.trackLiveLocalVariablesAtAssert(methodName, liveVarAtAsserts);
    }

    public ArrayList<ArrayList<LocalVariable>> computeLiveVarsAtAsserts() {
//        System.out.println("start computing");
        ArrayList<ArrayList<LocalVariable>> liveVarsAtAsserts = new ArrayList<>();
        int lastKnownOffset = 0;
        Boolean variableInTryCatch = false;
        for (VisitedInsn insn : this.insns) {
            if (insn.getType() == Type.AssertInvoke) {
                ArrayList<LocalVariable> liveSet = new ArrayList<>();
                for (LocalVariable variable : this.localVars) {
                    if (variableInTryCatch==false && variable.startOffset() <= lastKnownOffset && lastKnownOffset <= variable.endOffset()) {
                        liveSet.add(variable);
                    }
                    variableInTryCatch = false;
                }

                liveVarsAtAsserts.add(liveSet);
                continue;
            }

            VisitedLabel visitedLabel = (VisitedLabel) insn;
            int offset = visitedLabel.label.getOffset();
            lastKnownOffset = offset;
        }

        return liveVarsAtAsserts;
    }
}

class LocalVariable {
    public final String name;
    public final String desc;
    public final int varIndex;
    public final Label start;
    public final Label end;

    public LocalVariable(final String name,
                         final String desc,
                         final int varIndex,
                         final Label start,
                         final Label end) {
        this.name = name;
        this.desc = desc;
        this.varIndex = varIndex;
        this.start = start;
        this.end = end;
    }

    public int startOffset() {
        return start.getOffset();
    }

    public int endOffset() {
        return end.getOffset();
    }

    public int loadOpcode() {
        switch (desc) {
            case "I": // int
                return Opcodes.ILOAD;
            case "F": // float
                return Opcodes.FLOAD;
            case "J": // long
                return Opcodes.LLOAD;
            case "D": // double
                return Opcodes.DLOAD;
            default:
                if (desc.length() == 1) {
                    // likely loading a char/byte
                    // default to ILOAD
                    return Opcodes.ILOAD;
                }

                // Array or Object references need ALOAD.
                return Opcodes.ALOAD;
        }
    }

    static String[] names = { "ILOAD", "LLOAD", "FLOAD", "DLOAD", "ALOAD" };

    public String loadOpcodeName() {
        int loadOpcode = loadOpcode();
        return LocalVariable.names[loadOpcode - Opcodes.ILOAD];
    }
}

/**
 * VisitedInsn
 */
abstract class VisitedInsn {
    static enum Type {
        Label, AssertInvoke
    }

    abstract Type getType();

    static VisitedInsn makeLabel(final Label label) {
        return new VisitedLabel(label);
    }

    static VisitedInsn makeAssertInvoke(final int opcode, final String name, final String desc, final String owner) {
        return new VisitedAssertInvoke(opcode, name, desc, owner);
    }

}

class VisitedLabel extends VisitedInsn {

    public final Label label;

    public VisitedLabel(final Label label) {
        this.label = label;
    }

    @Override
    Type getType() {
        return Type.Label;
    }

}

class VisitedAssertInvoke extends VisitedInsn {

    public final String name;
    public final String desc;
    public final String owner;

    public VisitedAssertInvoke(final int opcode, final String name, final String desc, final String owner) {
        this.name = name;
        this.desc = desc;
        this.owner = owner;
    }

    @Override
    Type getType() {
        return Type.AssertInvoke;
    }

}
