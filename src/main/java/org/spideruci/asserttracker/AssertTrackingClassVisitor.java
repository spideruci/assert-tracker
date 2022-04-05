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
        if (name.endsWith("Test")) {
            this.isTestClass = true;
            this.testClassName = name.replace("/",".");
        }else{
            this.testClassName = "NoneTestClass";
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }


    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        
        if (this.cv != null) {
            MethodVisitor methodWriter = this.cv.visitMethod(access, name, descriptor, signature, exceptions);
            return new AssertVisitor(this.api, name, methodWriter, this.isTestClass, this.testClassName);
        }
        else {
            return new AssertVisitor(this.api, name,this.isTestClass, this.testClassName);
        }
    }
    
}
