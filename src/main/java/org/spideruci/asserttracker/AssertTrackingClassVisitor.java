package org.spideruci.asserttracker;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

public class AssertTrackingClassVisitor extends ClassVisitor {

    public AssertTrackingClassVisitor(int api) {
        super(api);
    }

    public AssertTrackingClassVisitor(int api, ClassWriter cw) {
        super(api, cw);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {

        if (this.cv != null) {
            MethodVisitor methodWriter = this.cv.visitMethod(access, name, descriptor, signature, exceptions);
            return new AssertVisitor(this.api, name, methodWriter);
        }
        else {
            return new AssertVisitor(this.api, name);
        }
    }
    
}
