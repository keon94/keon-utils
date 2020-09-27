package com.keon.projects.jassist;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

class CustomClassPool extends ClassPool {

    public CustomClassPool() {
        super(true);
    }

    @Override
    public CtClass get(final String classname) throws NotFoundException {
        try {
            return super.get(classname);
        } catch(final NotFoundException e) {
            final String classname2 = classname.replaceAll("(.*\\$.*)\\..*", "$1"); //working around a Java-assist bug for nested classes
            return super.get(classname2);
        }
    }

}
