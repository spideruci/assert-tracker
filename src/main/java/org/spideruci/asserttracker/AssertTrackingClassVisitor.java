package org.spideruci.asserttracker;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

public class AssertTrackingClassVisitor extends ClassVisitor {

    public boolean isTestClass;
    public String testClassName;
    public AssertTrackingClassVisitor(int api) {
        super(api);
        this.isTestClass=false;
        this.testClassName = "unknown";
    }

    public AssertTrackingClassVisitor(int api, ClassWriter cw) {
        super(api, cw);
        this.isTestClass=false;
        this.testClassName = "unknown";
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        //sometimes they are not names for test classes, but it doesn't matter. Since we only print out assertion info
        //it indicates that it's a test class name
        this.testClassName = name.replace("/",".");
        super.visit(version, access, name, signature, superName, interfaces);
    }


    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor methodWriter = this.cv == null ? null : this.cv.visitMethod(access, name, descriptor, signature, exceptions);
        return new AssertVisitor(api, methodWriter, access, name, descriptor, isTestClass, testClassName);
    }
}

